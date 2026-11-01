# SIM-1.2: Temperature alert threshold experiment

Parent story: SIM-1. Task: run the temperature scenarios and document potential thresholds for triggering alerts.

## Execution and scope

Executed 2026-10-30 using Python 3 and `tests/simulation/run_temperature_scenarios.py` version 1 (identified precisely by its SHA-256 in every result row). Repository baseline: `27eadd0c012f7fb4b8fb551b651b518d9af6f492`; simulation files were uncommitted when executed.

Reproduce from the repository root:

```sh
python3 tests/simulation/run_temperature_scenarios.py
```

All 12 cases expanded successfully into 700 one-minute clock slots, including five missing-reading slots. Validation checked scenario IDs, ordered phases, mode fields, positive sample counts, observation windows, ramp endpoints, missing readings, and preservation of zero and negative temperatures. Decimal arithmetic avoids display rounding in comparisons. Each case starts at minute 0 with fresh state.

The experiment evaluated 18 policies: limits of 4, 5, and 6°C; persistence of 0, 2, and 5 minutes; hysteresis of 0 and 0.5°C. All 216 case/policy comparisons matched an independent offline qualifying-run calculation. Exact expected and observed simulator transitions, first alert/clear times, notification counts, and input/simulator hashes are in [the result CSV](results/temperature-policy-results.csv).

These are standalone simulator results. Android display/notification integration has not been executed: the layouts display a literal 3°C, and the available receiver stores temperature without an implemented warm-alert policy. Passing here verifies the experimental policy model, not Android behavior, sensor accuracy, or food freshness.

## Policy semantics

A reading qualifies when temperature is strictly greater than the limit. Persistence measures elapsed virtual time from the first consecutive qualifying sample, firing only on a qualifying sample at or after the selected duration. A nonqualifying or missing sample resets pending persistence. An active alert clears on the first valid reading at or below `limit - hysteresis`; missing data preserves an active alert. Each inactive-to-active transition counts as one simulated notification; reminders are not modeled.

Freshness is tracked separately: data becomes stale when time since the last valid reading reaches two minutes, and fresh at the next valid reading. In TEMP-12 this means stale at minute 11 and fresh at minute 15. The virtual clock advances during missing phases. No end-of-window synthetic reading or clearing event is added. Events use elapsed minutes, not wall-clock time.

## Candidate comparison

With two-minute persistence and 0.5°C clearing hysteresis:

| Observation | >4°C | >5°C | >6°C |
| --- | --- | --- | --- |
| Normal steady/cycling | No alerts | No alerts | No alerts |
| 4.1°C boundary hold | Alert at 22 min | No alert | No alert |
| Two-sample 8°C spike | No alert | No alert | No alert |
| Sustained 6°C | Alert at 12 min | Alert at 12 min | No alert |
| Gradual warming | Alert at 17 min | Alert at 21 min | Alert at 25 min |
| Severe warming | Alert at 12 min | Alert at 12 min | Alert at 12 min |
| Cooling recovery | Alert at 2; clear at 33 min | Alert at 2; clear at 29 min | Alert at 2; clear at 25 min |
| Boundary noise | No alerts | No alerts | No alerts |
| Cold excursion / single outlier | No warm alerts | No warm alerts | No warm alerts |
| Warming after missing readings | Alert at 17 min | Alert at 17 min | No warm alert |

A two-sample spike at minutes 10 and 11 lasts only one elapsed minute between qualifying samples, so it cannot satisfy two-minute persistence. This follows the plan's elapsed-time definition.

Zero persistence responds to brief spikes and the isolated outlier. At a 4°C limit with zero persistence and zero hysteresis, boundary noise repeatedly activates and clears alerts; 0.5°C hysteresis instead leaves the first alert active because this case never reaches 3.5°C. Persistence filters that particular noise sequence before activation. Five-minute persistence further delays sustained detection compared with two minutes. Hysteresis changes recovery behavior; it does not itself reject an initial noisy reading.

## Potential thresholds for SIM-1.3

The completed [SIM-1.3 evaluation and proposed policy](temperature-threshold-evaluation.md) compares these candidates and records the implementation contract and remaining validation.

Carry forward **>4°C for two consecutive elapsed minutes, clearing at ≤3.5°C**, as an initial engineering candidate. It detects the sustained warming inputs while rejecting this plan's short spike, outlier, and boundary noise. Keep **>5°C for two minutes, clearing at ≤4.5°C**, as a comparison candidate: it ignores the 4.1°C hold and detects gradual warming four minutes later. Strict **>6°C** misses the sustained 6°C cases and is unsuitable if those cases must trigger an alert.

These parameters are synthetic experiment choices, not approved food-safety limits or a final threshold selection. The scenarios do not establish acceptable exposure durations. SIM-1.3 should review the missed excursions and detection delays before selecting a policy.

## Remaining application validation

Feed the same inputs into a testable application temperature/alert component once available. Compare displayed values, actual notification counts, warm transitions, and freshness transitions against the recorded expectations. Verify active-alert gaps and interrupted pending persistence with additional cases; TEMP-12 covers a gap before warming but does not exercise those two branches. Team review and Android integration testing remain open.
