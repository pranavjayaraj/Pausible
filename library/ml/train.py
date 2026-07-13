"""Train the Riverbloom acceptance model and export it for on-device inference.

Dependency-free by design (numpy only): the model is a 34->32->16->1 MLP,
small enough that a hand-rolled trainer is simpler and more reproducible than
a TF/Keras install. Export format is the pure-Kotlin inference contract:
model.json with schema metadata handshake + raw layer weights.

Usage:
    python3 train.py            # trains, evaluates, exports artifacts

Outputs (in ml/out/):
    model.json           — weights + schema metadata (ships in the AAR assets)
    golden_vectors.json  — input/output pairs for the Kotlin parity test
"""

import json
import os
import time

import numpy as np

from bootstrap_data import generate
from feature_spec import (
    FEATURE_COUNT,
    MODEL_TASK,
    SCHEMA_VERSION,
    WINDOW_MINUTES,
    spec_hash,
)

RNG = np.random.default_rng(42)
HIDDEN = [32, 16]
EPOCHS = 60
BATCH = 256
LR = 1e-3
PATIENCE = 8
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")


# ---------------------------------------------------------------- model

def init_params():
    sizes = [FEATURE_COUNT] + HIDDEN + [1]
    params = []
    for fan_in, fan_out in zip(sizes[:-1], sizes[1:]):
        w = RNG.normal(0, np.sqrt(2.0 / fan_in), (fan_in, fan_out))
        b = np.zeros(fan_out)
        params.append([w, b])
    return params


def forward(params, x):
    """Returns (activations per layer, output probability)."""
    acts = [x]
    h = x
    for i, (w, b) in enumerate(params):
        z = h @ w + b
        h = np.maximum(0, z) if i < len(params) - 1 else 1 / (1 + np.exp(-z))
        acts.append(h)
    return acts, h[:, 0]


def backward(params, acts, y, sample_w):
    """Weighted BCE gradient. acts[-1] is sigmoid output."""
    n = len(y)
    grads = []
    # dL/dz for sigmoid+BCE = (p - y), weighted
    delta = ((acts[-1][:, 0] - y) * sample_w / n)[:, None]
    for i in range(len(params) - 1, -1, -1):
        w, _ = params[i]
        gw = acts[i].T @ delta
        gb = delta.sum(axis=0)
        grads.insert(0, [gw, gb])
        if i > 0:
            delta = (delta @ w.T) * (acts[i] > 0)  # ReLU mask
    return grads


class Adam:
    def __init__(self, params, lr):
        self.lr, self.b1, self.b2, self.eps, self.t = lr, 0.9, 0.999, 1e-8, 0
        self.m = [[np.zeros_like(w), np.zeros_like(b)] for w, b in params]
        self.v = [[np.zeros_like(w), np.zeros_like(b)] for w, b in params]

    def step(self, params, grads):
        self.t += 1
        for i in range(len(params)):
            for j in range(2):
                g = grads[i][j]
                self.m[i][j] = self.b1 * self.m[i][j] + (1 - self.b1) * g
                self.v[i][j] = self.b2 * self.v[i][j] + (1 - self.b2) * g * g
                mhat = self.m[i][j] / (1 - self.b1 ** self.t)
                vhat = self.v[i][j] / (1 - self.b2 ** self.t)
                params[i][j] -= self.lr * mhat / (np.sqrt(vhat) + self.eps)


# ---------------------------------------------------------------- metrics

def average_precision(y, scores):
    order = np.argsort(-scores)
    y_sorted = y[order]
    tp = np.cumsum(y_sorted)
    precision = tp / (np.arange(len(y_sorted)) + 1)
    return float((precision * y_sorted).sum() / max(1.0, y_sorted.sum()))


def rules_baseline_score(X):
    """Stage-0 rules engine score, replicated over normalized features.
    MUST stay in sync with RulesEngine.kt — this is the bar the model has to
    beat before it ships."""
    still = X[:, 20]
    score = (
        0.40 * X[:, 5]                 # continuous screen-on
        + 0.20 * X[:, 1]               # app switching
        + 0.15 * X[:, 23] * still      # long stillness (only while still)
        + 0.10 * X[:, 6]               # distracting-app returns
        + 0.10 * X[:, 7]               # cold opens
        + 0.05 * X[:, 24]              # charging
        - 0.30 * np.maximum(0.0, 1.0 - X[:, 27] * 4)  # cooldown (<60 min since break)
        - 0.20 * X[:, 28]              # recent dismissals
        - 0.15 * X[:, 19]              # late night
    )
    return np.clip(score, 0.0, 1.0)


