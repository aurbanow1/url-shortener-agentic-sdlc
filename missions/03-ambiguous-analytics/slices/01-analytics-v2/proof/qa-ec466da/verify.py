"""Reconcile independent captures, copied reports and the exact candidate. No product mutation."""
import csv, hashlib, json, pathlib, re, subprocess, tarfile, xml.etree.ElementTree as ET
ROOT = pathlib.Path(__file__).resolve().parents[6]
P = pathlib.Path(__file__).resolve().parent
W = ROOT / '.worktrees/01-analytics-v2'
SHA = 'ec466da8da4b1efde9d612c6c8692070cc6fc4b9'
checks = []
def check(name, actual, expected=True):
    ok = actual == expected
    checks.append({'name': name, 'pass': ok, 'actual': actual, 'expected': expected})
    if not ok:
        raise AssertionError(f'{name}: {actual!r} != {expected!r}')
def read(name): return json.loads((P / name).read_text())
def canonical(text):return '\n'.join(line.rstrip() for line in text.splitlines()).rstrip()
def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
def stats(name): return next(r['json'] for r in ledger if r['name'] == name)
def rows(label, code, suffix='final-rows'):
    db = read(label + '-' + suffix + '.json')
    link = next(r for r in db['link'] if r['code'] == code)
    return [r for r in db['click'] if r['link_id'] == link['id']]
check('exact candidate HEAD', git('-C', str(W), 'rev-parse', 'HEAD'), SHA)
check('candidate worktree clean', git('-C', str(W), 'status', '--porcelain'), '')
prov = read('artifact-provenance.json')
check('immutable installed jar', hashlib.sha256(pathlib.Path(prov['jarPath']).read_bytes()).hexdigest(), prov['jarSha256'])
hashes = read('report-hashes.json')
for path, expected in hashes.items():
    check('copied report ' + path, hashlib.sha256((ROOT/path).read_bytes()).hexdigest(), expected)
coverage = {}
for suite in ['unit', 'functional', 'all']:
    report = ROOT/'docs/qa/coverage/01-analytics-v2'/suite/'jacocoTestReport.csv'
    data = list(csv.DictReader(report.open()))
    sums = {k:sum(int(r[k]) for r in data) for k in ['LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED']}
    coverage[suite] = {'lines': [sums['LINE_COVERED'], sums['LINE_COVERED']+sums['LINE_MISSED']],
                       'branches': [sums['BRANCH_COVERED'], sums['BRANCH_COVERED']+sums['BRANCH_MISSED']]}
check('merged coverage', coverage['all'], {'lines':[580,580], 'branches':[206,206]})
suite_counts = {}
for suite, expected in [('unit',221),('functional',241)]:
    xml = [ET.parse(f).getroot() for f in (P/'test-results'/suite).glob('TEST-*.xml')]
    counts = {k:sum(int(r.get(k,0)) for r in xml) for k in ['tests','failures','errors','skipped']}
    suite_counts[suite]=counts
    check(suite+' invocations', counts, {'tests':expected,'failures':0,'errors':0,'skipped':0})
check('authorized original suite', read('v1-authorized-summary.json'), {'tests':155,'failures':0,'errors':0,'skipped':0,'classes':22})
check('literal replay remains disclosed', read('v1-replay-summary.json')['failures'], 2)
decision = next(t for t in read('human-decision-transitions.json') if t['transitionId']==876)
check('human decision exact', decision['transitionNote'], 'accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A')
check('human identity', decision['actorSession'], 'human@kernel')
first_design = git('log','--reverse','--format=%H','--','missions/03-ambiguous-analytics/slices/01-analytics-v2/design.md').splitlines()[1]
check('stage-two SPEC is ancestor of first substantive design', subprocess.run(['git','merge-base','--is-ancestor','b8c327b',first_design],cwd=ROOT).returncode,0)
ledger=read('http-ledger.json'); cases=read('cases.json')
check('HTTP capture count',len(ledger),713)
check('capture names unique',len({r['name'] for r in ledger}),len(ledger))
for row in ledger:
    check(row['name']+' captured header', canonical((P/'http'/(row['name']+'.headers')).read_text()),canonical(row['headers']))
    # curl -I writes HEAD headers to its output file; HeadWireProbe separately checks body bytes.
    raw=(P/'http'/(row['name']+'.body')).read_text()
    check(row['name']+' captured output',canonical(raw),canonical(row['body']))
    check(row['name']+' request id',bool(re.fullmatch(r'[0-9a-f-]{36}',row['headerMap']['x-request-id'])))
    if row['status'] >= 400:
        check(row['name']+' problem type',row['headerMap']['content-type'],'application/problem+json')
        check(row['name']+' problem instance',row['json']['instance'],'urn:uuid:'+row['headerMap']['x-request-id'])
        check(row['name']+' problem status',row['json']['status'],row['status'])
