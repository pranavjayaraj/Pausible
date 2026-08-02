#!/usr/bin/env python3
"""
Check-In need-state bootstrapper.

Uses Claude Haiku (cloud, dev-time only) to generate high-quality data for the
ON-DEVICE need-state classifier. Nothing here ships to the device or runs at
runtime — you run this once, review the output, and paste the phrases into
NeedPrototypes.kt. The device stays 100% local (MiniLM/USE embeddings).

Three phases (run any subset with --phase):
  prototypes  -> diverse anchor phrases per state  -> out/prototypes.json
                 (these become NeedPrototypes.PHRASES -> class centroids)
  eval        -> HELD-OUT labeled (text,label) pairs -> out/eval.jsonl
                 (for tuning T_high / T_low / margin and, later, fine-tuning)
  ambiguous   -> cross-state sentences ("sore" -> [BODY,HAND,EYE]) with a
                 candidate list -> out/ambiguous.jsonl
                 (for tuning the Ambiguous->chips decision)

Then:
  python generate.py --emit-kotlin   # out/prototypes.json -> out/NeedPrototypes.kt

Setup:
  pip install anthropic
  export ANTHROPIC_API_KEY=...
  python generate.py --phase prototypes eval ambiguous
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

import anthropic

MODEL = "claude-haiku-4-5-20251001"
OUT = Path(__file__).parent / "out"

# --- The taxonomy: the single source of truth for the whole app -------------
# id -> (bucket, one-line definition the way a USER would experience it)
TAXONOMY: dict[str, tuple[str, str]] = {
    # Physical
    "BODY_TENSION":         ("PHYSICAL", "Stiff, achy body — neck/back/shoulder tension, bad posture from sitting."),
    "HAND_STRAIN":          ("PHYSICAL", "Wrist/hand/finger/thumb pain from typing, mousing, or scrolling."),
    "EYE_STRAIN":           ("PHYSICAL", "Tired, dry, blurry eyes; screen/glare headaches behind the eyes."),
    # Cognitive
    "SCATTERED":            ("COGNITIVE", "Brain fog, no attention span, constant context-switching, can't focus."),
    "OVERWHELMED":          ("COGNITIVE", "Drowning in tasks, decision fatigue, paralysis, bursting inbox."),
    "CONFUSED_LOST":        ("COGNITIVE", "Stuck, blocked, unsure of the next step, needs guidance."),
    "TASK_PARALYSIS":       ("COGNITIVE", "Knows the task, can't start it — avoidance, dread, procrastination."),
    "SENSORY_OVERLOAD":     ("COGNITIVE", "Too much input: noise, lights, chatter, pings — not too many tasks."),
    # Energy
    "DRAINED":              ("ENERGY", "Burnout, exhaustion, running on fumes, afternoon slump."),
    "RESTLESS":             ("ENERGY", "Twitchy, fidgety, pent-up kinetic energy, cabin fever, can't sit still."),
    "CANT_WIND_DOWN":       ("ENERGY", "Wired at night, racing thoughts, revenge bedtime procrastination."),
    # Emotional
    "WOUND_UP":             ("EMOTIONAL", "Angry, frustrated, defensive, on edge, high cortisol."),
    "LOW_MOOD":             ("EMOTIONAL", "Apathy, sadness, emptiness, the 'blahs', quiet-quitting."),
    "STUCK_ON_A_THOUGHT":   ("EMOTIONAL", "Rumination, looping anxiety, imposter syndrome, overthinking."),
    "DISCONNECTED":         ("EMOTIONAL", "Loneliness, feeling invisible, remote-work isolation."),
    # Biological / Positive
    "BIOLOGICAL_DEPLETION": ("BIO_POS", "Hungry, dehydrated, caffeine withdrawal, needing the restroom."),
    "ACCOMPLISHED_FLOW":    ("BIO_POS", "Productive, crushed a goal, in deep flow, competent, relieved."),
}

BUCKETS: dict[str, list[str]] = {}
for _id, (_b, _d) in TAXONOMY.items():
    BUCKETS.setdefault(_b, []).append(_id)

LABELS = list(TAXONOMY.keys())


def taxonomy_block() -> str:
    lines = []
    for bucket, ids in BUCKETS.items():
        lines.append(f"[{bucket}]")
        for i in ids:
            lines.append(f"  - {i}: {TAXONOMY[i][1]}")
    return "\n".join(lines)


SYSTEM = f"""You generate realistic first-person microcopy — the kind of thing a stressed \
knowledge-worker actually types into a wellness check-in box on their phone. You are NOT \
writing marketing copy or clinical descriptions.

