"""Reconcile this review's actual gate, captures and immutable merge custody. Run at repo root."""
import csv
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path('docs/review/03-ambiguous-analytics/proof')

def git(*args):
    return subprocess.check_output(['git', *args])

def sha(data):
    return hashlib.sha256(data).hexdigest()

def blob(commit, path):
    return git('show', f'{commit}:{path}')

target = git('rev-parse', '94aa2c0').decode().strip()
candidate = git('rev-parse', 'ec466da').decode().strip()
xprime = git('rev-parse', '22fc8e2').decode().strip()
merge = git('rev-parse', 'c9b66dd').decode().strip()
assert git('rev-parse', 'c9b66dd^2').decode().strip() == xprime
paths = git('diff', '--name-only', 'd55a502', target).decode().splitlines()
assert len(paths) == 45
product = [p for p in paths if p.startswith('src/') or p == 'docs/api/openapi.json']
assert len(product) == 18
assert all(blob(candidate, p) == blob(xprime, p) == blob(target, p) == Path(p).read_bytes() for p in product)
build_paths = ['src', 'build.gradle.kts', 'settings.gradle.kts', 'gradle', 'scripts', 'docs/api/openapi.json', 'Dockerfile', 'compose.yaml']
assert not git('diff', target, '18db1de', '--', *build_paths)
assert not git('diff', target, '--', *build_paths)
range_diff = git('range-diff', '2566c38..ec466da', 'd55a502..22fc8e2').decode()
assert len(range_diff.splitlines()) == 8 and all(' = ' in line for line in range_diff.splitlines())
(OUT / 'wave-range-diff.txt').write_text(range_diff)
(OUT / 'wave-range-log.txt').write_bytes(git('log', '--oneline', 'd55a502..94aa2c0'))

suites = {}
xml_hashes = {}
for suite in ['test', 'functionalTest']:
    files = sorted(Path('build/test-results', suite).glob('TEST-*.xml'))
    roots = [ET.parse(p).getroot() for p in files]
    suites[suite] = {k: sum(int(r.get(k, 0)) for r in roots) for k in ['tests', 'failures', 'errors', 'skipped']}
    xml_hashes.update({str(p): sha(p.read_bytes()) for p in files})
    assert all(suites[suite][k] == 0 for k in ['failures', 'errors', 'skipped'])
assert suites['test']['tests'] == 226 and suites['functionalTest']['tests'] == 250
coverage = {}
for suite in ['test', 'functionalTest', 'all']:
    rows = list(csv.DictReader(Path('build/reports/jacoco', suite, 'jacocoTestReport.csv').open()))
    coverage[suite] = {k: sum(int(r[k]) for r in rows) for k in ['LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED']}
assert coverage['all'] == dict(LINE_MISSED=0, LINE_COVERED=582, BRANCH_MISSED=0, BRANCH_COVERED=206)
for name in ['wave-check-94aa2c0.txt', 'wave-canonical-coverage-94aa2c0.txt']:
    assert 'BUILD SUCCESSFUL' in (OUT / name).read_text()
exec_files = {str(p): {'sha256': sha(p.read_bytes()), 'mtime': p.stat().st_mtime} for p in Path('build/jacoco').glob('*.exec')}

old_api = json.loads(blob('d55a502', 'docs/api/openapi.json'))
new_api = json.loads(blob(target, 'docs/api/openapi.json'))
assert json.loads(Path('build/openapi/openapi.json').read_text()) == new_api
def differences(a, b, path=''):
    if isinstance(a, dict) and isinstance(b, dict):
        result = []
        for key in sorted(set(a) | set(b)):
            p = path + '/' + key
            result += [p] if key not in a or key not in b else differences(a[key], b[key], p)
        return result
    return [] if a == b else [path]
api_changes = differences(old_api, new_api)
assert len(api_changes) == 6
assert set(api_changes) == {
    '/components/schemas/DayClicks/properties/uniqueVisitors',
    '/components/schemas/DayClicks/properties/botClicks',
    '/paths//api/links/{code}/stats/get/responses/200/description',
    '/paths//api/links/{code}/stats/get/responses/200/content/application/json/examples/stats/value/clicksPerDay',
    '/paths//api/links/{code}/stats/get/responses/200/content/application/json/examples/stats/value/code',
    '/paths//api/links/{code}/stats/get/responses/200/content/application/json/examples/stats/value/topReferrers',
}