for entry in read('manual-assertions.json'):
    if not entry.get('instrumentCorrection'): check('manual '+entry['name'],entry['pass'])
for label in ['default','trusted']:
    check(label+' readonly database equality',read(label+'-readonly-before.json'),read(label+'-readonly-after.json'))
    for mode,n in [('slow-retry',20),('concurrent',200)]:
        name=label+'-'+mode
        check(name+' total',stats(name+'-stats')['totalClicks'],n)
        data=rows(label,cases[name]);check(name+' stored rows',len(data),n)
        check(name+' values reduced before response',all(r['referrer']=='https://ref.example' and r['user_agent_class']=='browser' for r in data))
        redirects=[r for r in ledger if r['name'].startswith(name+'-burst-')]
        check(name+' response count',len(redirects),n)
        check(name+' response contract',all(r['status']==302 and r['headerMap']['location']=='https://qa-analytics.example/'+name and r['headerMap']['cache-control']=='no-store' for r in redirects))
        if mode=='slow-retry':
            check(name+' all below 250ms',all(r['durationSeconds']<0.250 for r in redirects))
            check(name+' inserts still pending at response completion',len(rows(label,cases[name],suffix='slow-before-settle'))<20)
    before=stats(label+'-metric-before-recorded')['measurements'][0]['value']
    after=stats(label+'-after-recorded')['measurements'][0]['value']
    check(label+' recorded delta',after-before,3)
    before=stats(label+'-metric-before-lost')['measurements'][0]['value']
    after=stats(label+'-after-lost')['measurements'][0]['value']
    check(label+' lost delta',after-before,2)
    check(label+' loss reason count',stats(label+'-after-write-failed')['measurements'][0]['value'],2)
    check(label+' no failed insert rows',len(rows(label,cases[label+'-metrics'])),3)
    check(label+' old clicks purged',read(label+'-purged-rows.json')['click'],[])
    check(label+' purge leaves links',len(read(label+'-purged-rows.json')['link']),len(read(label+'-final-rows.json')['link']))
    click_rows=read(label+'-final-rows.json')['click']
    forbidden=['uacanary','refpathcanary','refquerycanary','reffragcanary','SECRET-UA','SECRET-PATH','SECRET-QUERY','SECRET-FRAGMENT','203.0.113.7','203.0.113.8','198.51.100.1','10.9.9.9','192.0.2.81','192.0.2.82','198.51.100.83','203.0.113.84']
    check(label+' stored privacy',all(x not in json.dumps(click_rows) for x in forbidden))
    check(label+' hash format',all(re.fullmatch(r'[0-9a-f]{64}',r['client_hash']) for r in click_rows))
    scrape=next(r['body'] for r in ledger if r['name']==label+'-prometheus')
    lines=[l for l in scrape.splitlines() if l.startswith('urlshort_clicks_')]
    check(label+' scrape families',any(l.startswith('urlshort_clicks_recorded_total ') for l in lines) and any(l.startswith('urlshort_clicks_lost_total{') for l in lines))
    check(label+' scrape label keys',all('{' not in l or re.fullmatch(r'urlshort_clicks_lost_total\{reason="[^"]+"\} [0-9.]+',l) for l in lines))
    (P/(label+'-click-counters.txt')).write_text('\n'.join(lines)+'\n')
day_rows=rows('default',cases['ac03'])
check('midnight salt hash changes',len({r['client_hash'] for r in day_rows}),2)
check('default varied forwarding remains one identity',len({r['client_hash'] for r in rows('default',cases['ac08'])}),1)
check('trusted four requests yield three identities',len({r['client_hash'] for r in rows('trusted',cases['ac07'])}),3)
check('installed three peers one twice one bot',stats('installed-stats')['clicksPerDay'][0],{'date':'2026-10-03','clicks':4,'uniqueVisitors':3,'botClicks':1})
check('live committed OpenAPI equality',stats('installed-openapi'),json.loads((W/'docs/api/openapi.json').read_text()))
baseline_doc=json.loads(subprocess.check_output(['git','show','2566c38:docs/api/openapi.json'],cwd=ROOT))
candidate_doc=stats('installed-openapi')
for path,value in baseline_doc['paths'].items():
    if path!='/api/links/{code}/stats':check('unchanged OpenAPI operation '+path,candidate_doc['paths'][path],value)
for label in ['default','trusted']:
    for suffix in ['v1-canaries-stats','metric-stats']:
        body=stats(label+'-'+suffix)
        check(label+' aggregate fields '+suffix,list(body),['code','totalClicks','clicksPerDay','topReferrers'])
        check(label+' daily fields '+suffix,all(list(r)==['date','clicks','uniqueVisitors','botClicks'] for r in body['clicksPerDay']))
        check(label+' aggregate privacy '+suffix,all(x not in json.dumps(body) for x in ['uacanary','refpathcanary','refquerycanary','reffragcanary','203.0.113.77','198.51.100.23','client_hash','user_agent_class']))
