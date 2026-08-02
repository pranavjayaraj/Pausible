#!/usr/bin/env python3
"""
Threshold-tuning harness for the on-device need-state classifier.

Reproduces the DEVICE decision logic in Python so you can tune it offline:
  1. embed prototypes -> per-need centroids (mean of L2-normalized phrase vecs)
  2. embed each eval sentence -> cosine to all 17 centroids -> ranked
  3. apply the decision policy and sweep its thresholds:
        Confident : s1 >= T_high AND (s1 - s2) >= margin        -> auto-route to top1
        Unclear   : s1 <  T_low                                 -> ask user to say more
        Ambiguous : otherwise                                   -> chips = {s : score >= s1 - delta}

Metrics (per threshold combo):
  commit_rate  fraction auto-routed (Confident)
  commit_acc   accuracy WHEN it auto-routes   <- the number that protects trust
  miss_rate    auto-routed but WRONG          <- the harmful case; penalized hardest
  chip_rate    fraction sent to chips
  chip_recall  of those, true label is in the chip set (user CAN pick it)
  chip_size    avg chips shown
  unclear_rate fraction asked to rephrase

IMPORTANT: the embedder here is a PROXY for the device model. If MiniLM is what
ships, the cosine geometry matches closely — but confirm the final thresholds
on-device before trusting them to the last decimal.

Setup (same venv as generate.py):
  ./.venv/bin/pip install sentence-transformers numpy
  ./.venv/bin/python eval.py                    # uses out/*.json[l]
"""

from __future__ import annotations

import argparse
import json
from itertools import product
from pathlib import Path

import numpy as np

OUT = Path(__file__).parent / "out"


# --- Embedding --------------------------------------------------------------
class Embedder:
    """Wraps a sentence-transformers model; returns L2-normalized vectors."""

    def __init__(self, model_name: str):
        from sentence_transformers import SentenceTransformer  # lazy: heavy import
        self.model = SentenceTransformer(model_name)

    def encode(self, texts: list[str]) -> np.ndarray:
        v = self.model.encode(texts, convert_to_numpy=True, normalize_embeddings=True)
        return v.astype(np.float32)


def build_centroids(emb: Embedder, prototypes: dict[str, list[str]]) -> tuple[list[str], np.ndarray]:
    """Return (labels, centroid_matrix[len(labels), dim]), each centroid L2-normalized."""
    labels = list(prototypes.keys())
    mats = []
    for state in labels:
        vecs = emb.encode(prototypes[state])          # already normalized
        c = vecs.mean(axis=0)
        c /= (np.linalg.norm(c) + 1e-9)               # re-normalize the mean
        mats.append(c)
    return labels, np.vstack(mats).astype(np.float32)


