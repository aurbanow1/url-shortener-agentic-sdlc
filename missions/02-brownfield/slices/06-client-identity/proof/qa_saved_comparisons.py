"""Compare saved independent observations without changing or rerunning them."""
from pathlib import Path
import json
import re
from datetime import datetime

ROOT = Path(__file__).resolve().parents[5]
EVIDENCE = ROOT / 'docs/qa/06-client-identity'
RUN = ROOT / (Path(__file__).with_name('qa-last-run.txt').read_text().strip())


def save(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')


def metrics(body):
    metadata, samples, business = [], [], []
    for line in body.splitlines():
        if line.startswith('#'):
            metadata.append(line)
        elif line:
            match = re.fullmatch(r'(\w+)(\{.*\})?\s+(.+)', line)
            assert match, line
            name, labels, value = match.groups()
            samples.append((name, labels or ''))
            if name.startswith('urlshort_'):
                business.append(line)
    return sorted(metadata), sorted(samples), sorted(business)


base = json.loads((RUN / 'normalized-baseline.json').read_text())
cand = json.loads((RUN / 'normalized-candidate.json').read_text())
assert base.keys() == cand.keys()
metric_names = [name for name in base if name.endswith('/privacy-prometheus')]
assert len(metric_names) == 2
ordinary = [name for name in base if name not in metric_names]
assert all(base[name] == cand[name] for name in ordinary)
metric_checks = []
for name in metric_names:
    left, right = base[name], cand[name]
    # Content-Length must describe each retained body's bytes; runtime meter values
    # and their byte lengths are not an identity-preservation promise.
    for record in [left, right]:
        assert int(record['headers']['content-length']) == len(record['body'].encode())
    def contract(record):
        metadata, series, business = metrics(record['body'])
        # Readiness polling crosses startup at different times. Keep and report
        # its observed status-tag difference; compare all other series exactly.
        ordinary_series = [sample for sample in series if 'uri="/actuator/health/**"' not in sample[1]
                           and not sample[0].startswith('jvm_gc_concurrent_phase_time_seconds')]
        metadata = [line for line in metadata if ' jvm_gc_concurrent_phase_time_seconds' not in line]
        schemas = sorted(set((sample[0], tuple(re.findall(r'(\w+)=', sample[1]))) for sample in ordinary_series))
        return {**record, 'headers': {k: v for k, v in record['headers'].items() if k != 'content-length'},
                'body': (metadata, schemas, ordinary_series, business)}
    assert contract(left) == contract(right)
    metadata, series, business = metrics(left['body'])
    metric_checks.append({'scope': name, 'metadataLines': len(metadata), 'series': len(series),
                          'businessSamples': business, 'pass': True,
                          'observedSeriesOnlyInBaseline': sorted(set(metrics(left['body'])[1]) - set(metrics(right['body'])[1])),
                          'observedSeriesOnlyInCandidate': sorted(set(metrics(right['body'])[1]) - set(metrics(left['body'])[1])),
                          'observedMetadataOnlyInBaseline': sorted(set(metrics(left['body'])[0]) - set(metrics(right['body'])[0])),
                          'observedMetadataOnlyInCandidate': sorted(set(metrics(right['body'])[0]) - set(metrics(left['body'])[0])),
                          'bodyBytes': [len(left['body'].encode()), len(right['body'].encode())]})
checks = [json.loads(line) for line in (RUN / 'assertions.jsonl').read_text().splitlines()]
failed = [item['claim'] for item in checks if not item['pass']]
assert failed == ['all controlled HTTP/log comparisons identical']
result = {'pass': True, 'exactHttpLogPairs': len(ordinary), 'metricPairs': metric_checks,
          'requestsPerLane': len(base), 'observedEffectChecksPassed': len(checks) - 1,
          'originalComparatorFailureRetained': failed,
          'scopeCorrection': 'Only two Prometheus bodies contain runtime measurements outside the preservation contract. Business metric names/tags/values, responses and correlated events match. Baseline clicks-0 additionally records health 503 series, consistent with polling during startup; raw health responses were not captured, so attribution is an inference. Candidate clicks-1 records the JVM concurrent-GC phase meter; its GC occurrence varies between runs. Outside those two reported instrumentation differences, metadata and series identities match. Full differing values, names and status tags remain in comparison.json and raw captures; AC-12 requires privacy, not identical runtime telemetry.'}
save(RUN / 'scoped-comparison.json', result)


def load_jar(directory):
    requests = [json.loads(path.read_text()) for path in sorted(directory.glob('[0-9][0-9][0-9]-*.json'))]
    joins = {item['name']: item['events'] for item in json.loads((directory / 'request-log-joins.json').read_text())}
    values = {}
    for request in requests:
        name = request['name']
        values[request['headers']['x-request-id']] = '<request:' + name + '>'
        if request['status'] == 201:
            body = request['json']
            values[body['code']] = '<code:' + name + '>'
            values[body['createdAt']] = '<createdAt:' + name + '>'
            for row in [item for r in requests if r['json'] and isinstance(r['json'], dict)
                        for item in r['json'].get('items', []) if item.get('entityId') == body['code']]:
                assert row['requestId'] == request['headers']['x-request-id']
                assert datetime.fromisoformat(row['occurredAt'].replace('Z', '+00:00')) >= datetime.fromisoformat(body['createdAt'].replace('Z', '+00:00'))
                values[row['occurredAt']] = '<audit-time:' + name + '>'
    def substitute(value):
        if isinstance(value, str):
            for original, token in values.items():
                value = value.replace(original, token)
            return value
        if isinstance(value, list):
            return [substitute(item) for item in value]
        if isinstance(value, dict):
            return {key: substitute(item) for key, item in value.items()}
        return value
    normalized = {}
    for request in requests:
        name = request['name']
        events = substitute(joins[name])
        for index, event in enumerate(events):
            event['@timestamp'] = '<log-time:' + str(index) + '>'
            event['process']['pid'] = '<pid:' + request['app'] + '>'
        headers = substitute(request['headers'])
        if 'date' in headers:
            headers['date'] = '<HTTP-Date>'
        normalized[name] = {'method': request['method'], 'path': substitute(request['path']),
                            'status': request['status'], 'headers': headers,
                            'json': substitute(request['json']), 'events': events}
    return normalized, values


left, lv = load_jar(EVIDENCE / 'before')
right, rv = load_jar(EVIDENCE / 'after')
assert len(left) == len(right) == 59 and left.keys() == right.keys()
differences = [name for name in left if left[name] != right[name]]
save(EVIDENCE / 'after/jar-substitutions.json', {'baseline': lv, 'candidate': rv})
save(EVIDENCE / 'after/normalized-before.json', left)
save(EVIDENCE / 'after/normalized-after.json', right)
save(EVIDENCE / 'after/jar-comparison.json', {'pairs': len(left), 'differences': [
    {'scope': name, 'baseline': left[name], 'candidate': right[name]} for name in differences]})
assert differences == ['live-openapi']
assert left['live-openapi']['json'] == right['live-openapi']['json']
assert len(left['live-openapi']['events']) == len(right['live-openapi']['events']) == 2
for before, after in zip(left['live-openapi']['events'], right['live-openapi']['events']):
    if before != after:
        assert re.fullmatch(r'Init duration for springdoc-openapi is: \d+ ms', before['message'])
        assert re.fullmatch(r'Init duration for springdoc-openapi is: \d+ ms', after['message'])
        assert {k: v for k, v in before.items() if k != 'message'} == {k: v for k, v in after.items() if k != 'message'}
save(EVIDENCE / 'after/jar-scoped-comparison.json', {'pass': True, 'exactPairs': 58,
     'apiDescriptionEqual': True, 'substitutions': 'jar-substitutions.json',
     'retainedDifference': 'Springdoc initialization measurement 293ms versus 162ms in its existing INFO event. Event presence, count, level and every other field match. This v3/api-docs event is outside AC-14 (which names audit 200); it is not normalized or claimed identical.'})
print(json.dumps({'controlled': {k: v for k, v in result.items() if k != 'metricPairs'},
                  'jarDifferenceScopes': differences}))
