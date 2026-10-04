"""Read-only reconciliation of this review's candidate and saved QA evidence.

Run from the repository root. Writes only this review's verification JSON and
copies its fresh coverage CSVs. It neither runs QA's drivers nor edits tests.
"""
import csv
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
WT = ROOT / '.worktrees/06-client-identity'
OUT = Path(__file__).resolve().parent
QA = ROOT / 'docs/qa/06-client-identity'
CAND = 'fb63a88a9b92c1fec97ba74686af1a2f30304160'
BASE = '50ad9c3ab9e65baa4100ede1772b514322957fa5'
result = {'candidate': CAND, 'base': BASE}

def read(path):
    return json.loads(path.read_text())

def git(*args):
    return subprocess.check_output(['git', '-C', str(WT), *args], text=True).strip()

def jsonlines(path):
    return [json.loads(line) for line in path.read_text().splitlines() if line.startswith('{')]

assert git('rev-parse', 'HEAD') == CAND
assert not git('status', '--porcelain')
result['changedFiles'] = git('diff', '--name-only', BASE, CAND).splitlines()
assert len(result['changedFiles']) == 10
custody = read(QA / 'source-custody.json')
for row in custody['unchangedOriginalFunctionalFiles']:
    assert git('rev-parse', BASE + ':' + row['path']) == row['baselineBlob']
    assert git('rev-parse', CAND + ':' + row['path']) == row['candidateBlob'] == row['baselineBlob']
assert git('rev-parse', BASE + ':src/main') == git('rev-parse', custody['characterization'] + ':src/main')
assert not git('diff', '--name-only', custody['characterization'], '7e232599c11f9e23b412198ae8c669f5ae467ae5', '--', 'src/test', 'src/functionalTest')
result['unchangedOriginalFunctionalFiles'] = len(custody['unchangedOriginalFunctionalFiles'])
result['characterizationPrecedesMoveWithOriginalProduction'] = True
result['hashChecks'] = {}
for label, manifest, root in [('qaArtifacts', QA / 'artifact-hashes-final.json', ROOT),
                              ('coverage', QA / 'gate-final/coverage-hashes.json', ROOT / 'docs/qa/coverage/06-client-identity')]:
    hashes = read(manifest)
    mismatches = [p for p, h in hashes.items() if not (root / p).exists() or hashlib.sha256((root / p).read_bytes()).hexdigest() != h]
    result['hashChecks'][label] = {'files': len(hashes), 'mismatches': mismatches}
    assert not mismatches, (label, mismatches)

result['freshTests'] = {}
for suite in ['test', 'functionalTest']:
    totals = dict.fromkeys(['tests', 'failures', 'errors', 'skipped'], 0)
    for path in (WT / 'build/test-results' / suite).glob('TEST-*.xml'):
        xml = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(xml.get(key, 0))
    assert totals['tests'] and not sum(totals[k] for k in ['failures', 'errors', 'skipped'])
    result['freshTests'][suite] = totals
result['freshCoverage'] = {}
for suite in ['test', 'functionalTest', 'all']:
    path = WT / 'build/reports/jacoco' / suite / 'jacocoTestReport.csv'
    rows = list(csv.DictReader(path.open()))
    result['freshCoverage'][suite] = {key: sum(int(r[key]) for r in rows) for key in ['LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED']}
    shutil.copyfile(path, OUT / ('code-coverage-' + suite + '.csv'))
result['executionData'] = sorted(p.name for p in (WT / 'build/jacoco').glob('*.exec'))
assert result['executionData'] == ['functionalTest.exec', 'test.exec']
assert result['freshCoverage']['all'] == {'LINE_MISSED': 0, 'LINE_COVERED': 584, 'BRANCH_MISSED': 0, 'BRANCH_COVERED': 206}
inventory = read(QA / 'test-inventory.json')['methods']
unique_declarations = set()
for row in inventory:
    source = (WT / row['source']).read_text()
    assert re.search(r'\b' + re.escape(row['method']) + r'\s*\(', source), row
    report = ET.parse(ROOT / row['report']).getroot()
    assert int(report.get('tests')) and not sum(int(report.get(k, 0)) for k in ['failures', 'errors', 'skipped'])
    unique_declarations.add((row['source'], row['method']))
result['inventory'] = {'mappings': len(inventory), 'uniqueSourceMethods': len(unique_declarations),
                       'reports': len({r['report'] for r in inventory}),
                       'classOnly': sum('class parameterized' in r['attribution'] for r in inventory)}
assert result['inventory'] == {'mappings': 326, 'uniqueSourceMethods': 321, 'reports': 74, 'classOnly': 56}
trace = (QA / 'traceability-append.md').read_text()
assert trace.strip() in (ROOT / 'docs/qa/TRACEABILITY.md').read_text()
assert (QA / 'gaps-append.md').read_text().strip() in (ROOT / 'docs/qa/GAPS.md').read_text()
assert set(re.findall(r'^\| AC-(\d+) \|', trace, re.M)) == {str(i) for i in range(1, 16)}
result['traceabilityACs'] = 15

