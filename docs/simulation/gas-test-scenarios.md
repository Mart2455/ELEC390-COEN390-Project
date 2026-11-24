# SIM-3 Gas sensor simulation

## SIM-3.1 Define gas test scenarios

Each synthetic run lasts five minutes, with 60 samples at five-second intervals starting at 0 s. This models the Android service's five-second delay between polling attempts. Each scenario uses seeds 390, 391, and 392. The changing scenarios use response multipliers of 0.9, 1.0, and 1.1 respectively.

| ESP32 field | Sensor | Unit | Synthetic baseline |
| --- | --- | --- | ---: |
| `gasResistance` | BME680 | kΩ | 24.0 |
| `eco2` | CCS811 | ppm | 420 |
| `tvoc` | CCS811 | ppb | 35 |
| `mq3Raw` | MQ-3 | Raw ADC counts | 1500 |

| Scenario | Scripted condition |
| --- | --- |
| Baseline | All channels fluctuate around their starting values for the full run. |
| Apple | At 120 s, TVOC and MQ-3 start rising. TVOC has a scripted dip every sixth affected sample. |
| Banana | At 120 s, TVOC rises less steeply, MQ-3 rises faster, and gas resistance trends down. |
| Missing readings | From 120 s through 175 s, both CCS811 fields are unavailable. They return at 180 s. The other two channels continue as in the baseline. |

Apple and banana are labels for deliberately different synthetic profiles. The firmware supplies the channel names and units and returns both CCS811 readings as JSON null when CCS811 data is unavailable.

## SIM-3.2 Generate and replay readings

The [dataset](../../tests/simulation/gas-scenarios.csv) contains 720 samples. Each row is labeled synthetic and includes the scenario, seed, elapsed seconds, and four sensor fields. Missing readings are blank in the CSV and null in JSON Lines. All values were generated in Python. Hardware collection is pending.

Run these commands from the repository root:

```sh
python3 tests/simulation/sim3_gas_demo.py
python3 tests/simulation/sim3_gas_demo.py --csv tests/simulation/gas-scenarios.csv
python3 tests/simulation/sim3_gas_demo.py --replay missing --seed 390
```

Replay emits timestamped fixtures for test code to consume. Each line includes metadata alongside the four gas fields.

## SIM-3.3 Analyze gas patterns

The script compares the mean from 120 s onward against the same period of the baseline run with the same seed. It excludes missing samples from the means and reports their counts separately.

| Synthetic profile | Mean TVOC difference | Mean MQ-3 difference | Other observations |
| --- | ---: | ---: | --- |
| Apple | +47.94 to +59.06 ppb | +99.89 to +122.11 counts | TVOC fell between adjacent samples 33 times across the runs despite the overall rise. |
| Banana | +33.31 to +40.69 ppb | +149.83 to +183.17 counts | Gas resistance fell by 0.33–0.41 kΩ on average. |
| Missing readings | +0.03 to +0.17 ppb among available samples | 0 counts | Each CCS811 field was missing 12 times per run. |

The baseline's average within-run TVOC standard deviation was 2.71 ppb. The last-minute mean differed from the first-minute mean by −2.08 to 0.00 ppb across baseline runs. That apparent drift came from the scripted noise. The missing scenario's small mean differences came from omitting samples during the outage. Both food profiles rose across all seeds by design, and their differences reflect the input assumptions.

## SIM-3.4 Document feasibility and limitations

The fixtures support exercising three candidate app states:

- **Reading unavailable** when a signal required by the rule is null. Resume evaluation when readings return.
- **Check Food** when a sustained rise in TVOC and MQ-3 exceeds a provisional baseline-relative threshold. For a synthetic exercise, try at least +25 ppb TVOC and +60 raw counts for three consecutive samples. These are test inputs for a candidate rule.
- **Normal conditions** when required readings are available and the candidate change rule does not trigger.

Real-food spoilage, freshness, safety, and food identification all require separate evidence. Before assigning hardware thresholds, collect repeated empty-fridge baselines and repeated trials for each food under comparable conditions. Record sensor warm-up, temperature, humidity, and events such as door openings. Compare within-food and between-food variation and use separate runs to estimate false alarms and missed detections against an independently defined reference.
