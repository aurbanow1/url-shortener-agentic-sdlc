"""Read-only reconciliation of the assigned candidate, fresh gate and saved QA observations."""
import csv
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
WT = ROOT / '.worktrees/04-audit-columns'
QA = ROOT / 'missions/02-brownfield/slices/04-audit-columns/proof'
OUT = Path(__file__).parent
X = '305f8045d45b19a9e3287d5fe3508af6e04db9a4'
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def load(p): return json.loads(p.read_text())
def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT)
def instant(s):
    s = re.sub(r'([+-]\d\d)$', r'\1:00', s)
    s = re.sub(r'\.(\d+)(?=[+-])', lambda m: '.'+m[1].ljust(6,'0'), s)
    return datetime.fromisoformat(s)
assert git('-C', str(WT), 'rev-parse', 'HEAD').decode().strip() == X
assert not git('-C', str(WT), 'status', '--porcelain')
result = {'candidate': X, 'base': git('rev-parse', '2566c38').decode().strip()}
files = git('diff', '--name-only', 'main...'+X).decode().splitlines()
assert len(files) == 10
result['changedFiles'] = {p: digest(WT/p) for p in files}
reports = load(QA/'qa-report-hashes.json')
for r in reports: assert digest(ROOT/r['path']) == r['sha256'], r['path']
assert len(reports) == 366
result['savedReportHashesMatched'] = len(reports)
result['freshSuites'] = {}
junit = load(QA/'qa-junit-summary.json')
for label, suite, expected in [('unit','test',218), ('functional','functionalTest',233)]:
    roots = [ET.parse(p).getroot() for p in (WT/'build/test-results'/suite).glob('TEST-*.xml')]
    counts = {k:sum(int(r.get(k,0)) for r in roots) for k in ['tests','failures','errors','skipped']}
    assert counts == {'tests':expected,'failures':0,'errors':0,'skipped':0} == junit[label]['counts']
    saved = [ET.parse(ROOT/r['xml']).getroot() for r in junit[label]['classes']]
    assert {k:sum(int(r.get(k,0)) for r in saved) for k in counts} == counts
    result['freshSuites'][label] = counts
result['freshCoverage'] = {}
for label, folder in [('unit','test'), ('functional','functionalTest'), ('all','all')]:
    path = WT/'build/reports/jacoco'/folder/'jacocoTestReport.csv'
    rows = list(csv.DictReader(path.open()))
    counts = {k:sum(int(r[k]) for r in rows) for k in ['LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED']}
    saved = load(QA/'qa-coverage-csv.json')[label]
    assert counts == {kind+'_'+state.upper():saved[kind][state.lower()] for kind in ['LINE','BRANCH'] for state in ['missed','covered']}
    result['freshCoverage'][label] = {'counts':counts, 'csvSha256':digest(path)}
assert sorted(p.name for p in (WT/'build/jacoco').glob('*.exec')) == ['functionalTest.exec','test.exec']
result['coverageInputs'] = ['functionalTest.exec','test.exec']
baseline = load(QA/'qa-baseline-file-hashes.json')
for r in baseline:
    assert hashlib.sha256(git('show','2566c38:'+r['file'])).hexdigest() == r['baselineSha256']
    assert digest(WT/r['file']) == r['candidateSha256']
assert len(baseline) == 59 and sum(r['unchanged'] for r in baseline) == 57
result['baselineFiles'] = {'total':59,'unchanged':57,'grantedV3Pins':[r['file'] for r in baseline if not r['unchanged']]}
trace = (ROOT/'docs/qa/TRACEABILITY.md').read_text().split('## 04-audit-columns — QA2, candidate ')[1].split('\n## ')[0]
oldtrace = git('show','fdd8c5b:docs/qa/TRACEABILITY.md').decode().split('## 04-audit-columns — QA2, candidate ')[1].split('\n## ')[0]
assert trace.rstrip() == oldtrace.rstrip()
methods = load(QA/'qa-test-methods.json')
for r in methods:
    assert re.search(r'\b'+re.escape(r['method'])+r'\s*\(', (WT/r['source']).read_text()), r
    assert r['class'].removeprefix('dev.urlshort.')+'#'+r['method'] in trace, r
    xml = ET.parse(WT/r['report']).getroot()
    names = [c.get('name') for c in xml.findall('testcase')]
    assert all(n in names for n in r['matchingInvocations'])
    assert int(xml.get('tests')) == r['classInvocations'] and r['result'] == 'PASS'
assert len({(r['class'],r['method']) for r in methods}) == 271
for ac in range(1,12): assert re.search(r'AC-'+str(ac)+r'\b',trace)
for br in range(1,8): assert re.search(r'BR-'+str(br)+r'\b',trace)
result['methodMappings'] = {'total':271,'individualXmlNames':sum(bool(r['matchingInvocations']) for r in methods),'classOnlyParameterized':sum(not r['matchingInvocations'] for r in methods),'traceSectionUnchanged':True}
sql = WT/'src/main/resources/db/migration/V4__add_link_audit_columns.sql'
assert digest(sql) == digest(QA/'qa-candidate-migration.sql') == digest(QA.parent/'design-probe/migration'/sql.name)
result['migrationSha256'] = digest(sql)
effects = QA/'qa-305f804/run-20261003T234116500899Z'
summary = load(effects/'summary.json')
checks = load(effects/'assertions.json')
assert summary['candidate'] == X and len(checks) == summary['passed'] == summary['assertions'] == 787
assert all(r['pass'] for r in checks)
events = {}
for p in effects.glob('*.jsonl'):
    events[p.stem] = [json.loads(line) for line in p.read_text().splitlines() if line.startswith('{')]
