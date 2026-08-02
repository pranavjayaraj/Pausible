# Check-In need-state bootstrapper

Dev-time only. Uses **Claude Haiku** (cloud) to generate data for the **on-device**
need-state classifier. Nothing here ships or runs at runtime — the device stays 100% local.

## Why this exists

Haiku is an excellent zero-shot classifier for our 17-state taxonomy, but it's a cloud API —
we can't (and for privacy shouldn't) send every emotional check-in to a server. So we use Haiku
**offline** to bake its judgment into the local model:

- **prototypes** → diverse anchor phrases per state → paste into `NeedPrototypes.kt`
  (embedded once → per-need centroids for the Tier-2 embedding classifier).
- **eval** → a *held-out* labeled set → tune the `Confident / Ambiguous / Unclear` thresholds
  (`T_high`, `T_low`, `margin`) and, later, fine-tune a MobileBERT head.
- **ambiguous** → sentences like "sore" that fit 2+ states → tune the chips disambiguation.

## Run

```bash
pip install anthropic
export ANTHROPIC_API_KEY=...

python generate.py --phase prototypes eval ambiguous
python generate.py --emit-kotlin      # out/prototypes.json -> out/NeedPrototypes.kt
```

Outputs land in `out/`. **Review before committing** — treat Haiku's output as a first draft,
not ground truth. Keep `eval.jsonl` strictly held out (never embed it as a prototype), or your
threshold numbers will be optimistic.

## Tune thresholds (`eval.py`)

Once `out/` is populated, turn the held-out data into tuned decision thresholds. This reproduces
the **device** decision logic in Python, embedding with the same model the device will use
(`all-MiniLM-L6-v2` by default), then sweeps `T_high / T_low / margin / delta`:

```bash
./.venv/bin/pip install sentence-transformers numpy
./.venv/bin/python eval.py            # reads out/prototypes.json, out/eval.jsonl, out/ambiguous.jsonl
```

It prints the best threshold combos ranked by a trust-weighted utility (correct auto-routes are
rewarded, **wrong** auto-routes penalized hardest), the recommended config, the top
confident-but-wrong pairs (i.e. which prototypes to tighten), and a sanity check on the
ambiguous set. The embedder is a *proxy* — confirm final thresholds on-device.

> `sentence-transformers` pulls in torch (~1 GB). If that's too heavy, swap `Embedder` for a
> `fastembed` (ONNX) backend — the rest of the harness is unchanged.

## Cost

~30 short calls per full generate run; a few cents total. Re-run whenever you add/rename a state
in the taxonomy (edit `TAXONOMY` in `generate.py`).