# ---------------------------------------------------------------- training

def train():
    X, y = generate(60_000)
    n_val = int(len(X) * 0.2)
    idx = RNG.permutation(len(X))
    val_idx, tr_idx = idx[:n_val], idx[n_val:]
    Xtr, ytr, Xval, yval = X[tr_idx], y[tr_idx], X[val_idx], y[val_idx]

    pos_w = (len(ytr) - ytr.sum()) / max(1.0, ytr.sum())
    print(f"train={len(ytr)} val={len(yval)} base_rate={ytr.mean():.3f} pos_weight={pos_w:.2f}")

    params = init_params()
    opt = Adam(params, LR)
    best_ap, best_params, since_best = -1.0, None, 0

    for epoch in range(EPOCHS):
        order = RNG.permutation(len(Xtr))
        for s in range(0, len(Xtr), BATCH):
            b = order[s : s + BATCH]
            acts, _ = forward(params, Xtr[b])
            sample_w = np.where(ytr[b] == 1, pos_w, 1.0)
            opt.step(params, backward(params, acts, ytr[b], sample_w))

        _, pval = forward(params, Xval)
        ap = average_precision(yval, pval)
        if ap > best_ap:
            best_ap, since_best = ap, 0
            best_params = [[w.copy(), b.copy()] for w, b in params]
        else:
            since_best += 1
        if epoch % 5 == 0 or since_best == 0:
            print(f"epoch {epoch:3d}  val_pr_auc={ap:.4f}{'  *' if since_best == 0 else ''}")
        if since_best >= PATIENCE:
            print(f"early stop at epoch {epoch}")
            break

    # ------- ship gate: model must beat the rules engine on PR-AUC
    rules_ap = average_precision(yval, rules_baseline_score(Xval))
    _, pval = forward(best_params, Xval)
    model_ap = average_precision(yval, pval)
    print(f"\nval base rate       : {yval.mean():.4f}")
    print(f"rules-engine PR-AUC : {rules_ap:.4f}")
    print(f"model PR-AUC        : {model_ap:.4f}")
    if model_ap <= rules_ap:
        raise SystemExit("SHIP GATE FAILED: model does not beat the rules baseline")

    return best_params, {
        "val_pr_auc": round(model_ap, 4),
        "rules_pr_auc": round(rules_ap, 4),
        "val_base_rate": round(float(yval.mean()), 4),
    }


# ---------------------------------------------------------------- export

def export(params, metrics):
    os.makedirs(OUT_DIR, exist_ok=True)
    layers = []
    for i, (w, b) in enumerate(params):
        layers.append({
            "activation": "relu" if i < len(params) - 1 else "sigmoid",
            "weights": [[round(float(v), 6) for v in row] for row in w],  # [in][out]
            "bias": [round(float(v), 6) for v in b],
        })
    artifact = {
        "schema_version": SCHEMA_VERSION,
        "feature_count": FEATURE_COUNT,
        "window_size_min": WINDOW_MINUTES,
        "model_task": MODEL_TASK,
        "feature_spec_hash": spec_hash(),
        "trained_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "metrics": metrics,
        "layers": layers,
    }
    model_path = os.path.join(OUT_DIR, "model.json")
    with open(model_path, "w") as f:
        json.dump(artifact, f)
    print(f"\nwrote {model_path} ({os.path.getsize(model_path) / 1024:.1f} KB)")

    # Golden vectors: fixed probe inputs + this model's outputs. The Kotlin
    # parity test replays these through AcceptanceModel and asserts equality.
    rng = np.random.default_rng(123)
    probes = [np.zeros(FEATURE_COUNT), np.ones(FEATURE_COUNT)]
    Xp, _ = generate(200, seed=99)
    probes += [Xp[i] for i in rng.choice(len(Xp), 14, replace=False)]
    cases = []
    for x in probes:
        _, p = forward(params, x[None, :])
        cases.append({
            "features": [round(float(v), 6) for v in x],
            "expected": round(float(p[0]), 6),
        })
    golden_path = os.path.join(OUT_DIR, "golden_vectors.json")
    with open(golden_path, "w") as f:
        json.dump({"schema_version": SCHEMA_VERSION, "tolerance": 1e-4, "cases": cases}, f)
    print(f"wrote {golden_path} ({len(cases)} cases)")


if __name__ == "__main__":
    params, metrics = train()
    export(params, metrics)
