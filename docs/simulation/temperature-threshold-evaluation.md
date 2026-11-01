# SIM-1.3: Evaluate alert thresholds

Date: 2026-10-30. Parent story: SIM-1.

## Recommendation

Use **temperature >4°C for two consecutive elapsed minutes**, with an active alert clearing at **≤3.5°C**, as the proposed policy for application implementation and subsequent hardware testing.

This is an engineering recommendation based on synthetic inputs, not a validated food-safety policy. No Android or firmware behavior is changed by this evaluation.

## Evidence and decision

The [SIM-1.2 experiment](temperature-threshold-results.md) evaluated 18 policies against 12 scenarios. All 216 comparisons matched the reference calculation. The [result CSV](results/temperature-policy-results.csv) records the individual events and input/script hashes. Agreement verifies calculation consistency; scenario behavior supplies the basis for selecting a policy.

The selection priorities are to detect the sustained warming scenarios, reject the supplied brief spike and isolated outlier, avoid repeated alerts from boundary noise, and minimize delay among policies meeting those goals.

### Temperature limit

With two-minute persistence and 0.5°C hysteresis:

| Scenario | >4°C | >5°C | >6°C |
| --- | --- | --- | --- |
| TEMP-03: 4.1°C boundary hold | Alert at minute 22 | Missed | Missed |
| TEMP-05: sustained 6°C | Alert at minute 12 | Alert at minute 12 | Missed |
| TEMP-06: gradual warming | Alert at minute 17 | Alert at minute 21 | Alert at minute 25 |
| TEMP-07: sustained 15°C | Alert at minute 12 | Alert at minute 12 | Alert at minute 12 |
| TEMP-12: 6°C after missing readings | Alert at minute 17 | Alert at minute 17 | Missed |

Select >4°C because it detects every supplied sustained warm case, including the 4.1°C hold, and detects the gradual rise four minutes earlier than >5°C and eight earlier than >6°C. The >5°C alternative gains no additional rejection of the supplied transient cases when two-minute persistence is already used. Strict >6°C excludes readings equal to 6°C and therefore misses two relevant warming cases.

### Persistence

At a 4°C limit and 0.5°C hysteresis:

| Observation | 0 minutes | 2 minutes | 5 minutes |
| --- | --- | --- | --- |
| TEMP-04: brief 8°C spike | Alerts | Rejected | Rejected |
| TEMP-11: isolated 25°C reading | Alerts | Rejected | Rejected |
| TEMP-09: boundary noise | Alerts and stays active | Rejected | Rejected |
| TEMP-05: sustained 6°C | Minute 10 | Minute 12 | Minute 15 |
| TEMP-06: gradual warming | Minute 15 | Minute 17 | Minute 20 |

Select two minutes: it rejects all three supplied nuisance inputs, while five minutes adds three minutes of detection delay without further benefit in these cases. This result does not establish that two minutes rejects all real door-opening events.

### Clearing hysteresis

Select 0.5°C as a recovery margin, clearing at ≤3.5°C. In TEMP-08, it clears at minute 33 rather than minute 31 with zero hysteresis, requiring a cooler recovery reading. This margin reduces immediate reactivation opportunities near 4°C in principle, but the existing cases do not demonstrate fewer notifications with two-minute persistence: both hysteresis settings produce the same alert counts. Confirm the margin with additional recovery/rebound inputs and actual sensor noise before finalizing it.

## Expected behavior of the proposed policy

All times below are elapsed minutes from each independent scenario's start.

| Scenario | Warm alert | Clear | Interpretation |
| --- | --- | --- | --- |
| TEMP-01: steady normal | None | — | No nuisance alert |
| TEMP-02: normal cycling | None | — | 4°C does not qualify |
| TEMP-03: boundary hold | 22 | None in window | Detects sustained 4.1°C |
| TEMP-04: brief spike | None | — | Persistence filters spike |
| TEMP-05: sustained warming | 12 | None in window | Detects sustained 6°C |
| TEMP-06: gradual warming | 17 | None in window | Detects rising temperature |
| TEMP-07: severe warming | 12 | None in window | One activation; no reminders modeled |
| TEMP-08: cooling recovery | 2 | 33 | Clears when recovery reaches ≤3.5°C |
| TEMP-09: boundary noise | None | — | Qualifying run repeatedly resets |
| TEMP-10: cold excursion | None | — | No low-temperature policy evaluated |
| TEMP-11: single outlier | None | — | Persistence filters outlier |
| TEMP-12: missing then warm | 17 | None in window | Stale at 11; fresh at 15 |

The proposed policy generates six warm activations across the 12 independent cases, with no warm activation in the normal, brief-spike, boundary-noise, cold-excursion, or isolated-outlier cases. A missing clear means no qualifying recovery reading occurred before the case ended.

## Implementation contract and follow-up

- Compare unrounded Celsius values: activate only above 4°C; clear at or below 3.5°C.
- Start persistence at the first qualifying reading. At one-minute sampling, readings at minutes 10, 11, and 12 must all qualify for activation at minute 12. Two samples at minutes 10 and 11 are insufficient.
- Reset pending persistence on a nonqualifying or missing reading. Preserve an already active warm alert during missing readings.
- Track stale data separately: flag it once two minutes have elapsed since the last valid reading, and clear staleness at the next valid reading. Do not convert missing readings to 0°C.
- Emit one notification per warm activation. Reminder frequency and low-temperature alerts are outside this experiment.
- Before final deployment, exercise gaps during pending and active alerts, recovery followed by repeated rebounds, irregular sampling, and real sensor noise. Verify Android notifications and displayed readings against the same event expectations.

SIM-1.3's local analysis and threshold proposal are complete. Team approval, application integration, and physical validation remain separate follow-up work; no GitHub issue status is changed here.
