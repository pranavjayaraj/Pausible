# Riverbloom acceptance model — training pipeline

Trains the on-device microbreak-timing model (`P(user accepts a prompt now)`)
and exports it for the pure-Kotlin inference runtime in `:sense-ml`.

> Full system documentation (architecture, decision flow, label rules,
> modification checklists): [`docs/SENSE_ML.md`](../../docs/SENSE_ML.md).

## Layout

| File | Role |
|---|---|
| `feature_spec.py` | **Single source of truth** for the 34-feature schema. Mirrored exactly by `FeatureSchema.kt`. Hash of this spec is stamped into every artifact. |
| `bootstrap_data.py` | Synthetic cold-start dataset (scenario simulator + latent receptivity function). Replaced by real decision-log data after launch. |
| `train.py` | numpy-only MLP trainer (34→32→16→1, Adam, weighted BCE, early stop on val PR-AUC) + artifact export. |

## Run

```bash
python3 train.py   # needs numpy only
```

Outputs to `out/`:

- `model.json` (~18 KB) — weights + schema metadata handshake. Deployed as
  `library/sense-ml/src/main/assets/riverbloom_acceptance_v3.json`.
- `golden_vectors.json` — probe inputs/outputs for the Kotlin parity test
  (`ModelParityTest`). Copied to `library/sense-ml/src/test/resources/`.

After retraining, refresh both copies:

```bash
cp out/model.json ../sense-ml/src/main/assets/riverbloom_acceptance_v3.json
cp out/model.json out/golden_vectors.json ../sense-ml/src/test/resources/
```

## Ship gate

`train.py` refuses to export a model whose validation PR-AUC does not beat the
Stage-0 rules engine (`rules_baseline_score`, kept in sync with
`RulesEngine.kt`). Current run: model **0.711** vs rules **0.575**.

## Changing the feature schema

1. Edit `SPEC` in `feature_spec.py` **and** `FeatureSchema.kt` identically.
2. Bump `SCHEMA_VERSION` in both. No exceptions — the runtime refuses to load
   a model whose `feature_spec_hash` doesn't match its compiled pipeline.
3. Retrain, re-export, re-run `:sense-ml` tests (parity + spec-hash tests will
   catch any divergence).

## Labels & production data (post-launch)

Bootstrap labels are simulated. Real training rows come from the SDK decision
log: only windows where a prompt was **shown** are labeled
(accepted=1 / dismissed·ignored=0); unprompted windows stay unlabeled.
ε-exploration (~5%) at borderline scores is required to de-bias the data the
rules engine collects.