requests = [load(p) for p in sorted(effects.glob('http-*.json'))]
assert len(requests) == summary['httpRequests'] == 94
for r in requests:
    rid = r['headers']['x-request-id']
    matches = [e for e in events[r['app']] if e.get('requestId') == rid and e.get('message') == 'request completed']
    assert len(matches) == 1 and matches[0]['status'] == r['status'], r['name']
    if r['json'] is not None: assert json.loads(r['body']) == r['json']
    for key in ['updatedAt','createdBy','updatedBy','updated_at','created_by','updated_by']:
        assert '"'+key+'"' not in r['body'], (r['name'],key)
    if r['status'] >= 400:
        assert r['headers']['content-type'].startswith('application/problem+json')
        assert r['json']['status'] == r['status']
        assert not any(token in r['body'] for token in ['Exception','org.h2','UPDATE link','INSERT INTO','stackTrace'])
for token in ['QA04-USERAGENT-CANARY','QA04-IDEMPOTENCY-CANARY','192.0.2.10','https://example.com/qa04-A']:
    assert token not in json.dumps(events), token
created, retired, release, failure = [load(effects/n) for n in ['AC2-created-row.json','AC3-retired-row.json','AC4-key-release.json','AC5-real-jdbc-rollback.json']]
assert created['created_at'] == created['updated_at']
assert retired['updated_at'] == retired['retired_at'] and retired['created_at'] == created['created_at']
assert release['A']['updated_at'] == release['B']['created_at'] == release['B']['updated_at']
assert release['A']['idempotency_key'] is None
assert failure['before'] == failure['after'] and failure['auditBefore'] == failure['auditAfter']
assert all(r['created_by'] == r['updated_by'] == 'anonymous' for r in [created,retired,release['A'],release['B'],failure['after']])
for r in failure['auditAfter']:
    assert r['created_at'] == r['updated_at'] and r['created_by'] == r['updated_by'] == r['actor'] == 'anonymous'
    assert instant(r['created_at']) != instant(r['occurred_at'])
before, after, full, rollback, reapplied = [load(effects/n)['results'] for n in ['upgrade-before-f6.json','upgrade-after-candidate.json','before-rollback-copy.json','rollback-after.json','rollback-reapplied.json']]
oldcols = {(r['table_name'],r['column_name']) for r in before[0]}
assert [r for r in after[0] if (r['table_name'],r['column_name']) in oldcols] == before[0]
assert len(after[0]) == len(before[0])+7 == 22 and before[1:4] == after[1:4]
newcols = [r for r in after[0] if (r['table_name'],r['column_name']) not in oldcols]
assert all(r['is_nullable'] == 'NO' and r['column_default'] is not None for r in newcols)
for r in after[5]:
    assert r['updated_at'] == (r['retired_at'] or r['created_at']) and r['created_by'] == r['updated_by'] == 'anonymous'
for r in after[6]:
    assert r['created_at'] == r['updated_at'] and r['created_by'] == r['updated_by'] == r['actor']
    assert instant(r['created_at']) <= instant(r['occurred_at'])
assert rollback[0:2] == before[0:2] and rollback[2:4] == full[2:4]
assert [r['version'] for r in rollback[4]] == ['1','2','3']
assert reapplied[0:4] == full[0:4]
assert [r['version'] for r in reapplied[4]] == ['1','2','3','4']
literal = ' '.join(line.removeprefix('--').strip() for line in sql.read_text().split('-- rollback')[1].split('\n\n')[0].splitlines()[1:])
assert [s.strip() for s in literal.split(';') if s.strip()] == load(effects/'rollback-literal-header.json')['sql']
for p, expected in load(effects/'jar-hashes.json').items(): assert digest(Path(p)) == expected
assert all(a['exit'] in [0,143] for a in summary['apps']) and len(summary['apps']) == 7
result['savedEffectsReconciled'] = {'passingAssertions':787,'responseLogJoins':94,'createRetireReleaseStamps':True,'actualJdbcFailureRollback':True,'oldMetadataConstraintsValuesPreserved':True,'literalRollbackStatements':8,'rollbackReapplyObserved':True,'jarHashesMatched':2,'appsRecordedStopped':7}
state = load(OUT/'live-proof-305f804.json')
result['proofStates'] = {str(i['index']):i['state'] for i in state['items']}
result['receiptHashMismatches'] = []
for i in state['items']:
    if i['judgment']:
        assert i['judgment']['subject']['ref'] == X
        for e in i['judgment']['evidence']:
            if digest(ROOT/e['ref']) != e['sha256']: result['receiptHashMismatches'].append(e['ref'])
assert result['proofStates'] == {str(i):('pending' if i==5 else 'accepted') for i in range(1,10)}
assert not result['receiptHashMismatches']
result['scope'] = 'Fresh canonical suites executed by reviewer; saved QA effects audited, not a new execution of external driver. Item 5 remains post-merge, per assigned packet and lead decision.'
(OUT/'evidence-audit-305f804.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