# --- Decision policy (vectorized over a precomputed score matrix) -----------
def sorted_scores(eval_vecs: np.ndarray, centroids: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """cosine = dot (both normalized). Return per-row scores & label-indices, sorted desc."""
    scores = eval_vecs @ centroids.T                  # (N, K)
    order = np.argsort(-scores, axis=1)               # (N, K) indices into labels
    ssorted = np.take_along_axis(scores, order, axis=1)
    return ssorted, order


def evaluate(ssorted: np.ndarray, order: np.ndarray, y_idx: np.ndarray,
             t_high: float, t_low: float, margin: float, delta: float) -> dict:
    n = ssorted.shape[0]
    s1, s2 = ssorted[:, 0], ssorted[:, 1]
    top1 = order[:, 0]

    confident = (s1 >= t_high) & (s1 - s2 >= margin)
    unclear = (~confident) & (s1 < t_low)
    ambiguous = ~confident & ~unclear

    # confident: correct if top1 == truth
    commit_correct = int(np.sum(confident & (top1 == y_idx)))
    n_commit = int(np.sum(confident))

    # ambiguous: chip set = labels with score >= s1 - delta; recall = truth in chips
    chip_hits, chip_sizes = 0, []
    amb_idx = np.where(ambiguous)[0]
    for i in amb_idx:
        thresh = s1[i] - delta
        chip = order[i][ssorted[i] >= thresh]
        chip_sizes.append(len(chip))
        if y_idx[i] in chip:
            chip_hits += 1
    n_amb = len(amb_idx)
    n_unclear = int(np.sum(unclear))

    return {
        "t_high": t_high, "t_low": t_low, "margin": margin, "delta": delta,
        "commit_rate": n_commit / n,
        "commit_acc": (commit_correct / n_commit) if n_commit else 0.0,
        "miss_rate": (n_commit - commit_correct) / n,
        "chip_rate": n_amb / n,
        "chip_recall": (chip_hits / n_amb) if n_amb else 0.0,
        "chip_size": float(np.mean(chip_sizes)) if chip_sizes else 0.0,
        "unclear_rate": n_unclear / n,
    }


def utility(m: dict) -> float:
    """Reward correct auto-routes; partial credit for chips that contain the answer;
    penalize wrong auto-routes hardest (they break trust) and over-asking."""
    return (
        m["commit_rate"] * m["commit_acc"]
        + m["chip_rate"] * m["chip_recall"] * 0.6
        - m["miss_rate"] * 1.5
        - m["unclear_rate"] * 0.3
        - max(0.0, m["chip_size"] - 3) * 0.05    # discourage menus
    )


def confusion(ssorted, order, y_idx, labels, best) -> list[tuple[str, str, int]]:
    """Top confident-but-wrong (true -> predicted) pairs under the best config."""
    s1, s2 = ssorted[:, 0], ssorted[:, 1]
    confident = (s1 >= best["t_high"]) & (s1 - s2 >= best["margin"])
    top1 = order[:, 0]
    from collections import Counter
    c = Counter()
    for i in np.where(confident & (top1 != y_idx))[0]:
        c[(labels[y_idx[i]], labels[top1[i]])] += 1
    return [(t, p, n) for (t, p), n in c.most_common(10)]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--prototypes", default=str(OUT / "prototypes.json"))
    ap.add_argument("--eval", default=str(OUT / "eval.jsonl"))
    ap.add_argument("--ambiguous", default=str(OUT / "ambiguous.jsonl"))
    ap.add_argument("--model", default="all-MiniLM-L6-v2")
    ap.add_argument("--top", type=int, default=8, help="how many top configs to print")
    args = ap.parse_args()

    prototypes = json.loads(Path(args.prototypes).read_text())
    eval_rows = [json.loads(l) for l in Path(args.eval).read_text().splitlines() if l.strip()]

    print(f"loading embedder: {args.model} ...")
    emb = Embedder(args.model)
    labels, centroids = build_centroids(emb, prototypes)
    lab_ix = {s: i for i, s in enumerate(labels)}

    eval_vecs = emb.encode([r["text"] for r in eval_rows])
    y_idx = np.array([lab_ix[r["label"]] for r in eval_rows])
    ssorted, order = sorted_scores(eval_vecs, centroids)

    # --- sweep -------------------------------------------------------------
    grid = dict(
        t_high=[0.35, 0.40, 0.45, 0.50, 0.55],
        margin=[0.03, 0.05, 0.08, 0.12],
        t_low=[0.20, 0.25, 0.30],
        delta=[0.05, 0.08, 0.12],
    )
    results = []
    for th, mg, tl, dl in product(grid["t_high"], grid["margin"], grid["t_low"], grid["delta"]):
        if tl > th:
            continue
        m = evaluate(ssorted, order, y_idx, th, tl, mg, dl)
        m["utility"] = utility(m)
        results.append(m)
    results.sort(key=lambda m: -m["utility"])

    print(f"\neval rows: {len(eval_rows)}   states: {len(labels)}   configs tried: {len(results)}\n")
    hdr = f"{'util':>6} {'Thi':>5}{'mrg':>6}{'Tlo':>6}{'dlt':>6}  {'commit%':>8}{'acc':>6}{'miss%':>7}  {'chip%':>6}{'recall':>7}{'sz':>5}  {'ask%':>6}"
    print(hdr); print("-" * len(hdr))
    for m in results[:args.top]:
        print(f"{m['utility']:6.3f} {m['t_high']:5.2f}{m['margin']:6.2f}{m['t_low']:6.2f}{m['delta']:6.2f}  "
              f"{m['commit_rate']*100:7.1f}%{m['commit_acc']*100:6.0f}{m['miss_rate']*100:6.1f}%  "
              f"{m['chip_rate']*100:5.1f}%{m['chip_recall']*100:6.0f}%{m['chip_size']:5.1f}  "
              f"{m['unclear_rate']*100:5.1f}%")

    best = results[0]
    print(f"\nRECOMMENDED: T_high={best['t_high']} T_low={best['t_low']} "
          f"margin={best['margin']} delta={best['delta']}")
    print(f"  auto-routes {best['commit_rate']*100:.0f}% of check-ins at "
          f"{best['commit_acc']*100:.0f}% accuracy; {best['chip_rate']*100:.0f}% -> chips "
          f"(answer present {best['chip_recall']*100:.0f}% of the time); "
          f"{best['unclear_rate']*100:.0f}% -> ask.")

    # --- where it confidently guesses WRONG: fix these prototypes ----------
    conf = confusion(ssorted, order, y_idx, labels, best)
    if conf:
        print("\ntop confident-but-wrong pairs (tighten these prototypes):")
        for t, p, c in conf:
            print(f"  {t:22s} -> {p:22s}  x{c}")

    # --- sanity-check the ambiguous set with the chosen config -------------
    amb_path = Path(args.ambiguous)
    if amb_path.exists():
        amb = [json.loads(l) for l in amb_path.read_text().splitlines() if l.strip()]
        if amb:
            av = emb.encode([r["text"] for r in amb])
            ss, od = sorted_scores(av, centroids)
            s1 = ss[:, 0]
            good = wrong_commit = 0
            for i, r in enumerate(amb):
                cand = {lab_ix[c] for c in r["candidates"] if c in lab_ix}
                confident = (s1[i] >= best["t_high"]) and (s1[i] - ss[i, 1] >= best["margin"])
                chip = set(od[i][ss[i] >= s1[i] - best["delta"]])
                if confident:
                    wrong_commit += 0 if od[i, 0] in cand else 1
                elif chip & cand:
                    good += 1
            print(f"\nambiguous set ({len(amb)}): {good} surfaced a valid chip, "
                  f"{wrong_commit} wrongly auto-committed (want this near 0).")


if __name__ == "__main__":
    main()
