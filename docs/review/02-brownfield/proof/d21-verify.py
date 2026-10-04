"""Passive reconciliation of D21's merged source, fresh gate and saved QA effects."""
from pathlib import Path
import csv
import hashlib
import json
import re
import subprocess
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
OUT = ROOT / 'docs/review/02-brownfield/proof'
WT = ROOT / '.worktrees/review-wave02-fda42757'
QA = ROOT / 'docs/qa/06-client-identity'
RECHECK = QA / 'recheck-e40b095'

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def hashes(manifest, base=ROOT, aliases=None):
    entries = json.loads(manifest.read_text())
    for name, expected in entries.items():
        path = base / (aliases or {}).get(name, name)
        assert digest(path) == expected, str(path)
    return len(entries)

def counts(directory):
    result = dict.fromkeys(['tests', 'failures', 'errors', 'skipped'], 0)
    for path in directory.glob('TEST-*.xml'):
        root = ET.parse(path).getroot()
        for key in result:
            result[key] += int(root.attrib.get(key, 0))
    assert not any(result[k] for k in ['failures', 'errors', 'skipped']), result
    return result

def http(path):
    header, body = path.read_text().split('\n\n', 1)
    lines = header.splitlines()
    headers = dict(line.split(':', 1) for line in lines[1:])
    headers = {k.lower(): v.strip() for k, v in headers.items()}
    return int(lines[0].split()[1]), headers, json.loads(body) if body else None

def matrix(directory, prefix):
    substitutions, responses, events = {}, {}, {}
    for path in sorted(directory.glob(prefix + '-matrix-*.http')):
        status, headers, body = http(path)
        label = path.stem.removeprefix(prefix + '-')
        request_id = headers['x-request-id']
        assert str(uuid.UUID(request_id)) == request_id
        substitutions[request_id] = '<request:' + label + '>'
        if status == 201:
            substitutions[body['code']] = '<code:' + label + '>'
        responses[label] = (status, headers, body)
    for path in sorted(directory.glob(prefix + '-matrix-*.jsonl')):
        if path.name.endswith('-sql.jsonl'):
            continue
        for line in path.read_text().splitlines():
            event = json.loads(line)
            rid = event.get('requestId')
            if rid:
                events.setdefault(rid, []).append(event)

    def normal(value):
        if isinstance(value, str):
            for old, new in substitutions.items():
                value = value.replace(old, new)
            return value
        if isinstance(value, dict):
            return {k: normal(v) for k, v in value.items()}
        if isinstance(value, list):
            return [normal(v) for v in value]
        return value

    result = {}
    for label, (status, headers, body) in responses.items():
        joined = events[headers['x-request-id']]
        assert len(joined) == 1, label
        assert joined[0]['message'] == 'request completed' and joined[0]['status'] == status, label
        event = normal(joined[0])
        event['@timestamp'] = '<log-time>'
        event['process']['pid'] = '<pid>'
        headers['date'] = '<http-date>'
        if label.endswith('-stats'):
            assert body['totalClicks'] == 3
            assert body['clicksPerDay'] == [{'date': '2026-10-01', 'clicks': 3, 'uniqueVisitors': 2, 'botClicks': 0}]
        if label.endswith('-shared-429'):
            assert status == 429 and headers['retry-after'] == '30'
            assert body['status'] == 429 and body['title'] == 'Too Many Requests'
            assert 'location' not in headers
        if label.endswith('-independent') and '-create-' in label:
            assert status == 201
        if label.endswith('-independent') and '-redirect-' in label:
            assert status == 302
        result[label] = normal([status, headers, body, event])
    assert len(result) == 159
    assert sum(k.endswith('-stats') for k in result) == 12
    return result

head = git('rev-parse', 'fda42757')
merge = git('rev-parse', 'b8d7fc16')
candidate = git('rev-parse', 'e40b095')
product = ['src', 'scripts', 'build.gradle.kts', 'settings.gradle.kts', 'gradle', 'gradlew', 'gradlew.bat', '.github', 'Dockerfile', 'compose.yaml', 'docs/api']
assert not git('diff', candidate, head, '--', *product)
assert not git('diff', merge, head, '--', *product)
assert not subprocess.check_output(['git', 'status', '--porcelain'], cwd=WT, text=True)
original_functional = git('ls-tree', '-r', '--name-only', '50ad9c3', '--', 'src/functionalTest').splitlines()
assert not git('diff', '50ad9c3', head, '--', *original_functional)
changed = git('diff', '--name-only', '50ad9c3', head, '--', *product).splitlines()
assert len(changed) == 10
unchanged = ['src/main/resources/db', 'docs/api', 'build.gradle.kts', '.github', 'scripts']
assert not git('diff', '50ad9c3', head, '--', *unchanged)
assert git('rev-parse', '1b4e0a7:src/main') == git('rev-parse', '50ad9c3:src/main')
aliases = json.loads((RECHECK / 'historic-custody-aliases.json').read_text())['archiveAliases']
qa_counts = {'current': hashes(RECHECK / 'artifact-hashes-final.json'),
             'coverage': hashes(RECHECK / 'coverage-hashes.json', ROOT / 'docs/qa/coverage/06-client-identity'),
             'historical': hashes(QA / 'artifact-hashes-final.json', aliases=aliases)}
gate = {suite: counts(WT / 'build/test-results' / suite) for suite in ['test', 'functionalTest']}
coverage = {}
for suite in ['test', 'functionalTest', 'all']:
    p = WT / 'build/reports/jacoco' / suite / 'jacocoTestReport.csv'
    rows = list(csv.DictReader(p.open()))
    coverage[suite] = {k: sum(int(r[k]) for r in rows) for k in ['LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED']}
    if suite == 'all':
        (OUT / 'd21-coverage-fda42757.csv').write_bytes(p.read_bytes())
assert coverage['all']['LINE_MISSED'] == coverage['all']['BRANCH_MISSED'] == 0
xml = ET.parse(WT / 'build/reports/jacoco/all/jacocoTestReport.xml').getroot()
bundle = {x.attrib['type']: {'missed': int(x.attrib['missed']), 'covered': int(x.attrib['covered'])} for x in xml.findall('counter')}
baseline = matrix(QA / 'effects/run-20261004T021042100465Z', 'baseline')
current = matrix(RECHECK / 'matrix-effects/run-20261004T030728543427Z', 'candidate')
assert baseline == current, [k for k in current if current[k] != baseline[k]]
assert (WT / 'build/openapi/openapi.json').read_bytes() == (WT / 'docs/api/openapi.json').read_bytes()
result = {'pass': True, 'merge': merge, 'reviewedHead': head, 'candidate': candidate,
          'candidateAndMergedProductIdentical': True, 'originalFunctionalFilesUnchanged': len(original_functional),
          'changedProductFiles': changed, 'qaHashes': qa_counts, 'freshGate': gate, 'coverageCsv': coverage,
          'coverageBundle': bundle, 'canonicalExecutionFiles': sorted(p.name for p in (WT/'build/jacoco').glob('*.exec')),
          'independentBaselineToCurrentRawLogComparisons': 159, 'exactGroupingRows': 12,
          'substitutions': 'Only response-derived UUID/code associations, HTTP Date, JSON event timestamp and PID. No status, field, value, event count, grouping or target substitution.',
          'generatedOpenApiByteIdentical': True}
(OUT / 'd21-reconciliation-fda42757.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