The app routes each utterance into exactly ONE of these {len(TAXONOMY)} "need states":

{taxonomy_block()}

Hard rules for every sentence you produce:
- First person, present tense, how a real person vents ("my neck is killing me", "ugh sore").
- VARY everything: length (from 1-3 word fragments like "sore" or "so tired" up to a full \
run-on sentence), register (blunt, slangy, metaphorical, mild typos are fine), and intensity \
(from a mild "eyes a bit tired" to "my retinas are on fire").
- No emojis unless it's genuinely how someone types. No hashtags. No clinical/therapy jargon.
- Never mention the state's NAME or the app. Describe the feeling, don't label it.
Return output ONLY by calling the provided tool."""


# --- Tool schemas (force structured output) ---------------------------------
def tool_prototypes(n: int) -> dict:
    return {
        "name": "emit_prototypes",
        "description": f"Return exactly {n} anchor phrases for the target need state.",
        "input_schema": {
            "type": "object",
            "properties": {
                "phrases": {"type": "array", "items": {"type": "string"}, "minItems": n, "maxItems": n}
            },
            "required": ["phrases"],
        },
    }


def tool_eval(n: int) -> dict:
    return {
        "name": "emit_eval",
        "description": f"Return exactly {n} held-out labeled examples for the target state.",
        "input_schema": {
            "type": "object",
            "properties": {
                "examples": {
                    "type": "array", "minItems": n, "maxItems": n,
                    "items": {
                        "type": "object",
                        "properties": {"text": {"type": "string"}},
                        "required": ["text"],
                    },
                }
            },
            "required": ["examples"],
        },
    }


def tool_ambiguous() -> dict:
    return {
        "name": "emit_ambiguous",
        "description": "Return sentences that genuinely fit 2+ of the given sibling states.",
        "input_schema": {
            "type": "object",
            "properties": {
                "examples": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "text": {"type": "string"},
                            "candidates": {
                                "type": "array",
                                "items": {"type": "string", "enum": LABELS},
                                "minItems": 2,
                            },
                            "primary": {"type": "string", "enum": LABELS,
                                        "description": "single best guess if forced to pick"},
                        },
                        "required": ["text", "candidates", "primary"],
                    },
                }
            },
            "required": ["examples"],
        },
    }


def call(client: anthropic.Anthropic, user: str, tool: dict, temperature: float) -> dict:
    """One forced-tool call; returns the tool input dict."""
    msg = client.messages.create(
        model=MODEL,
        max_tokens=2048,
        temperature=temperature,
        system=SYSTEM,
        tools=[tool],
        tool_choice={"type": "tool", "name": tool["name"]},
        messages=[{"role": "user", "content": user}],
    )
    for block in msg.content:
        if block.type == "tool_use":
            return block.input
    raise RuntimeError("model did not call the tool")


def _norm(s: str) -> str:
    return re.sub(r"[^a-z0-9 ]", "", s.lower()).strip()


# --- Phases -----------------------------------------------------------------
def gen_prototypes(client, per_state: int) -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for state, (bucket, desc) in TAXONOMY.items():
        siblings = [s for s in BUCKETS[bucket] if s != state]
        sib_txt = "\n".join(f"  - {s}: {TAXONOMY[s][1]}" for s in siblings) or "  (none)"
        user = f"""TARGET STATE: {state}
Definition: {desc}

Its same-bucket siblings (do NOT drift into these — each phrase must be unambiguously {state}):
{sib_txt}

Generate {per_state} DIVERSE anchor phrases for {state}. Include at least three very short \
fragments (1-3 words). Each phrase must clearly belong to {state} and NOT equally fit a \
sibling above. Spread across mild -> severe intensity."""
        got = call(client, user, tool_prototypes(per_state), temperature=1.0)["phrases"]
        # de-dupe (case/punct-insensitive) within the state
        seen, kept = set(), []
        for p in got:
            k = _norm(p)
            if k and k not in seen:
                seen.add(k); kept.append(p.strip())
        result[state] = kept
        print(f"  {state:22s} {len(kept)} phrases", file=sys.stderr)
    return result


def gen_eval(client, per_state: int) -> list[dict]:
    rows: list[dict] = []
    for state, (_b, desc) in TAXONOMY.items():
        user = f"""TARGET STATE: {state}
Definition: {desc}

