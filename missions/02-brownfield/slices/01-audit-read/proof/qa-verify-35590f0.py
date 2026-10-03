"""Reconcile independently captured QA evidence; never modify product code."""
import csv, hashlib, json, pathlib, subprocess, collections, datetime, re, tarfile, shutil

ROOT = pathlib.Path(__file__).resolve().parents[5]
P = pathlib.Path(__file__).resolve().parent
TREE = ROOT / '.worktrees/01-audit-read'
SHA = '35590f06c852543c29097a42c43b7802be90ba40'
checks = []
def check(condition, name):
    assert condition, name
    checks.append(name)
def read(name): return json.loads((P / name).read_text())
def normalize(row):
    value = row['occurred_at'].replace(' ', 'T')
    if re.search(r'[+-]\d\d$', value): value += ':00'
    value = re.sub(r'\.(\d{1,5})(?=[+-])', lambda m: '.'+m.group(1).ljust(6,'0'), value)
    at = datetime.datetime.fromisoformat(value)
    stamp = at.astimezone(datetime.timezone.utc).isoformat(timespec='milliseconds' if at.microsecond else 'seconds').replace('+00:00', 'Z')
    return dict(occurredAt=stamp, actor=row['actor'], action=row['action'], entity=row['entity'], entityId=row['entity_id'], requestId=row['request_id'], before=None if row['before_state'] is None else json.loads(row['before_state']), after=json.loads(row['after_state']))

check(subprocess.check_output(['git','-C',str(TREE),'rev-parse','HEAD'],text=True).strip()==SHA,'Exact candidate HEAD')
check(not subprocess.check_output(['git','-C',str(TREE),'status','--porcelain'],text=True),'Clean product worktree')
ledger = read('qa-http-ledger-35590f0.json')
if not any(x['name'].startswith('audit-burst-') for x in ledger):
    for i in range(1,91):
        name='audit-burst-%02d' % i
        h=(P/'qa-http'/(name+'.headers')).read_text()
        b=(P/'qa-http'/(name+'.body')).read_text()
        ledger.append(dict(name=name,method='GET',url='http://127.0.0.1:18131/api/audit?limit=1',peer='controlled 127.0.0.100',headersSent=[],headers=h,body=b,json=json.loads(b),status=int(h.splitlines()[0].split()[1]),headerMap=dict((x.split(':',1)[0].lower(),x.split(':',1)[1].strip()) for x in h.splitlines()[1:] if ':' in x)))
    (P/'qa-http-ledger-35590f0.json').write_text(json.dumps(ledger,indent=2)+'\n')
byname={x['name']:x for x in ledger}
check(len(byname)==len(ledger),'Unique capture names')
check([byname['audit-burst-%02d'%i]['status'] for i in range(1,91)]==[200]*60+[429]*30,'Audit burst: first60 admitted, following30 limited')
check(all(int(byname['audit-burst-%02d'%i]['headerMap']['retry-after'])>=1 for i in range(61,91)),'429 Retry-After positive')
events=[]
for f in sorted(P.glob('qa-*.jsonl')):
    for line in f.read_text().splitlines():
        if line.strip():
            event=json.loads(line)
            if 'requestId' in event:
                events.append(dict(file=f.name,event=event))
index=collections.defaultdict(list)
for e in events:index[e['event']['requestId']].append(e)
for r in ledger:
    rid=r['headerMap'].get('x-request-id')
    check(bool(rid),'Header request id: '+r['name'])
    check(bool(index[rid]),'JSON log correlation: '+r['name'])
    check(any(e['event'].get('message')=='request completed' and e['event'].get('status')==r['status'] for e in index[rid]),'Completion status matches: '+r['name'])
    for e in index[rid]:check(e['event']['requestId']==rid,'Every request event ID agrees: '+r['name'])
