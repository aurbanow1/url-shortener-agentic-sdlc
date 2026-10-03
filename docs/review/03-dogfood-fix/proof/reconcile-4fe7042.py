"""Reviewer reconciliation. Reads QA evidence; writes only this review's summary."""
import csv
import hashlib
import json
import math
import re
import subprocess
import tarfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
WT = ROOT / '.worktrees/03-dogfood-fix'
QA = ROOT / 'missions/02-brownfield/slices/03-dogfood-fix/proof/qa-4fe7042'
SHA = '4fe70427bd0d182e886d6a19b217daa1d9e39f5d'
BASE = '15db6c5'

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)

def read(path):
    return json.loads(path.read_text())

def digest(data):
    return hashlib.sha256(data).hexdigest()

def totals(folder):
    suites = [ET.parse(p).getroot() for p in folder.glob('TEST-*.xml')]
    assert suites, folder
    return {k: sum(int(s.get(k, 0)) for s in suites) for k in ('tests', 'failures', 'errors', 'skipped')}

def exchange(name):
    lines = (QA / 'http' / (name + '.headers')).read_text().splitlines()
    headers = {k.lower(): v.strip() for line in lines if ':' in line for k, v in [line.split(':', 1)]}
    return int(lines[0].split()[1]), headers, read(QA / 'http' / (name + '.body'))

assert git('-C', str(WT), 'rev-parse', 'HEAD').decode().strip() == SHA
assert git('-C', str(WT), 'status', '--porcelain').decode().strip() == 'M gradlew.bat'
assert (WT / 'gradlew.bat').read_bytes().replace(b'\r\n', b'\n') == git('show', SHA + ':gradlew.bat').replace(b'\r\n', b'\n')
subprocess.run(['git', 'merge-base', '--is-ancestor', 'cb148c4', SHA], cwd=ROOT, check=True)
suite_counts = {}
coverage = {}
for suite, fresh, count in [('unit', 'test', 204), ('functional', 'functionalTest', 207)]:
    suite_counts[suite] = totals(WT / 'build/test-results' / fresh)
    assert suite_counts[suite] == dict(tests=count, failures=0, errors=0, skipped=0)
for suite, fresh in [('unit', 'test'), ('functional', 'functionalTest'), ('all', 'all')]:
    copied = next((ROOT / 'docs/qa/coverage/03-dogfood-fix' / suite).glob('*.csv'))
    current = WT / 'build/reports/jacoco' / fresh / 'jacocoTestReport.csv'
    with copied.open() as f:
        rows = list(csv.DictReader(f))
    with current.open() as f:
        assert rows == list(csv.DictReader(f)), suite
    coverage[suite] = {k: sum(int(r[k]) for r in rows) for k in ('LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED')}
assert coverage['all'] == dict(LINE_MISSED=0, LINE_COVERED=508, BRANCH_MISSED=0, BRANCH_COVERED=194)
hashes = read(QA / 'report-hashes.json')
for path, expected in hashes.items():
    assert digest((ROOT / path).read_bytes()) == expected, path

raw_hashes = read(QA / 'raw-capture-hashes.json')
with tarfile.open(QA / 'raw-captures.tar.gz') as archive:
    for row in raw_hashes:
        raw = archive.extractfile(row['path']).read()
        assert digest(raw) == row['sha256'], row['path']
        path = QA / row['path']
        display = raw
        if path.suffix in ('.headers', '.xml', '.txt') or path.name.endswith('-prometheus.body'):
            display = ('\n'.join(line.rstrip() for line in raw.decode().splitlines()).rstrip('\n') + '\n').encode()
        assert path.read_bytes() == display, path

old = json.loads(git('show', BASE + ':docs/api/openapi.json'))
new = json.loads(git('show', SHA + ':docs/api/openapi.json'))
assert new == read(WT / 'build/openapi/openapi.json') == exchange('candidate-openapi')[2]
assert old == exchange('base-openapi')[2]
problem = new['components']['schemas']['ProblemDetail']
field = new['components']['schemas']['ProblemFieldError']
assert set(problem['properties']) == {'detail', 'errors', 'instance', 'status', 'title', 'type'}
assert 'errors' not in problem.get('required', [])
assert problem['properties']['errors']['type'] == 'array'
assert problem['properties']['errors']['items'] == {'$ref': '#/components/schemas/ProblemFieldError'}
assert set(field['properties']) == set(field['required']) == {'field', 'rule', 'message'}
assert all(v == {'type': 'string'} for v in field['properties'].values())
for key in ('detail', 'instance', 'status', 'title', 'type'):
    assert problem['properties'][key] == old['components']['schemas']['ProblemDetail']['properties'][key]
refs = [media['schema'] for path in new['paths'].values() for op in path.values()
        for response in op['responses'].values() for typ, media in response.get('content', {}).items()
        if typ == 'application/problem+json']
assert len(refs) == 22 and all(x == {'$ref': '#/components/schemas/ProblemDetail'} for x in refs)
for doc in (old, new):
    doc['components']['schemas'].pop('ProblemDetail')
    doc['components']['schemas'].pop('ProblemFieldError', None)
assert old == new

