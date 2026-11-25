"""Generate repeatable synthetic /sensors gas fixtures and compare scenarios."""

import argparse
import csv
import json
from pathlib import Path
from random import Random
from statistics import mean, stdev

INTERVAL = 5
DURATION = 300
CHANGE_START = 120
RECOVERY = 180
SEEDS = (390, 391, 392)
SCENARIOS = ("baseline", "apple", "banana", "missing")
CHANNELS = {
    "gasResistance": ("kOhm", 24.0, 0.3),
    "eco2": ("ppm", 420, 8),
    "tvoc": ("ppb", 35, 4),
    "mq3Raw": ("ADC counts", 1500, 10),
}


def generate(scenario, seed):
    random = Random(seed)
    response_scale = (0.9, 1.0, 1.1)[SEEDS.index(seed)]
    for second in range(0, DURATION, INTERVAL):
        values = {
            "gasResistance": round(24.0 + random.uniform(-0.3, 0.3), 2),
            **{
                channel: center + random.randint(-noise, noise)
                for channel, (_, center, noise) in CHANNELS.items()
                if channel != "gasResistance"
            },
        }
        if second >= CHANGE_START:
            step = 1 + (second - CHANGE_START) // INTERVAL
            if scenario == "apple":
                values["tvoc"] += round(response_scale * 3 * step) - (12 if step % 6 == 0 else 0)
                values["mq3Raw"] += round(response_scale * 6 * step)
            elif scenario == "banana":
                values["tvoc"] += round(response_scale * 2 * step)
                values["mq3Raw"] += round(response_scale * 9 * step)
                values["gasResistance"] = round(values["gasResistance"] - response_scale * 0.02 * step, 2)
            elif scenario == "missing" and second < RECOVERY:
                values["eco2"] = values["tvoc"] = None
        yield {
            "source": "synthetic",
            "scenario": scenario,
            "seed": seed,
            "elapsed_s": second,
            **values,
        }


def after_change(rows, channel):
    return [
        row[channel]
        for row in rows
        if row["elapsed_s"] >= CHANGE_START and row[channel] is not None
    ]


def summarize(runs):
    print("SIM-3 gas simulation | Synthetic data | 3 runs per scenario")
    print("Change = mean from 120 s onward minus the matching baseline mean.")
    print("Ranges show the lowest and highest change across the three seeds.")
    for scenario in SCENARIOS:
        print(f"\n{scenario}")
        for channel, (unit, _, _) in CHANNELS.items():
            series = [after_change(runs[scenario, seed], channel) for seed in SEEDS]
            reference = [after_change(runs["baseline", seed], channel) for seed in SEEDS]
            changes = [mean(values) - mean(base) for values, base in zip(series, reference)]
            missing = sum(
                row[channel] is None for seed in SEEDS for row in runs[scenario, seed]
            )
            total = len(SEEDS) * len(runs[scenario, SEEDS[0]])
            print(
                f"  {channel:14} delta {min(changes):+7.2f} to {max(changes):+7.2f} {unit:10}"
                f" | missing {missing}/{total}"
            )

    print("\nBaseline noise (mean within-run SD) and drift (last minute minus first)")
    for channel, (unit, _, _) in CHANNELS.items():
        baseline = [[row[channel] for row in runs["baseline", seed]] for seed in SEEDS]
        minute = 60 // INTERVAL
        drift = [mean(values[-minute:]) - mean(values[:minute]) for values in baseline]
        print(
            f"  {channel:14} SD {mean(map(stdev, baseline)):7.2f} {unit:10}"
            f" | drift {min(drift):+7.2f} to {max(drift):+7.2f}"
        )

    dips = 0
    for seed in SEEDS:
        values = after_change(runs["apple", seed], "tvoc")
        dips += sum(next_value < value for value, next_value in zip(values, values[1:]))
    print(f"\nApple TVOC declined on {dips} adjacent sample pairs after 120 s.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    output = parser.add_mutually_exclusive_group()
    output.add_argument("--csv", type=Path, help="Write all synthetic runs as CSV.")
    output.add_argument("--replay", choices=SCENARIOS, help="Emit one run as JSON Lines.")
    parser.add_argument("--seed", type=int, choices=SEEDS, default=SEEDS[0])
    args = parser.parse_args()

    if args.replay:
        for row in generate(args.replay, args.seed):
            print(json.dumps(row))
        return

    runs = {
        (scenario, seed): list(generate(scenario, seed))
        for scenario in SCENARIOS
        for seed in SEEDS
    }
    if args.csv:
        with args.csv.open("w", newline="", encoding="utf-8") as file:
            writer = csv.DictWriter(file, fieldnames=("source", "scenario", "seed", "elapsed_s", *CHANNELS))
            writer.writeheader()
            for rows in runs.values():
                writer.writerows(rows)
    summarize(runs)


if __name__ == "__main__":
    main()