bad=['QA-AUDIT-URL-CANARY','QA-AUDIT-STORED-CANARY','QA-AUDIT-UA-CANARY','QA-AUDIT-SQL-FAIL-CANARY','QA-SETTINGS-CONTENT-CANARY','QA-UPGRADE-URL-CANARY','QA-UPGRADE-UA-CANARY','QA-REF-CANARY','192.0.2.10','198.51.100.9','https://example.org/','https://referrer.example/','OTEyMzQ1Njc4OQ']
for r in ledger:
    if r.get('json') and isinstance(r['json'],dict) and r['json'].get('next'):bad.append(r['json']['next'])
request_text='\n'.join(json.dumps(e['event']) for e in events)
whole_text='\n'.join(f.read_text() for f in P.glob('qa-*.jsonl'))
check(all(value not in whole_text for value in set(bad)),'No audit/client/cursor/SQL-message canaries in whole-run logs')
for a,b in [('qa-live-real-before.json','qa-live-real-after.json'),('qa-seed45-before.json','qa-seed45-after.json'),('qa-readonly-before.json','qa-readonly-after.json')]:check(read(a)==read(b),'All stored tables unchanged: '+a)
for prefix,snapshot in [('ac06-page-','qa-seed45-before.json'),('ac10-pass1-page','qa-readonly-before.json'),('ac10-pass2-page','qa-readonly-before.json')]:
    responses=sorted((r for r in ledger if r['name'].startswith(prefix)),key=lambda x:int(x['name'][len(prefix):]))
    check([len(r['json']['items']) for r in responses]==[20,20,5],prefix+' page sizes')
    check([item for r in responses for item in r['json']['items']]==[normalize(row) for row in reversed(read(snapshot)['audit_log'])],prefix+' every row and field equals store')
real=byname['ac04-order-read-1']['json']['items']
check(real==[normalize(row) for row in reversed(read('qa-live-real-before.json')['audit_log'])],'Four real mutation rows exact and write-ordered')
for letter in 'ABC':
    r=byname['fixed-ac02-create-'+letter]
    a=next(x for x in real if x['action']=='link.create' and x['entityId']==r['json']['code'])
    check(a['requestId']==r['headerMap']['x-request-id'] and a['before'] is None and a['after']==dict(url=r['json']['url'],state='active'),'Create after and requestID: '+letter)
    check(r['before']<=a['occurredAt']<=r['after'],'Create audit time inside client interval: '+letter)
check(real[0]['before']['state']=='active' and real[0]['after']['state']=='retired' and real[0]['requestId']==byname['ac03-retire-A']['headerMap']['x-request-id'],'Retire before/after and requestID')
for prefix,first,snapshot,fresh,n in [('ac08-continuation-','ac08-first','qa-ac08-before.json','ac08-fresh',35),('ac20-continuation-','ac20-first-held','qa-ac20-before.json','ac20-fresh',31)]:
    cont=sorted((r for r in ledger if r['name'].startswith(prefix)),key=lambda x:int(x['name'][len(prefix):]))
    items=byname[first]['json']['items']+[x for r in cont for x in r['json']['items']]
    check([x for x in items if x['entityId']!='qa-held-row']==[normalize(row) for row in reversed(read(snapshot)['audit_log'])],prefix+' original30 exactly once')
    check(len({x['requestId'] for x in items})==len(items),prefix+' no duplicate')
    check(len(byname[fresh]['json']['items'])==n and len({x['requestId'] for x in byname[fresh]['json']['items']})==n,fresh+' all expected rows unique')
check(byname['ac20-fresh']['json']['items'][0]['entityId']=='qa-held-row','Fresh traversal includes late-committing held write first')
failure=byname['ac21-store-failed']
check(failure['status']==500 and set(failure['json'])=={'instance','status','title'},'SQL read failure is safe500 problem without page')
check(byname['ac21-recovered']['status']==200,'Store recovery200')
for r in ledger:
    if r['status'] in (400,403,405,429,500):
        check(r['headerMap']['content-type'].startswith('application/problem+json'),'Problem media type: '+r['name'])
        if r['method']!='HEAD':check('items' not in r['json'] and 'next' not in r['json'],'No trail in error: '+r['name'])
    if r['method']=='HEAD':check('"items"' not in r['body'] and 'QA-AUDIT' not in r['body'],'HEAD curl has headers only: '+r['name'])
