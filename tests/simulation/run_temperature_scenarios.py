"""SIM-1.2 synthetic policy experiment. Run with Python 3; no dependencies."""
import csv
import hashlib
import itertools
import json
from decimal import Decimal as D
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).with_name('temperature-scenarios.csv')
OUT = ROOT / 'docs/simulation/results'
WINDOWS = [60, 120, 30, 32, 40, 71, 130, 56, 60, 40, 31, 30]


def expand():
    cases = {}
    phases = {}
    for row in csv.DictReader(SOURCE.open()):
        sid = row['scenario_id']
        samples = cases.setdefault(sid, [])
        phase, n = int(row['phase']), int(row['sample_count'])
        assert phase == phases.get(sid, 0) + 1 and n > 0
        phases[sid] = phase
        mode = row['mode']
        if mode in ('constant', 'linear'):
            assert not row['values_c']
            a, b = D(row['start_c']), D(row['end_c'])
            if mode == 'constant':
                assert a == b
                values = [a] * n
            else:
                assert n >= 2
                values = [a + (b-a)*i/(n-1) for i in range(n)]
        elif mode == 'cycle':
            assert not row['start_c'] and not row['end_c']
            cycle = list(map(D, row['values_c'].split(';')))
            values = [cycle[i % len(cycle)] for i in range(n)]
        else:
            assert mode == 'missing'
            assert not any(row[k] for k in ('start_c', 'end_c', 'values_c'))
            values = [None] * n
        assert all(v is None or v.is_finite() for v in values)
        samples.extend(values)
    assert list(cases) == [f'TEMP-{i:02}' for i in range(1, 13)]
    assert list(map(len, cases.values())) == WINDOWS
    assert cases['TEMP-12'][10:15] == [None]*5
    assert cases['TEMP-06'][10] == 3 and cases['TEMP-06'][40] == 10
    assert cases['TEMP-08'][15] == 8 and cases['TEMP-08'][35] == 3
    assert 0 in cases['TEMP-10'] and -2 in cases['TEMP-10']
    return cases


def simulate(values, limit, persistence, hysteresis):
    active, pending, last_valid, stale = False, None, None, False
    events = []
    for minute, value in enumerate(values):
        if value is None:
            pending = None
            if last_valid is not None and minute-last_valid >= 2 and not stale:
                stale = True
                events.append([minute, 'stale'])
            continue
        last_valid = minute
        if stale:
            events.append([minute, 'fresh'])
            stale = False
        if active:
            if value <= limit-hysteresis:
                active, pending = False, None
                events.append([minute, 'clear'])
        elif value > limit:
            if pending is None:
                pending = minute
            if minute-pending >= persistence:
                active = True
                events.append([minute, 'warm'])
        else:
            pending = None
    return events


def expected(values, limit, persistence, hysteresis):
    # Independent offline calculation: find qualifying runs, then clearing samples.
    events, cursor = [], 0
    while cursor < len(values):
        fire = None
        for start in range(cursor, len(values)):
            end = start + persistence
            if end < len(values) and all(v is not None and v > limit for v in values[start:end+1]):
                fire = end
                break
        if fire is None:
            break
        events.append([fire, 'warm'])
        clear = next((t for t in range(fire+1, len(values)) if values[t] is not None and values[t] <= limit-hysteresis), None)
        if clear is None:
            break
        events.append([clear, 'clear'])
        cursor = clear+1
    last, stale = None, False
    for t, v in enumerate(values):
        if v is not None:
            if stale:
                events.append([t, 'fresh'])
            last, stale = t, False
        elif last is not None and t-last >= 2 and not stale:
            events.append([t, 'stale'])
            stale = True
    return sorted(events, key=lambda event: (event[0], {'stale': 0, 'fresh': 0, 'warm': 1, 'clear': 1}[event[1]]))


def main():
    cases = expand()
    OUT.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    fields = ['scenario', 'limit_c', 'persistence_min', 'hysteresis_c', 'expected_events', 'actual_events', 'first_alert_min', 'first_clear_min', 'notification_count', 'result', 'input_sha256', 'simulator_sha256']
    simulator_digest = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    with (OUT/'temperature-policy-results.csv').open('w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        count = 0
        for limit, persistence, hysteresis in itertools.product([4,5,6], [0,2,5], [D('0'),D('0.5')]):
            for sid, values in cases.items():
                actual = simulate(values, D(limit), persistence, hysteresis)
                reference = expected(values, D(limit), persistence, hysteresis)
                assert actual == reference, (sid, limit, persistence, hysteresis, actual, reference)
                alerts = [t for t, event in actual if event == 'warm']
                clears = [t for t, event in actual if event == 'clear']
                writer.writerow(dict(zip(fields, [sid, limit, persistence, str(hysteresis), json.dumps(reference), json.dumps(actual), alerts[0] if alerts else '', clears[0] if clears else '', len(alerts), 'PASS', digest, simulator_digest])))
                count += 1
    print(f'Validated 12 cases and {sum(map(len, cases.values()))} minute slots; {count} policy/case comparisons passed.')
    print(OUT/'temperature-policy-results.csv')


if __name__ == '__main__':
    main()
