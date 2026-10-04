"""Independent current-candidate gate/effect/custody validation."""
from pathlib import Path
import copy
import csv
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
SHA = 'e40b09541feb0b7555c475baa82587fdd09e4890'
OLD = ROOT/'docs/qa/06-client-identity'
COV = ROOT/'docs/qa/coverage/06-client-identity'
def save(name, value):
    (OUT/(name+'.json')).write_text(json.dumps(value,indent=2)+'\n')

assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip() == SHA
assert not subprocess.check_output(['git','status','--porcelain'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip()
source = json.loads((OUT/'source-custody.json').read_text())
def git_value(argument):
    return subprocess.check_output(['git','rev-parse',argument],cwd=ROOT,text=True).strip()
assert git_value(SHA+':src/main') == git_value(source['previousCandidate']+':src/main') == source['productionTree']
assert git_value(SHA+':src/test') == git_value(source['previousCandidate']+':src/test')
changed = subprocess.check_output(['git','diff','--name-only',source['previousCandidate'],SHA],cwd=ROOT,text=True).splitlines()
assert changed == source['changedFiles']
original_source = json.loads((OLD/'source-custody.json').read_text())
for item in original_source['unchangedOriginalFunctionalFiles']:
    assert git_value(SHA+':'+item['path']) == item['baselineBlob'] == item['candidateBlob']
gate = json.loads((OUT/'gate-summary.json').read_text())
fresh_invocations = 0
for dirname,expected in gate['tests'].items():
    actual = dict.fromkeys(['tests','failures','errors','skipped'],0)
    for path in (OUT/'test-results'/dirname).glob('TEST-*.xml'):
        report = ET.parse(path).getroot()
        for key in actual:
            actual[key] += int(report.get(key,'0'))
    assert actual == expected, (dirname, actual, expected)
    fresh_invocations += actual['tests']
assert fresh_invocations == 590
for suite,expected in gate['coverage'].items():
    actual = dict.fromkeys(expected,0)
    with (COV/suite/'jacocoTestReport.csv').open() as stream:
        for row in csv.DictReader(stream):
            for key in actual:
                actual[key] += int(row[key])
    assert actual == expected, (suite, actual, expected)
jar = json.loads((OUT/'jar-custody.json').read_text())
assert hashlib.sha256((ROOT/'.worktrees/06-client-identity/build/libs/urlshort.jar').read_bytes()).hexdigest() == jar['jarSha256']
assert jar['jarBytesIdentical'] and jar['jarSha256'] == '92e1b7aef3b91749facdc39bfbc35cc8df8367119294d801436a7db34b38e58f'
compatibility = dict.fromkeys(['tests','failures','errors','skipped'],0)
for path in (OUT/'current-characterization-on-original/xml').glob('TEST-*.xml'):
    report = ET.parse(path).getroot()
    for key in compatibility:
        compatibility[key] += int(report.get(key,'0'))
assert compatibility == {'tests':72,'failures':0,'errors':0,'skipped':0}
assert (OUT/'traceability-append.md').read_text() in (ROOT/'docs/qa/TRACEABILITY.md').read_text()
assert SHA in (ROOT/'docs/qa/GAPS.md').read_text()
hashes = json.loads((OUT/'coverage-hashes.json').read_text())
assert len(hashes) == 378
for relative,digest in hashes.items():
    assert hashlib.sha256((COV/relative).read_bytes()).hexdigest() == digest
historic = json.loads((OLD/'artifact-hashes-final.json').read_text())
aliases = {}
for relative,digest in historic.items():
    path = ROOT/relative
    if relative.startswith('docs/qa/coverage/06-client-identity/'):
        path = COV/'archive-fb63a88'/path.relative_to(COV)
        aliases[relative] = str(path.relative_to(ROOT))
    assert hashlib.sha256(path.read_bytes()).hexdigest() == digest, relative
save('historic-custody-aliases',{'pass':True,'originalArtifactHashesVerified':len(historic),
     'archiveAliases':aliases,'reason':'Fresh reports occupy required canonical suite directories; prior fb63a88 reports/SUMMARY preserved byte-for-byte in archive-fb63a88.'})

inventory = json.loads((OLD/'test-inventory.json').read_text())
reports = {}
for suite,dirname in [('unit','test'),('functional','functionalTest')]:
    for path in (OUT/'test-results'/dirname).glob('TEST-*.xml'):
        report = ET.parse(path).getroot()
        assert all(int(report.get(key,'0')) == 0 for key in ['failures','errors','skipped'])
        reports[suite,report.get('name')] = report
for row in inventory['methods']:
    report = reports[row['suite'],row['class']]
    named = any(case.get('name').startswith(row['method']+'(') for case in report.findall('testcase'))
    assert named or row['annotation'] in ['ParameterizedTest','RepeatedTest']
    assert row['method'] in (ROOT/'.worktrees/06-client-identity'/row['source']).read_text()
    row['report'] = row['report'].replace('gate-final/test-results','recheck-e40b095/test-results')
inventory['candidate'] = SHA
save('test-inventory',inventory)
assert set(reports) == {(row['suite'],row['class']) for row in inventory['methods']}

probe = (OUT/'held-writer-probe.txt').read_text()
matches = re.findall(r'PROBE trust=(none|P) elapsed_ms=(\d+) stats_requests=(\d+) stats_429=(\d+) helper_failure=(\w+)',probe)
assert len(matches) == 2
assert all(2500 <= int(elapsed) < 10000 and reads == '2' and limited == '0' and failure == 'none'
           for trust,elapsed,reads,limited,failure in matches)
assert 'BUILD SUCCESSFUL' in probe and 'BUILD FAILED' not in probe
save('held-writer-summary',{'pass':True,'candidate':SHA,'cases':[{'trust':trust,'elapsedMs':int(elapsed),
     'helperStatsRequests':1,'positiveControlRequests':1,'stats429':0,'helperFailure':None,
     'fullGroupingDayPrivacyOraclePassed':True} for trust,elapsed,reads,limited,failure in matches],
     'instrument':'External actual helper probe, real writer delayed3s; helper old-failure expectation replaced with resolution assertions, no product/test edit.'})

run = ROOT/(OUT/'matrix-last-run.txt').read_text().strip()
matrix = json.loads((run/'summary.json').read_text())
assert matrix['pass'] and matrix['rows'] == 12 and matrix['requests'] == 159 and matrix['assertions'] == 818 and matrix['appsStopped'] == 3
responses = [json.loads(line) for line in (run/'requests.jsonl').read_text().splitlines()]
joins = json.loads((run/'request-log-joins.json').read_text())
assert len(responses) == len(joins) == 159
logs = {(item['app'],item['name']):item['events'] for item in joins}
values = {}
for record in responses:
    scope = record['app'].removeprefix('candidate-')+'/'+record['name']
    values[record['headers']['x-request-id']] = '<request:'+scope+'>'
    if record['status'] == 201:
        values.setdefault(record['json']['code'],'<code:'+scope+'>')
def replace(value):
    if isinstance(value,str):
        for old,new in values.items():
            value = value.replace(old,new)
        return value
    if isinstance(value,list):
        return [replace(item) for item in value]
    if isinstance(value,dict):
        return {key:replace(item) for key,item in value.items()}
    return value
normalized = {}
for record in responses:
    raw = (run/record['raw']).read_bytes().decode()
    head,body = raw.split('\r\n\r\n',1)
    assert int(head.splitlines()[0].split()[1]) == record['status'] and body == record['body']
    app = record['app']
    original_events = [json.loads(line) for line in (run/(app+'.jsonl')).read_text().splitlines() if line.startswith('{')]
    assert [event for event in original_events if event.get('requestId') == record['headers']['x-request-id']] == logs[app,record['name']]
    scope = app.removeprefix('candidate-')+'/'+record['name']
    headers = replace(record['headers'])
    headers['date'] = '<HTTP-Date>'
    events = replace(logs[app,record['name']])
    for index,event in enumerate(events):
        event['@timestamp'] = '<log-time:'+str(index)+'>'
        event['process']['pid'] = '<pid:'+app.removeprefix('candidate-')+'>'
    normalized[scope] = {'method':record['method'],'path':replace(record['path']),
                         'status':record['status'],'headers':headers,'json':replace(record['json']),
                         'body':None if record['json'] is not None else replace(record['body']),'events':events}
original = json.loads((ROOT/'docs/qa/06-client-identity/effects/run-20261004T021042100465Z/normalized-candidate.json').read_text())
differences = [scope for scope,record in normalized.items() if record != original[scope]]
assert not differences,differences
save('matrix-substitutions',values)
save('matrix-normalized',normalized)
save('matrix-comparison',{'pass':True,'exactPreviousCandidatePairs':len(normalized),'differences':differences,
                         'substitutions':'generated request UUID/code; HTTP Date; JSON log timestamp/PID only. Fixed day/times and all status/header/body/event stable values preserved.'})
save('validation',{'pass':True,'candidate':SHA,'freshInvocations':590,'coverageResourcesVerified':len(hashes),
                   'historicArtifactHashesVerified':len(historic),'archivedCoverageAliases':len(aliases),
                   'methodMappings':len(inventory['methods']),'matrixRawLogPairs':len(normalized),
                   'heldWriterCases':len(matches),'compatibilityReplayInvocations':72,'productionAndJarBytesEqual':True})
print((OUT/'validation.json').read_text())