wire = []
for name, expected, has_errors in [('bad-create', 400, True), ('mismatch', 422, True), ('unknown', 404, False), ('gone', 410, False), ('burst-89', 429, False), ('bad-audit', 400, True)]:
    pair = []
    for label in ('base', 'candidate'):
        status, headers, body = exchange(label + '-' + name)
        assert status == expected and headers['content-type'] == 'application/problem+json'
        assert body.pop('instance') == 'urn:uuid:' + headers['x-request-id']
        assert set(body) <= set(problem['properties']) and ('errors' in body) == has_errors
        if has_errors:
            assert len(body['errors']) == 1 and set(body['errors'][0]) == {'field', 'rule', 'message'}
            assert all(isinstance(v, str) for v in body['errors'][0].values())
        pair.append(body)
    assert pair[0] == pair[1], name
    wire.append({'case': name, 'status': expected, 'normalizedBody': pair[1]})

correlation = {}
cwd = read(QA / 'installed-launch-arguments.json')['workingDirectory']
for label in ('base', 'candidate'):
    prom = (QA / 'http' / (label + '-prometheus.body')).read_text()
    for metric, series in [('disk-free', 'disk_free_bytes'), ('disk-total', 'disk_total_bytes')]:
        status, _, body = exchange(label + '-' + metric)
        assert status == 200 and all(math.isfinite(v['value']) and v['value'] > 0 for v in body['measurements'])
        assert any(x.startswith(series) and math.isfinite(float(x.split()[-1])) for x in prom.splitlines() if not x.startswith('#'))
        if label == 'candidate':
            assert not body['availableTags'] and cwd not in json.dumps(body)
    assert ('path="' not in prom and cwd not in prom) if label == 'candidate' else ('path="' in prom and cwd in prom)
    selector_status = int((QA / 'http' / (label + '-disk-selector.headers')).read_text().splitlines()[0].split()[1])
    assert selector_status == (404 if label == 'candidate' else 200)
    captured = list((QA / 'http').glob(label + '-*.headers'))
    for logfile in [QA / (label + '.jsonl'), QA / (label + '-console.txt')]:
        text = logfile.read_text()
        events = [json.loads(x) for x in text.splitlines() if x.startswith('{')]
        completed = [e for e in events if e.get('message') == 'request completed']
        assert len(completed) == len(captured) == 104
        by_id = {e['requestId']: e for e in completed}
        assert len(by_id) == 104
        for path in captured:
            lines = path.read_text().splitlines()
            rid = next(s.split(':', 1)[1].strip() for s in lines if s.lower().startswith('x-request-id:'))
            assert by_id[rid]['status'] == int(lines[0].split()[1])
        assert all(v not in text for v in ['qa-dogfood-privacy-canary-4fe7042', 'https://qa-dogfood.example/canary-4fe7042', 'ftp://invalid.example/', '127.0.0.1'])
    correlation[label] = len(captured)

reds = {}
for name, folder in [('schema', 'red-openapi-results'), ('metrics', 'red-metrics-results')]:
    failures = [t for f in (QA / folder).glob('TEST-*.xml') for t in ET.parse(f).getroot().findall('testcase') if t.find('failure') is not None]
    assert len(failures) == 2
    reds[name] = [t.get('name') for t in failures]
for before, after in [('cce7cf7', 'a28a20a'), ('72dfffb', SHA), ('0d000da', 'cce7cf7'), ('0982cb5', 'cce7cf7')]:
    subprocess.run(['git', 'merge-base', '--is-ancestor', before, after], cwd=ROOT, check=True)
for path, expected in read(QA / 'shipped-source-hashes.json').items():
    assert digest(git('show', BASE + ':' + path)) == expected
for path, expected in read(QA / 'jar-provenance.json')['baseInputHashes'].items():
    assert digest(git('show', BASE + ':' + path)) == expected
for suite, count in [('unit', 203), ('functional', 202)]:
    assert totals(QA / ('shipped-' + suite + '-results')) == dict(tests=count, failures=0, errors=0, skipped=0)
trace = (ROOT / 'docs/qa/TRACEABILITY.md').read_text().split('## 03-dogfood-fix', 1)[1].split('\n## ', 1)[0]
methods = read(QA / 'source-methods.json')
assert len(methods) == 233
for row in methods:
    assert row['method'] in (ROOT / row['path']).read_text()
    assert row['cls'].replace('dev.urlshort.', '') + '#' + row['method'] in trace
assert all('AC-' + str(i) in trace for i in range(1, 10))
assert all('BR-' + str(i) in trace for i in range(1, 6))

result = dict(candidate=SHA, baseline=BASE, inheritedDiff='gradlew.bat line endings only',
              freshTests=suite_counts, coverage=coverage, qaReportHashes=len(hashes),
              retainedRawFilesVerified=len(raw_hashes), fullWireComparisons=wire,
              oneProblemSchemaReferences=len(refs), requestLogPairs=correlation,
              historicalRedTests=reds, originalTests={'unit': 203, 'functional': 202},
              sourceMethodsInTraceability=len(methods), apiDiffOnlyProblemSchemas=True,
              metrics='both gauges positive, no path; old selector 200 to 404')
(OUT / 'evidence-reconciliation-4fe7042.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