def verify_http(directory, records, joined):
    logs = {app: jsonlines(directory / (app + '.jsonl')) for app in {r['app'] for r in records}}
    for row in records:
        raw = (directory / row['raw']).read_bytes()
        header, body = raw.split(b'\r\n\r\n', 1)
        lines = header.decode().split('\r\n')
        assert int(lines[0].split()[1]) == row['status'], row['name']
        headers = {line.split(':', 1)[0].lower(): line.split(':', 1)[1].strip() for line in lines[1:]}
        assert headers == row['headers'], row['name']
        value = json.loads(body) if body and 'json' in headers.get('content-type', '') else None
        assert value == row['json'], row['name']
        if 'body' in row:
            assert body.decode() == row['body']
        rid = headers['x-request-id']
        assert str(uuid.UUID(rid)) == rid
        events = [e for e in logs[row['app']] if e.get('requestId') == rid]
        assert events == joined[(row['app'], row['name'])], row['name']
        assert sum(e.get('message') == 'request completed' and e.get('status') == row['status'] for e in events) == 1
        if row['status'] >= 400:
            assert headers['content-type'].startswith('application/problem+json')
            if row['method'] != 'HEAD':
                assert value['status'] == row['status'] and value['instance'] == 'urn:uuid:' + rid
        if row['status'] == 302:
            assert headers['cache-control'] == 'no-store' and 'location' in headers and not body
    return logs

RUN = QA / 'effects/run-20261004T021042100465Z'
records = jsonlines(RUN / 'requests.jsonl')
joined = {(r['app'], r['name']): r['events'] for r in read(RUN / 'request-log-joins.json')}
logs = verify_http(RUN, records, joined)
result['controlledRawResponsesAndLogJoins'] = len(records)
substitutions = read(RUN / 'substitutions.json')
normalized = {}

def substitute(value, mapping):
    if isinstance(value, str):
        for old, new in mapping.items():
            value = value.replace(old, new)
        return value
    if isinstance(value, list):
        return [substitute(v, mapping) for v in value]
    if isinstance(value, dict):
        return {k: substitute(v, mapping) for k, v in value.items()}
    return value

for lane in ['baseline', 'candidate']:
    selected = [r for r in records if r['lane'] == lane and r['app'] != 'baseline-seed']
    values = {}
    for row in selected:
        scope = row['app'].removeprefix(lane + '-') + '/' + row['name']
        values[row['headers']['x-request-id']] = '<request:' + scope + '>'
        if row['status'] == 201 and isinstance(row['json'], dict) and 'code' in row['json']:
            values.setdefault(row['json']['code'], '<code:' + scope + '>')
    assert values == substitutions[lane]
    normalized[lane] = {}
    for row in selected:
        app = row['app'].removeprefix(lane + '-')
        scope = app + '/' + row['name']
        headers = substitute(row['headers'], values)
        if 'date' in headers:
            headers['date'] = '<HTTP-Date>'
        events = substitute(joined[(row['app'], row['name'])], values)
        for i, event in enumerate(events):
            event['@timestamp'] = '<log-time:' + str(i) + '>'
            event['process']['pid'] = '<pid:' + app + '>'
        normalized[lane][scope] = {'method': row['method'], 'path': substitute(row['path'], values), 'status': row['status'],
            'headers': headers, 'json': substitute(row['json'], values),
            'body': None if row['json'] is not None else substitute(row['body'], values), 'events': events}
    assert normalized[lane] == read(RUN / ('normalized-' + lane + '.json'))
different = [k for k in normalized['baseline'] if normalized['baseline'][k] != normalized['candidate'][k]]
assert set(different) == {'clicks-0/privacy-prometheus', 'clicks-1/privacy-prometheus'}
result['controlledExactNormalizedPairs'] = len(normalized['baseline']) - len(different)
result['retainedMetricDifferences'] = different
for scope in different:
    a, b = [normalized[lane][scope] for lane in ['baseline', 'candidate']]
    assert {k: v for k, v in a.items() if k not in ['body', 'headers']} == {k: v for k, v in b.items() if k not in ['body', 'headers']}
    assert {k: v for k, v in a['headers'].items() if k != 'content-length'} == {k: v for k, v in b['headers'].items() if k != 'content-length'}
    for record in [a, b]:
        assert int(record['headers']['content-length']) == len(record['body'].encode())
    business = [sorted(s for s in x['body'].splitlines() if s.startswith('urlshort_')) for x in [a, b]]
    assert business[0] == business[1] and len(business[0]) == 8
result['metricBusinessSamplesPerPair'] = 8
canaries = ['QA06-FULL-AGENT-CANARY', 'QA06-PATH-CANARY', 'QA06-QUERY-CANARY', 'QA06-FRAGMENT-CANARY',
            'QA06-AUDIT-TARGET-CANARY', 'QA06-INBOUND-ID-CANARY', '10.9.9.9', '10.9.9.8', '203.0.113.7', '203.0.113.8', '10.0.0.5', '198.51.100.1']
for events in logs.values():
    assert not any(c in json.dumps(events) for c in canaries)
result['controlledLogPrivacyApps'] = len(logs)
result['jarAndBlankRaw'] = {}
for label in ['before', 'after', 'blank-settings/run-20261004T022839335431Z']:
    directory = QA / label
    rows = [read(p) for p in sorted(directory.glob('[0-9]*.json'))]
    source = read(directory / 'request-log-joins.json')
    by_name = {r['name']: r['events'] for r in source}
    joins = {(r['app'], r['name']): by_name[r['name']] for r in rows}
    verify_http(directory, rows, joins)
    if label.startswith('blank'):
        for row in rows:
            if '/api/audit' in row['path']:
                assert row['status'] == (403 if 'forged' in row['name'] else 200)
    result['jarAndBlankRaw'][label] = len(rows)
result['scope'] = 'Saved evidence audited; no repeat of QA external apps. Fresh reviewer gate and independently reproduced CR-01 are separate evidence.'
(OUT / 'code-verification-fb63a88.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