committed=json.loads(subprocess.check_output(['git','-C',str(TREE),'show',SHA+':docs/api/openapi.json'],text=True))
live=byname['upgrade-candidate-api']['json']
check(live==committed==byname['live-api-document']['json'],'Whole live API equals committed candidate document')
old=byname['upgrade-shipped-api']['json']
check(all(live['paths'][p]==v for p,v in old['paths'].items()),'Every existing operation/response/example unchanged')
audit=live['paths']['/api/audit']['get']
check(set(audit['responses'])=={'200','400','403','429','500'},'Audit API documents success and all error responses including429')
check({p['name'] for p in audit['parameters']}=={'limit','cursor'},'Audit paging parameters documented')
check(bool(audit['responses']['200']['content']['application/json'].get('examples')),'Audit example documented')
(P/'qa-api-diff-35590f0.txt').write_text('PASS: entire key-sorted candidate live /v3/api-docs equals candidate docs/api/openapi.json.\nPASS: shipped path operations, parameters, responses and examples unchanged.\nDiff: empty\n')
before,after=read('qa-upgrade-before.json'),read('qa-upgrade-after.json')
check(before['audit_log']==after['audit_log'] and before['link']==after['link'],'Installed upgrade preserves all stored link/audit columns')
check(len(before['click'])==3 and len(after['click'])==4 and before['click']==after['click'][:3],'Installed upgrade preserves three clicks; verificationGET adds only fourth')
check(byname['upgrade-candidate-audit-page1']['json']['items']+byname['upgrade-candidate-audit-page2']['json']['items']==[normalize(row) for row in reversed(before['audit_log'])],'Upgrade pages exactly reflect original shipped audit rows')
for name in ['active','retired']:check(byname['upgrade-shipped-stats-'+name]['json']==byname['upgrade-candidate-stats-'+name]['json'],'Upgrade preserves complete statistics: '+name)
invocations=read('qa-test-invocations-35590f0.json')
for suite in ['unit','functional']:check(len(invocations[suite])==200 and all(t['result']=='PASS' for t in invocations[suite]),'200 green '+suite+' invocations')
shipped=read('qa-shipped-suite-summary-f6dd29e.json')
check(shipped['count']==155 and shipped['skipped']==0 and {x['method'] for x in shipped['failures']}=={'AC28_liveDocumentDescribesTheSlice()','AC20_everyOperationDocumentsTheTooManyRequestsProblem()'},'Original155: onlytwo authorized enumeration failures,153pass')
coverage={}
for suite in ['unit','functional','all']:
    rows=list(csv.DictReader(next((ROOT/'docs/qa/coverage/01-audit-read'/suite).glob('*.csv')).open()))
    totals={k:sum(int(r[k]) for r in rows) for k in ['LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED']}
    coverage[suite]=totals
check(coverage['all']==dict(LINE_MISSED=0,LINE_COVERED=492,BRANCH_MISSED=0,BRANCH_COVERED=190),'Merged CSV exactly492/492lines190/190branches')
hashes=read('qa-report-copy-hashes-35590f0.json')
for path,digest in hashes.items():check(hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,'Byte-equal report copy: '+path)
result=dict(candidate=SHA,httpCaptures=len(ledger),requestLogEvents=len(events),statuses=dict(collections.Counter(str(x['status']) for x in ledger)),assertionCount=len(checks),coverage=coverage,passedChecks=checks)
(P/'qa-verification-35590f0.json').write_text(json.dumps(result,indent=2)+'\n')
(P/'qa-request-log-lines-35590f0.json').write_text(json.dumps(events,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='passedChecks'},indent=2))
