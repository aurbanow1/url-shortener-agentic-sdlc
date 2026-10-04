"""Read-only audit of the focused CR-01 return; writes its result in this directory."""
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
WT = ROOT / '.worktrees/06-client-identity'
QA = ROOT / 'docs/qa/06-client-identity'
NEW = QA / 'recheck-e40b095'
COV = ROOT / 'docs/qa/coverage/06-client-identity'
CAND = 'e40b09541feb0b7555c475baa82587fdd09e4890'
OLD = 'fb63a88a9b92c1fec97ba74686af1a2f30304160'

def read(p):
    return json.loads(p.read_text())

def git(*args):
    return subprocess.check_output(['git', '-C', str(WT), *args], text=True).strip()

def lines(p):
    return [json.loads(s) for s in p.read_text().splitlines() if s.startswith('{')]

assert git('rev-parse', 'HEAD') == CAND and not git('status', '--porcelain')
changed = git('diff', '--name-only', OLD, CAND).splitlines()
assert changed == ['src/functionalTest/java/dev/urlshort/web/ClientIdentityCharacterizationJourneyTest.java']
for path in ['src/main', 'src/test', 'build.gradle.kts', 'docs/api/openapi.json']:
    assert git('rev-parse', CAND + ':' + path) == git('rev-parse', OLD + ':' + path)
jar_hash = hashlib.sha256((WT / 'build/libs/urlshort.jar').read_bytes()).hexdigest()
assert jar_hash == read(NEW / 'jar-custody.json')['jarSha256'] == '92e1b7aef3b91749facdc39bfbc35cc8df8367119294d801436a7db34b38e58f'
result = {'candidate': CAND, 'previousCandidate': OLD, 'changedFiles': changed,
          'productionUnitBuildAndApiUnchanged': True, 'actualJarSha256': jar_hash}
result['hashes'] = {}
for label, root, manifest in [('current', ROOT, NEW / 'artifact-hashes-final.json'),
                              ('coverage', COV, NEW / 'coverage-hashes.json')]:
    hashes = read(manifest)
    for p, expected in hashes.items():
        assert hashlib.sha256((root / p).read_bytes()).hexdigest() == expected, p
    result['hashes'][label] = len(hashes)
aliases = read(NEW / 'historic-custody-aliases.json')['archiveAliases']
historic = read(QA / 'artifact-hashes-final.json')
for p, expected in historic.items():
    assert hashlib.sha256((ROOT / aliases.get(p, p)).read_bytes()).hexdigest() == expected, p
result['hashes']['historicalWithArchiveAliases'] = len(historic)
result['hashes']['aliases'] = len(aliases)
for row in read(QA / 'source-custody.json')['unchangedOriginalFunctionalFiles']:
    assert git('rev-parse', CAND + ':' + row['path']) == row['baselineBlob']
result['freshTests'] = {}
for label, directory in [('unit', WT / 'build/test-results/test'),
                         ('functional', WT / 'build/test-results/functionalTest'),
                         ('qaCurrentCharacterizationOnOriginal', NEW / 'current-characterization-on-original/xml')]:
    total = dict.fromkeys(['tests', 'failures', 'errors', 'skipped'], 0)
    for p in directory.glob('TEST-*.xml'):
        x = ET.parse(p).getroot()
        for k in total:
            total[k] += int(x.get(k, 0))
    assert total['tests'] and not sum(total[k] for k in ['failures', 'errors', 'skipped'])
    result['freshTests'][label] = total
result['freshCoverage'] = {}
for suite in ['test', 'functionalTest', 'all']:
    p = WT / 'build/reports/jacoco' / suite / 'jacocoTestReport.csv'
    rows = list(csv.DictReader(p.open()))
    result['freshCoverage'][suite] = {k: sum(int(r[k]) for r in rows) for k in ['LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED']}
    (OUT / ('code-coverage-e40b095-' + suite + '.csv')).write_bytes(p.read_bytes())
