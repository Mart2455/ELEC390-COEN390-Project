# Temperature test scenarios

This plan defines hardware-free inputs for [SIM-1.1, issue #10](https://github.com/Mart2455/ELEC390-COEN390-Project/issues/10). SIM-1.2 will execute the scenarios and record results; SIM-1.3 will use those results to propose alert thresholds. 

## Scope and assumptions

- Simulate the temperature of the air near one fridge sensor, in degrees Celsius. These inputs do not model food core temperature, microbial growth, or actual sensor accuracy.
- Use 3°C as the nominal baseline and 2–4°C for normal operating test inputs, consistent with the project's cold-environment test scope in SIM-4. Health Canada recommends a refrigerator setting of 4°C or lower; the 2°C lower bound is a project test choice.
- All rates, durations, noise patterns, and failure values below are synthetic engineering inputs, not measured refrigerator behavior.
- A single zone is sufficient for these tests. Reset temperature history, pending alerts, and timers before each independent case.
- No hardware or Android changes are required to define these cases. The current Android temperature display is hardcoded.

## Reproducible input format

The companion CSV at `../../tests/simulation/temperature-scenarios.csv` defines sequential phases for every case below.

Each phase contains `sample_count` samples. The first phase begins at elapsed time 0; the next phase starts one sample interval after the previous phase's last sample.

Interpret `mode` as follows:

- `constant`: repeat `start_c`; `end_c` must match it.
- `linear`: for sample index i from 0 through n-1, emit `start_c + (end_c - start_c) * i / (n - 1)`. Both endpoints are included; n must be at least 2.
- `cycle`: repeat `values_c`, a semicolon-separated numeric sequence, until `sample_count` samples are produced. Other temperature fields are blank.
- `missing`: advance the virtual clock but emit no temperature reading. Temperature fields are blank. This represents missing telemetry, not 0°C.

Keep full numeric precision for comparisons; rounding the displayed temperature must not change a threshold decision. The nominal sample interval is a simulation choice, not a firmware requirement.

## Scenario matrix

Durations refer to observation windows, including the final sample interval. CSV phase definitions are authoritative for exact sample values.

| ID | Condition and input sequence | Window | Observation to record during SIM-1.2 |
| --- | --- | --- | --- |
| TEMP-01 | Normal steady operation: 3°C | 60 min | Baseline display and any unexpected warm alerts. |
| TEMP-02 | Normal cycling: repeat 2, 2.5, 3, 3.5, 4, 3.5, 3, 2.5°C | 120 min | Behavior across the full normal test range; count nuisance alerts. |
| TEMP-03 | Reference boundary: hold 3.9, 4.0, then 4.1°C for 10 min each | 30 min | Exact comparison behavior at and either side of 4°C; distinguish `>` from `>=`. |
| TEMP-04 | Brief door-opening-like spike: 3°C for 10 min, 8°C for 2 min, 3°C for 20 min | 32 min | Sensitivity to a short excursion and return to baseline. |
| TEMP-05 | Sustained moderate warming: 3°C for 10 min, 6°C for 30 min | 40 min | Whether a sustained excursion is detected; detection delay. |
| TEMP-06 | Gradual warming: 3°C for 10 min, linear 3→10°C over 31 samples, then 10°C for 30 min | 71 min | Threshold crossing and alert delay under a gradual rise. |
| TEMP-07 | Prolonged severe warming: 3°C for 10 min, 15°C for 120 min | 130 min | Persistent warning behavior and repeated notification count. |
| TEMP-08 | Cooling recovery: 8°C for 15 min, linear 8→3°C over 21 samples, then 3°C for 20 min | 56 min | When the alert clears, and whether it returns after stable recovery. |
| TEMP-09 | Boundary noise: repeat 3.8, 4.2, 3.9, 4.1, 4.0°C | 60 min | Alert toggling near 4°C; compare hysteresis and persistence choices. |
| TEMP-10 | Overcooling: 3°C for 10 min, 0°C for 10 min, -2°C for 10 min, 3°C for 10 min | 40 min | Preserve zero/negative readings and distinguish a cold excursion from warming. A low-temperature alert is optional and separate from the warm-alert study. |
| TEMP-11 | Single reading outlier: 3°C for 10 min, 25°C for one sample, 3°C for 20 min | 31 min | Response to an isolated extreme value; retain raw input even if filtering is evaluated. |
| TEMP-12 | Missing readings: 3°C for 10 min, no readings for 5 min, 6°C for 15 min | 30 min | Data freshness and recovery; missing data must not be converted to a healthy 0°C reading. |

The door-opening, failure, and recovery names describe the intended stress case; they do not establish a physical cause from temperature alone.

## Execution and result recording for SIM-1.2

1. Expand each CSV case into timestamped readings using the rules above. Feed it to a simulator or a testable temperature/alert component; no ESP32 is needed.
2. First validate timestamps, values, phase boundaries, and gaps independently of any alert policy.
3. Evaluate candidate warm-alert policies using the same inputs. Starting comparison parameters may include limits of 4, 5, and 6°C; persistence of 0, 2, and 5 simulated minutes; and clearing hysteresis of 0 or 0.5°C. These are experiment choices, not approved food-safety limits.
4. Record the exact policy: comparison operator, persistence rule, clearing rule, missing-data timeout, and behavior on gaps. For a reproducible initial policy, reset pending warm persistence on missing data, preserve an already-active warm alert until valid readings satisfy the clearing rule, and maintain data freshness separately. A 2-minute missing-data timeout may be evaluated as a prototype choice.
5. Define persistence as elapsed virtual time since the first qualifying sample; fire only on a qualifying sample at or after that duration. For the initial comparison, clear on the first valid reading at or below `limit - hysteresis`. Use each policy to calculate expected transition timestamps before comparing actual output.
6. Record the scenario ID, input version/commit, simulator version, policy parameters, expected and actual transitions, first alert time, clearing time, notification count, and pass/fail against that policy. Leave results blank until execution.
7. Carry the measured tradeoffs and any failures into SIM-1.3. Short spikes, sustained warming, and boundary noise should be compared explicitly when selecting a policy.

No temperature scenario by itself proves that food is fresh or safe to eat. In particular, a return to a lower air temperature does not establish that earlier exposure has been reversed.

## SIM-1.1 completion criteria

- Normal and warmer-than-normal conditions are defined with stable identifiers.
- Boundary, transient, sustained, recovery, noise, cold, outlier, and missing-data cases have repeatable inputs.
- Units, timing, reset rules, observations, and the handoff to SIM-1.2/SIM-1.3 are specified.
- The CSV is checked for unique ordered phases, valid mode fields, sample counts, and consistency with this matrix.
- Team review remains necessary before checking the GitHub issue's review/test box. Simulation execution belongs to SIM-1.2.

## References

- [SIM-1.1: Define Temperature Test Scenarios](https://github.com/Mart2455/ELEC390-COEN390-Project/issues/10)
- [SIM-1.2: Test Temperature Alert Thresholds](https://github.com/Mart2455/ELEC390-COEN390-Project/issues/11)
- [SIM-1.3: Evaluate Alert Thresholds](https://github.com/Mart2455/ELEC390-COEN390-Project/issues/12)
- [SIM-4.1: Set Up Sensor Cold Environment Test](https://github.com/Mart2455/ELEC390-COEN390-Project/issues/13)
- [Health Canada: Safe food handling in the home, Refrigerator](https://www.canada.ca/en/health-canada/services/general-food-safety-tips/safe-food-handling-home.html)
