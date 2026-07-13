"""Single source of truth for the Riverbloom acceptance-model feature schema.

The Kotlin side (`:sense-ml` FeatureSchema.kt) mirrors this file exactly. Any
change here MUST bump SCHEMA_VERSION and be replicated in Kotlin — the spec
hash below is embedded in every exported model artifact, and the Kotlin
runtime refuses to run a model whose hash doesn't match its own pipeline.
"""

import hashlib

SCHEMA_VERSION = 3
FEATURE_COUNT = 34
WINDOW_MINUTES = 5
MODEL_TASK = "accept_given_prompt"

# (index, name, normalization, arg)
# normalization tokens: div (value/arg, clipped 0..1), onehot, flag,
# cyc_sin / cyc_cos (sin/cos(2*pi*value/arg)), rate (already 0..1)
SPEC = [
    (0, "foreground_duration_sec", "div", 300),
    (1, "app_switch_count", "div", 20),
    (2, "unique_app_count", "div", 10),
    (3, "longest_session_sec", "div", 1800),
    (4, "screen_on_time_sec", "div", 300),
    (5, "continuous_screen_on_min", "div", 60),
    (6, "distracting_return_count", "div", 5),
    (7, "cold_open_count", "div", 10),
    (8, "unlock_count_last_hour", "div", 15),
    (9, "cat_work", "onehot", None),
    (10, "cat_social", "onehot", None),
    (11, "cat_video", "onehot", None),
    (12, "cat_game_dating", "onehot", None),
    (13, "cat_chat", "onehot", None),
    (14, "cat_other", "onehot", None),
    (15, "hour_sin", "cyc_sin", 24),
    (16, "hour_cos", "cyc_cos", 24),
    (17, "dow_sin", "cyc_sin", 7),
    (18, "dow_cos", "cyc_cos", 7),
    (19, "late_night_flag", "flag", None),
    (20, "activity_still", "onehot", None),
    (21, "activity_on_foot", "onehot", None),
    (22, "activity_unknown", "onehot", None),
    (23, "minutes_in_current_activity", "div", 120),
    (24, "charging_flag", "flag", None),
    (25, "battery_pct", "div", 100),
    (26, "min_since_first_unlock_today", "div", 960),
    (27, "min_since_last_completed_break", "div", 240),
    (28, "dismiss_count_24h", "div", 5),
    (29, "snooze_count_24h", "div", 5),
    (30, "accept_rate_7d", "rate", None),
    (31, "accept_rate_this_hour", "rate", None),
    (32, "avg_response_delay_sec", "div", 120),
    (33, "break_completion_rate", "rate", None),
]

assert len(SPEC) == FEATURE_COUNT
assert [i for i, *_ in SPEC] == list(range(FEATURE_COUNT))


def canonical_spec_string() -> str:
    """Must byte-match FeatureSchema.canonicalSpecString() in Kotlin."""
    lines = [f"schema_version:{SCHEMA_VERSION}", f"window_minutes:{WINDOW_MINUTES}"]
    for idx, name, norm, arg in SPEC:
        lines.append(f"{idx}:{name}:{norm}" + (f":{arg}" if arg is not None else ""))
    return "\n".join(lines)


def spec_hash() -> str:
    return hashlib.sha256(canonical_spec_string().encode("utf-8")).hexdigest()


if __name__ == "__main__":
    print(canonical_spec_string())
    print("\nspec_hash:", spec_hash())
