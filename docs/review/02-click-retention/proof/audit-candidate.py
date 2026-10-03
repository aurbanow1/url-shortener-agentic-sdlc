"""Reconcile saved QA evidence and this review's fresh canonical gate; no app mutation."""
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
WT = ROOT / '.worktrees/02-click-retention'
QA = ROOT / 'missions/02-brownfield/slices/02-click-retention/proof'
OUT = Path(__file__).parent
X = 'a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1'
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def load(p): return json.loads(p.read_text())
def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT)
assert git('-C', str(WT), 'rev-parse', 'HEAD').decode().strip() == X
assert not git('-C', str(WT), 'status', '--porcelain')
result = {'candidate': X, 'changedFiles': git('diff', '--name-only', 'main...'+X).decode().splitlines()}
assert len(result['changedFiles']) == 18
manifest = load(QA / 'qa-coverage-audit-a8fc8b6.json')
for row in manifest['files']: assert digest(ROOT/row['copy']) == row['sha256'], row['copy']
result['savedReportHashesMatched'] = len(manifest['files'])
result['freshSuites'] = {}
for suite, expected in [('test',174), ('functionalTest',172)]:
    roots = [ET.parse(p).getroot() for p in (WT/'build/test-results'/suite).glob('TEST-*.xml')]
    counts = {k: sum(int(r.get(k,0)) for r in roots) for k in ['tests','failures','errors','skipped']}
    assert counts == {'tests':expected,'failures':0,'errors':0,'skipped':0}, counts
    result['freshSuites'][suite] = counts
result['freshCoverage'] = {}
for label, folder in [('unit','test'), ('functional','functionalTest'), ('all','all')]:
    path = WT/'build/reports/jacoco'/folder/'jacocoTestReport.csv'
    rows = list(csv.DictReader(path.open()))
    counts = {k:sum(int(r[k]) for r in rows) for k in ['LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED']}
    assert counts == manifest['coverage'][label], (label,counts)
    result['freshCoverage'][label] = counts
inventory = load(QA/'qa-test-inventory-a8fc8b6.json')
trace = (ROOT/'docs/qa/TRACEABILITY.md').read_text().split('## 02-click-retention — candidate ')[1].split('\n## ')[0]
for r in inventory:
    assert digest(WT/r['source']) == r['sourceSha256'], r['source']
    assert re.search(r'\b'+re.escape(r['method'])+r'\s*\(', (WT/r['source']).read_text())
    assert r['class'].removeprefix('dev.urlshort.')+'#'+r['method'] in trace
assert len({(r['class'],r['method']) for r in inventory}) == 210
for ac in range(1,17): assert re.search(r'AC-'+str(ac)+r'\b',trace)
for br in range(1,8): assert re.search(r'BR-'+str(br)+r'\b',trace)
result['candidateMethodsMappedAndHashed'] = len(inventory)
original = load(QA/'qa-original-source-audit-f6dd29e.json')
for row in original['unchangedSourceFiles']:
    assert hashlib.sha256(git('show','f6dd29e:'+row['path'])).hexdigest() == row['sha256'] == digest(WT/row['path'])
result['originalSourcesIdentical'] = len(original['unchangedSourceFiles'])
roots = [ET.parse(p).getroot() for p in (QA/'qa-original-suite-xml').glob('TEST-*.xml')]
counts = {k:sum(int(r.get(k,0)) for r in roots) for k in ['tests','failures','errors','skipped']}
assert counts == {'tests':155,'failures':0,'errors':0,'skipped':0}
result['qaOriginalSuite'] = counts
sql = WT/'src/main/resources/db/migration/V3__add_click_audit_columns.sql'
assert digest(sql) == digest(QA/'qa-migration-V3-a8fc8b6.sql') == digest(QA.parent/'design-probe/migration'/sql.name)
result['reviewedMigrationSha256'] = digest(sql)
effects = QA/'qa-effects-final-a8fc8b6'
capture = load(effects/'effects.json')
assert capture['candidate'] == X and len(capture['checks']) == 249
assert all(r['passed'] for r in capture['checks'])
events = []
for p in effects.glob('*.jsonl'):
    for line in p.read_text().splitlines():
        if line.startswith('{'): events.append(json.loads(line))
requests = [o for o in capture['observations'] if 'headers' in o]
for o in requests:
    request_id = o['headers']['x-request-id']
    assert o['raw'].splitlines()[0].split()[1] == str(o['status'])
    matches = [e for e in events if e.get('requestId') == request_id and e.get('message') == 'request completed']
    assert len(matches) == 1 and matches[0]['status'] == o['status'], request_id
assert len(requests) == 207
for token in ['QA_BROWSER_SECRET','QA_PURGE_DRIVER_SECRET_CANARY','Empty key']:
    assert not any(token in json.dumps(e) for e in events), token
purges = [e for e in events if e.get('message') in ['clicks purged','click purge failed']]
for e in purges:
    assert not set(e).intersection({'requestId','clientHash','referrer','userAgentClass','code','linkId','error'})
    assert e['log']['level'] == ('INFO' if e['message'] == 'clicks purged' else 'WARN')
    assert {'cutoff','retentionDays'}.issubset(e)
    assert ('deleted' if e['message']=='clicks purged' else 'errorType') in e
before, after = load(effects/'upgrade-before.json'), load(effects/'upgrade-after.json')
assert before[:2] == after[:2] and before[3] == after[3]
kept = [r for r in before[2] if r['clicked_on'] >= '2026-07-05']
assert len(kept) == 3
assert [r for r in after[2] if r['id'] in {r['id'] for r in kept}] == kept
assert len(after[2]) == 4
oldmeta, newmeta = load(effects/'v2-schema-detail.json'), load(effects/'v3-schema-detail.json')
newcols = {(r['table_name'],r['column_name']):r for r in newmeta[0]}
assert all(newcols[(r['table_name'],r['column_name'])] == r for r in oldmeta[0])
assert oldmeta[1:] == newmeta[1:]
assert len(newmeta[0]) - len(oldmeta[0]) == 8
result['qaEffectsAudit'] = {'assertionsRecordedPassing':249,'rawResponsesCorrelated':207,'purgeEventsChecked':len(purges),'upgradeAndSchemaRecomputed':True,'ownedProcessesRecordedStopped':len(capture['stopped'])}
state = load(OUT/'proof-state-a8fc8b6.json')
result['proofStates'] = {str(i['index']):i['state'] for i in state['items']}
result['receiptEvidenceHashMismatches'] = []
for i in state['items']:
    if i['judgment']:
        assert i['judgment']['subject']['ref'] == X
        for e in i['judgment']['evidence']:
            if digest(ROOT/e['ref']) != e['sha256']: result['receiptEvidenceHashMismatches'].append(e['ref'])
assert not result['receiptEvidenceHashMismatches']
assert result['proofStates'] == {str(i):('pending' if i==9 else 'accepted') for i in range(1,11)}
result['scope'] = 'Saved QA effects audited; not a new execution of the external driver. Fresh coverage includes test.exec and functionalTest.exec only.'
(OUT/'evidence-audit-a8fc8b6.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
