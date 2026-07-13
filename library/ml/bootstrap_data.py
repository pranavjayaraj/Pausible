"""Bootstrap (synthetic) dataset generator for the acceptance model.

We have no real outcome logs before launch, so we simulate context windows
from behavioral scenarios and sample accept/ignore labels from a hand-designed
latent receptivity function + noise. This is explicitly a COLD-START artifact:
its only job is to give the on-device model sane initial weights that roughly
agree with the Stage-0 rules engine while already using the personalization
features. It is replaced by real decision-log training data after launch.

Labels follow the product convention: 1 = prompt shown and accepted,
0 = prompt shown and dismissed/ignored. (Windows where no prompt is shown are
unlabeled in production and never trained on; the simulator only emits
"prompt shown" windows for the same reason.)
"""

import numpy as np

from feature_spec import FEATURE_COUNT

CATEGORIES = ["work", "social", "video", "game_dating", "chat", "other"]
ACTIVITIES = ["still", "on_foot", "unknown"]  # in_vehicle is hard-gated upstream

SCENARIOS = {
    # name: (weight, sampler kwargs)
    "deep_focus": 0.25,
    "fragmented": 0.20,
    "casual": 0.25,
    "late_night": 0.10,
    "on_the_move": 0.12,
    "idle_pickup": 0.08,
}


def _clip01(x):
    return np.clip(x, 0.0, 1.0)


def sample_raw(rng: np.random.Generator, scenario: str) -> dict:
    """Sample plausible raw signal values for one 5-min context window."""
    r = {}
    if scenario == "deep_focus":
        r["continuous_screen_on_min"] = rng.uniform(25, 90)
        r["foreground_duration_sec"] = rng.uniform(240, 300)
        r["app_switch_count"] = rng.poisson(1.5)
        r["unique_app_count"] = 1 + rng.poisson(0.5)
        r["category"] = rng.choice(["work", "video", "chat"], p=[0.6, 0.25, 0.15])
        r["activity"] = "still"
        r["minutes_in_current_activity"] = rng.uniform(30, 150)
        r["hour"] = rng.uniform(9, 19)
    elif scenario == "fragmented":
        r["continuous_screen_on_min"] = rng.uniform(5, 30)
        r["foreground_duration_sec"] = rng.uniform(150, 300)
        r["app_switch_count"] = rng.poisson(9)
        r["unique_app_count"] = 3 + rng.poisson(2.5)
        r["category"] = rng.choice(["work", "chat", "social"], p=[0.4, 0.3, 0.3])
        r["activity"] = rng.choice(["still", "unknown"], p=[0.8, 0.2])
        r["minutes_in_current_activity"] = rng.uniform(10, 90)
        r["hour"] = rng.uniform(8, 22)
    elif scenario == "casual":
        r["continuous_screen_on_min"] = rng.uniform(2, 25)
        r["foreground_duration_sec"] = rng.uniform(60, 280)
        r["app_switch_count"] = rng.poisson(4)
        r["unique_app_count"] = 1 + rng.poisson(1.5)
        r["category"] = rng.choice(["social", "video", "chat", "other"], p=[0.35, 0.3, 0.2, 0.15])
        r["activity"] = rng.choice(["still", "unknown"], p=[0.7, 0.3])
        r["minutes_in_current_activity"] = rng.uniform(5, 60)
        r["hour"] = rng.uniform(7, 23)
    elif scenario == "late_night":
        r["continuous_screen_on_min"] = rng.uniform(15, 75)
        r["foreground_duration_sec"] = rng.uniform(200, 300)
        r["app_switch_count"] = rng.poisson(5)
        r["unique_app_count"] = 1 + rng.poisson(1.5)
        r["category"] = rng.choice(["social", "video", "game_dating"], p=[0.45, 0.35, 0.2])
        r["activity"] = "still"
        r["minutes_in_current_activity"] = rng.uniform(30, 180)
        r["hour"] = rng.choice([23.5, 0.5, 1.5, 2.5, 4.0])
    elif scenario == "on_the_move":
        r["continuous_screen_on_min"] = rng.uniform(1, 12)
        r["foreground_duration_sec"] = rng.uniform(30, 200)
        r["app_switch_count"] = rng.poisson(3)
        r["unique_app_count"] = 1 + rng.poisson(1)
        r["category"] = rng.choice(["chat", "social", "other"], p=[0.4, 0.3, 0.3])
        r["activity"] = "on_foot"
        r["minutes_in_current_activity"] = rng.uniform(2, 40)
        r["hour"] = rng.uniform(7, 21)
    else:  # idle_pickup — compulsive checking pattern
        r["continuous_screen_on_min"] = rng.uniform(0.5, 5)
        r["foreground_duration_sec"] = rng.uniform(20, 120)
        r["app_switch_count"] = rng.poisson(2)
        r["unique_app_count"] = 1 + rng.poisson(1)
        r["category"] = rng.choice(["social", "chat", "other"], p=[0.5, 0.3, 0.2])
        r["activity"] = rng.choice(["still", "unknown"], p=[0.6, 0.4])
        r["minutes_in_current_activity"] = rng.uniform(1, 30)
        r["hour"] = rng.uniform(6, 24) % 24

    r["longest_session_sec"] = min(1800.0, r["continuous_screen_on_min"] * 60 * rng.uniform(0.5, 1.0))
    r["screen_on_time_sec"] = min(300.0, r["foreground_duration_sec"] * rng.uniform(0.9, 1.1))
    r["distracting_return_count"] = rng.poisson(2.0 if r["category"] in ("social", "game_dating") else 0.3)
    r["cold_open_count"] = rng.poisson(4.0 if scenario == "idle_pickup" else 1.0)
    r["unlock_count_last_hour"] = rng.poisson(8.0 if scenario == "idle_pickup" else 3.0)
    r["day_of_week"] = rng.integers(0, 7)
    r["charging"] = rng.random() < (0.35 if r["activity"] == "still" else 0.05)
    r["battery_pct"] = rng.uniform(10, 100)
    r["min_since_first_unlock"] = _hour_to_awake_minutes(r["hour"], rng)
    r["min_since_last_break"] = rng.exponential(90)
    r["dismiss_count_24h"] = rng.poisson(1.2)
    r["snooze_count_24h"] = rng.poisson(0.6)
    # Per-user personalization block: sample a persona and derive consistent rates
    persona = rng.beta(2.5, 2.5)  # user's general receptivity 0..1
    r["accept_rate_7d"] = _clip01(persona + rng.normal(0, 0.08))
    r["accept_rate_this_hour"] = _clip01(persona + rng.normal(0, 0.18))
    r["avg_response_delay_sec"] = rng.uniform(5, 120) * (1.5 - persona)
    r["break_completion_rate"] = _clip01(persona + rng.normal(0, 0.10))
    r["persona"] = persona
    r["late_night"] = 1.0 if (r["hour"] >= 23 or r["hour"] < 5) else 0.0
    return r