head=read('head-wire.json')
check('HEAD zero wire bodies', [r['bodyBytes'] for r in head],[0,0,0])
check('HEAD statuses', [r['status'] for r in head],[200,200,410])
correlation={};all_hashes=set()
for f in P.glob('*rows.json'):
    all_hashes.update(r['client_hash'] for r in json.loads(f.read_text()).get('click',[]))
for label,port in [('installed',18170),('default',18171),('trusted',18172)]:
    events=[json.loads(line) for line in (P/(label+'.jsonl')).read_text().splitlines() if line.strip()]
    console=[]
    for line in (P/(label+'-console.jsonl')).read_text().splitlines():
        line=line.removeprefix('^C')
        if line.strip():console.append(json.loads(line))
    outputs=[r for r in ledger if f':{port}/' in r['url']]
    outputs += [{'name':'head-wire','status':r['status'],'headerMap':{'x-request-id':r['headers']['x-request-id'][0]}} for r in head if f':{port}/' in r['url']]
    ids={r['headerMap']['x-request-id']:r for r in outputs}
    completed=[r for r in events if r.get('message')=='request completed']
    check(label+' all request events represented',set(r['requestId'] for r in completed),set(ids))
    for sink,data in [('file',events),('default console',console)]:
        completed=[r for r in data if r.get('message')=='request completed']
        check(label+' '+sink+' completion count',len(completed),len(outputs))
        for rid,row in ids.items():
            found=[r for r in completed if r.get('requestId')==rid]
            check(label+' '+sink+' '+row['name']+' exactly once',len(found),1)
            check(label+' '+sink+' '+row['name']+' status',found[0]['status'],row['status'])
        text=json.dumps(data)
        check(label+' '+sink+' no private canary',all(x not in text for x in ['realcanary','uacanary','refpathcanary','refquerycanary','reffragcanary','SECRET-UA','SECRET-PATH','SECRET-QUERY','SECRET-FRAGMENT','QA-UA-CANARY','QA-ANALYTICS-WRITE-CANARY','must-not-be-logged.example','203.0.113.','198.51.100.','192.0.2.','10.9.9.9']))
        check(label+' '+sink+' no stored hashes',all(h not in text for h in all_hashes))
        lost=[r for r in data if r.get('message')=='click lost']
        check(label+' '+sink+' loss count',len(lost),0 if label=='installed' else 2)
        check(label+' '+sink+' loss correlation and reason',all(r.get('requestId') in ids and r.get('reason')=='write failed' and r['log']['level']=='WARN' and r.get('errorType')=='org.springframework.jdbc.UncategorizedSQLException' for r in lost))
    correlation[label]={'responses':len(outputs),'lostEvents':0 if label=='installed' else 2,'bothSinksMatched':True}
audit=stats('installed-final-audit')['items']
check('installed create and retire audit rows',[(r['action'],r['requestId']) for r in audit],[(name,next(x['headerMap']['x-request-id'] for x in ledger if x['name']==cap)) for name,cap in [('link.retire','installed-retire'),('link.create','installed-key-first'),('link.create','installed-create')]])
check('all app ports stopped',read('shutdown.json')['postStopCurlExitCodes'],[7,7,7])
inventory=read('source-test-inventory.json');invocations=read('invocation-attribution.json')
check('all source methods mapped',len(inventory),282)
check('all invocations mapped',len(invocations),462)
check('no empty trace mapping',all(r['mapsTo'] for r in inventory+invocations))
check('all current ACs have functional source map',{r for m in inventory if m['suite']=='functional' for r in m['mapsTo'] if r.startswith('AC-')},{'AC-'+str(n) for n in range(1,16)})
if (P/'raw-capture-manifest.json').exists():
    manifest=read('raw-capture-manifest.json')
    check('raw archive hash',hashlib.sha256((P/'raw-captures.tar.gz').read_bytes()).hexdigest(),manifest['archiveSha256'])
    with tarfile.open(P/'raw-captures.tar.gz') as archive:
        for name,expected in manifest['files'].items():
            blob=archive.extractfile(name).read()
            check('original raw bytes '+name,hashlib.sha256(blob).hexdigest(),expected)
            display=ROOT/name
            if display.exists() and display.suffix in ['.headers','.body','.xml','.txt']:
                check('display equals canonical raw '+name,canonical(display.read_text()),canonical(blob.decode()))
summary={'candidate':SHA,'checks':len(checks),'allPass':True,'reportFiles':len(hashes),'suiteCounts':suite_counts,'coverage':coverage,'curlResponses':len(ledger),'headWireResponses':len(head),'correlation':correlation,'literalReplay':'153/155; two inherited enumerations disclosed and authorized by a12a0e2','authorizedReplay':'155/155'}
(P/'verification-checks.json').write_text(json.dumps(checks,indent=2,default=lambda v:sorted(v))+'\n')
(P/'verification-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