assert result['freshCoverage']['all'] == {'LINE_MISSED': 0, 'LINE_COVERED': 584, 'BRANCH_MISSED': 0, 'BRANCH_COVERED': 206}
assert sorted(p.name for p in (WT / 'build/jacoco').glob('*.exec')) == ['functionalTest.exec', 'test.exec']
inventory = read(NEW / 'test-inventory.json')
for r in inventory['methods']:
    assert re.search(r'\b' + re.escape(r['method']) + r'\s*\(', (WT / r['source']).read_text())
    x = ET.parse(ROOT / r['report']).getroot()
    assert not sum(int(x.get(k, 0)) for k in ['failures', 'errors', 'skipped'])
result['traceabilityMethodMappings'] = len(inventory['methods'])
assert (NEW / 'traceability-append.md').read_text() in (ROOT / 'docs/qa/TRACEABILITY.md').read_text()
assert CAND in (ROOT / 'docs/qa/GAPS.md').read_text()
run = ROOT / (NEW / 'matrix-last-run.txt').read_text().strip()
requests = lines(run / 'requests.jsonl')
joined = {(r['app'], r['name']): r['events'] for r in read(run / 'request-log-joins.json')}
logs = {app: lines(run / (app + '.jsonl')) for app in {r['app'] for r in requests}}
values = {}
for row in requests:
    scope = row['app'].removeprefix('candidate-') + '/' + row['name']
    values[row['headers']['x-request-id']] = '<request:' + scope + '>'
    if row['status'] == 201:
        values.setdefault(row['json']['code'], '<code:' + scope + '>')
assert values == read(NEW / 'matrix-substitutions.json')

def sub(value):
    if isinstance(value, str):
        for old, new in values.items():
            value = value.replace(old, new)
        return value
    if isinstance(value, list):
        return [sub(x) for x in value]
    if isinstance(value, dict):
        return {k: sub(v) for k, v in value.items()}
    return value

normalized = {}
prior = read(QA / 'effects/run-20261004T021042100465Z/normalized-candidate.json')
for row in requests:
    header, body = (run / row['raw']).read_bytes().split(b'\r\n\r\n', 1)
    headers = header.decode().split('\r\n')
    assert int(headers[0].split()[1]) == row['status']
    assert {h.split(':', 1)[0].lower(): h.split(':', 1)[1].strip() for h in headers[1:]} == row['headers']
    assert body.decode() == row['body']
    assert (json.loads(body) if body and 'json' in row['headers'].get('content-type', '') else None) == row['json']
    events = [e for e in logs[row['app']] if e.get('requestId') == row['headers']['x-request-id']]
    assert events == joined[row['app'], row['name']]
    scope = row['app'].removeprefix('candidate-') + '/' + row['name']
    events = sub(events)
    for i, e in enumerate(events):
        e['@timestamp'] = '<log-time:' + str(i) + '>'
        e['process']['pid'] = '<pid:' + row['app'].removeprefix('candidate-') + '>'
    headers = sub(row['headers'])
    headers['date'] = '<HTTP-Date>'
    normalized[scope] = {'method': row['method'], 'path': sub(row['path']), 'status': row['status'],
                         'headers': headers, 'json': sub(row['json']),
                         'body': None if row['json'] is not None else sub(row['body']), 'events': events}
    assert normalized[scope] == prior[scope]
assert normalized == read(NEW / 'matrix-normalized.json')
assert len(requests) == 159
result['recomputedExactRawResponseLogPairs'] = len(requests)
result['qaEffectAssertions'] = len(lines(run / 'assertions.jsonl'))
assert result['qaEffectAssertions'] == 818 and all(r['pass'] for r in lines(run / 'assertions.jsonl'))
probe = (OUT / 'polling-resolution-e40b095.txt').read_text()
matches = re.findall(r'RESOLUTION trust=(\w+) elapsed_ms=(\d+) helper_reads=(\d+) stats_requests=(\d+) stats_429=(\d+) helper_failure=(\w+)', probe)
assert len(matches) == 2 and {r[0] for r in matches} == {'false', 'true'}
assert all(2800 <= int(r[1]) < 10000 and r[2:] == ('1', '2', '0', 'none') for r in matches)
assert 'BUILD SUCCESSFUL' in probe and 'BUILD FAILED' not in probe
result['independentResolutionCases'] = [{'trusted': r[0], 'elapsedMs': int(r[1]), 'helperReads': 1, 'refusals': 0, 'fullOracle': 'pass'} for r in matches]
(OUT / 'code-verification-e40b095.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