def _hour_to_awake_minutes(hour, rng):
    wake = rng.uniform(6, 9)
    awake = (hour - wake) % 24
    return max(0.0, awake * 60)


def featurize(r: dict) -> np.ndarray:
    """Raw dict -> normalized 34-float vector. Mirrors FeatureBuilder.kt exactly."""
    f = np.zeros(FEATURE_COUNT, dtype=np.float64)
    f[0] = _clip01(r["foreground_duration_sec"] / 300)
    f[1] = _clip01(r["app_switch_count"] / 20)
    f[2] = _clip01(r["unique_app_count"] / 10)
    f[3] = _clip01(r["longest_session_sec"] / 1800)
    f[4] = _clip01(r["screen_on_time_sec"] / 300)
    f[5] = _clip01(r["continuous_screen_on_min"] / 60)
    f[6] = _clip01(r["distracting_return_count"] / 5)
    f[7] = _clip01(r["cold_open_count"] / 10)
    f[8] = _clip01(r["unlock_count_last_hour"] / 15)
    f[9 + CATEGORIES.index(r["category"])] = 1.0
    f[15] = np.sin(2 * np.pi * r["hour"] / 24)
    f[16] = np.cos(2 * np.pi * r["hour"] / 24)
    f[17] = np.sin(2 * np.pi * r["day_of_week"] / 7)
    f[18] = np.cos(2 * np.pi * r["day_of_week"] / 7)
    f[19] = r["late_night"]
    f[20 + ACTIVITIES.index(r["activity"])] = 1.0
    f[23] = _clip01(r["minutes_in_current_activity"] / 120)
    f[24] = 1.0 if r["charging"] else 0.0
    f[25] = _clip01(r["battery_pct"] / 100)
    f[26] = _clip01(r["min_since_first_unlock"] / 960)
    f[27] = _clip01(r["min_since_last_break"] / 240)
    f[28] = _clip01(r["dismiss_count_24h"] / 5)
    f[29] = _clip01(r["snooze_count_24h"] / 5)
    f[30] = r["accept_rate_7d"]
    f[31] = r["accept_rate_this_hour"]
    f[32] = _clip01(r["avg_response_delay_sec"] / 120)
    f[33] = r["break_completion_rate"]
    return f


def true_accept_logit(r: dict, rng: np.random.Generator) -> float:
    """Hand-designed latent receptivity. NOT the rules engine — deliberately
    richer (persona interactions, saturating focus pressure) so the model has
    something beyond the rules to learn."""
    focus = _clip01(r["continuous_screen_on_min"] / 60)
    switch = _clip01(r["app_switch_count"] / 20)
    logit = (
        -1.3
        + 2.3 * focus * (1.0 - 0.5 * focus)          # rises then saturates
        + 1.0 * switch
        + 0.8 * (r["activity"] == "still")
        - 1.6 * (r["activity"] == "on_foot")
        + 0.5 * r["charging"]
        - 0.9 * r["late_night"]
        + 2.2 * (r["accept_rate_this_hour"] - 0.5)
        + 1.4 * (r["persona"] - 0.5)
        - 1.1 * _clip01(r["dismiss_count_24h"] / 5)
        - 1.0 * max(0.0, 1.0 - r["min_since_last_break"] / 45)  # cooldown fatigue
        + 0.4 * _clip01(r["cold_open_count"] / 10)               # checking = reachable
    )
    return logit + rng.normal(0, 0.35)


def generate(n: int, seed: int = 7):
    rng = np.random.default_rng(seed)
    names = list(SCENARIOS.keys())
    probs = np.array(list(SCENARIOS.values()))
    X = np.zeros((n, FEATURE_COUNT))
    y = np.zeros(n)
    for i in range(n):
        scenario = rng.choice(names, p=probs)
        r = sample_raw(rng, scenario)
        X[i] = featurize(r)
        p = 1.0 / (1.0 + np.exp(-true_accept_logit(r, rng)))
        y[i] = 1.0 if rng.random() < p else 0.0
    return X, y