effects = json.loads((OUT / 'wave-effects-94aa2c0.json').read_text())
log_text = (OUT / 'wave-probe-94aa2c0.txt').read_text()
events = [json.loads(line) for line in log_text.splitlines() if line.startswith('{')]
requests = [e for e in events if e['message'] == 'request completed']
assert len(effects['checks']) == 136 and len(effects['observations']) == len(requests) == 42
joins = []
for observation in effects['observations']:
    rid = observation['headers']['x-request-id']
    matching = [e for e in requests if e['requestId'] == rid]
    assert len(matching) == 1 and matching[0]['status'] == observation['status']
    header, body = observation['raw'].split('\r\n\r\n', 1)
    assert int(header.split()[1]) == observation['status'] and body == observation['body']
    assert f'x-request-id: {rid}' in header.lower()
    joins.append({'method': observation['method'], 'path': observation['path'], 'status': observation['status'], 'requestId': rid})
lost = [e for e in events if e['message'] == 'click lost']
purges = [e for e in events if e['message'] == 'clicks purged']
assert len(lost) == 2 and all(e['reason'] == 'write failed' and e['log']['level'] == 'WARN' for e in lost)
assert all(any(j['requestId'] == e['requestId'] and j['status'] == 302 for j in joins) for e in lost)
assert len(purges) == 2 and all(e['deleted'] == 5 and e['retentionDays'] == 90 and 'requestId' not in e for e in purges)
canaries = ['203.0.113.', '198.51.100.', 'wave-PRIVATE', 'wave-crawler-PRIVATE', 'private-path', 'private-query', 'https://news.example']
assert all(c not in log_text for c in canaries)
assert sum(e['message'] == 'Graceful shutdown complete' for e in events) == 2
assert 'WAVE_CONTROLS_PASS checks=136 requests=42' in log_text

proof = json.loads((OUT / 'live-wave-proof-94aa2c0.json').read_text())
assert not proof['issues']
assert [i['state'] for i in proof['items']] == ['accepted'] * 11 + ['pending']
receipt_refs = {}
for item in proof['items']:
    for evidence in (item.get('judgment') or {}).get('evidence', []):
        actual = sha(Path(evidence['ref']).read_bytes())
        receipt_refs[evidence['ref']] = {'expected': evidence['sha256'], 'actual': actual, 'equal': actual == evidence['sha256']}
assert all(e['equal'] for e in receipt_refs.values())

result = {
    'target': target, 'base': git('rev-parse', 'd55a502').decode().strip(), 'candidate': candidate,
    'xprime': xprime, 'product_merge': merge, 'gate_head': git('rev-parse', '18db1de').decode().strip(),
    'product_tree_equal_to_gate_head_and_current': True, 'all_eight_patches_identical': True,
    'range_files': {p: sha(blob(target, p)) for p in paths}, 'all_18_product_paths_equal_candidate_xprime_target_working': True,
    'suites': suites, 'xml_hashes': xml_hashes, 'coverage': coverage, 'exec_files': exec_files,
    'coverage_scope': 'Recomputed with canonical test.exec and functionalTest.exec only; old designDogfoodTestProbe.exec excluded by review init.',
    'openapi_changes': api_changes, 'live_openapi_equal': True,
    'probe_checks': len(effects['checks']), 'wire_log_joins': joins, 'write_failure_events': lost, 'purge_events': purges,
    'canaries_absent_from_product_log': canaries, 'two_owned_contexts_closed': True,
    'proof_states': {i['index']: i['state'] for i in proof['items']}, 'receipt_evidence': receipt_refs,
}
(OUT / 'wave-evidence-94aa2c0.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps({k: result[k] for k in ['target', 'suites', 'coverage', 'probe_checks', 'proof_states']}, indent=2))
print(f'PASS: 45 range files, 18 equal product paths, 42 wire/log joins, {len(receipt_refs)} current receipt hashes')