Generate {per_state} HELD-OUT test utterances for {state}. These are for EVALUATION, so make \
them DIFFERENT in wording from typical examples — harder, more oblique, more real. Still \
unambiguously {state}."""
        got = call(client, user, tool_eval(per_state), temperature=0.9)["examples"]
        for e in got:
            rows.append({"text": e["text"].strip(), "label": state})
        print(f"  eval {state:22s} +{len(got)}", file=sys.stderr)
    return rows


def gen_ambiguous(client, per_bucket: int) -> list[dict]:
    rows: list[dict] = []
    for bucket, ids in BUCKETS.items():
        if len(ids) < 2:
            continue
        sib_txt = "\n".join(f"  - {s}: {TAXONOMY[s][1]}" for s in ids)
        user = f"""These sibling states are easy to confuse from short text:
{sib_txt}

Generate up to {per_bucket} realistic utterances that GENUINELY fit 2 or more of them — the \
classic case is a bare word like "sore" that could be BODY_TENSION, HAND_STRAIN, or EYE_STRAIN. \
For each, list the candidate states and a single 'primary' best-guess. If some of these states \
are not actually confusable with each other, produce fewer rather than forcing it."""
        got = call(client, user, tool_ambiguous(), temperature=0.9)["examples"]
        for e in got:
            rows.append({"text": e["text"].strip(), "candidates": e["candidates"], "primary": e["primary"]})
        print(f"  ambiguous [{bucket}] +{len(got)}", file=sys.stderr)
    return rows


# --- Kotlin emitter ---------------------------------------------------------
def emit_kotlin(prototypes: dict[str, list[str]]) -> str:
    order = LABELS  # stable, taxonomy order
    body = []
    for state in order:
        phrases = prototypes.get(state, [])
        inner = ",\n".join(f'            "{p.replace(chr(92), chr(92)*2).replace(chr(34), chr(92)+chr(34))}"' for p in phrases)
        body.append(f"        NeedState.{state} to listOf(\n{inner}\n        )")
    joined = ",\n\n".join(body)
    return f"""package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState

/**
 * GENERATED by tools/checkin-bootstrap/generate.py — review before committing.
 * Tier-2 prototype phrases: embedded once and averaged into per-need centroids.
 */
internal object NeedPrototypes {{
    val PHRASES: Map<NeedState, List<String>> = mapOf(
{joined}
    )
}}
"""


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--phase", nargs="*", default=[], choices=["prototypes", "eval", "ambiguous"])
    ap.add_argument("--per-state", type=int, default=24, help="prototypes per state")
    ap.add_argument("--eval-per-state", type=int, default=15)
    ap.add_argument("--ambiguous-per-bucket", type=int, default=8)
    ap.add_argument("--emit-kotlin", action="store_true", help="out/prototypes.json -> out/NeedPrototypes.kt")
    args = ap.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)

    if args.emit_kotlin:
        protos = json.loads((OUT / "prototypes.json").read_text())
        (OUT / "NeedPrototypes.kt").write_text(emit_kotlin(protos))
        print(f"wrote {OUT/'NeedPrototypes.kt'}")
        return

    if not args.phase:
        ap.error("pass --phase and/or --emit-kotlin")

    client = anthropic.Anthropic()  # ANTHROPIC_API_KEY from env

    if "prototypes" in args.phase:
        print("generating prototypes...", file=sys.stderr)
        protos = gen_prototypes(client, args.per_state)
        (OUT / "prototypes.json").write_text(json.dumps(protos, indent=2, ensure_ascii=False))
        print(f"wrote {OUT/'prototypes.json'}")

    if "eval" in args.phase:
        print("generating eval set...", file=sys.stderr)
        rows = gen_eval(client, args.eval_per_state)
        with (OUT / "eval.jsonl").open("w") as f:
            for r in rows:
                f.write(json.dumps(r, ensure_ascii=False) + "\n")
        print(f"wrote {OUT/'eval.jsonl'} ({len(rows)} rows)")

    if "ambiguous" in args.phase:
        print("generating ambiguous set...", file=sys.stderr)
        rows = gen_ambiguous(client, args.ambiguous_per_bucket)
        with (OUT / "ambiguous.jsonl").open("w") as f:
            for r in rows:
                f.write(json.dumps(r, ensure_ascii=False) + "\n")
        print(f"wrote {OUT/'ambiguous.jsonl'} ({len(rows)} rows)")


if __name__ == "__main__":
    main()
