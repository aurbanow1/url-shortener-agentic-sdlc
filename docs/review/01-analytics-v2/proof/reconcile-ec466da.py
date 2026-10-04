"""Independent read-only reconciliation; writes only this review's result file."""
import collections, csv, hashlib, json, pathlib, re, subprocess, tarfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[4]
OUT = pathlib.Path(__file__).resolve().parent
Q = ROOT / 'missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-ec466da'
W = ROOT / '.worktrees/01-analytics-v2'
SHA = 'ec466da8da4b1efde9d612c6c8692070cc6fc4b9'
def read(p): return json.loads(p.read_text())
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
def norm(s): return '\n'.join(x.rstrip() for x in s.splitlines()).rstrip()
assert git('-C', str(W), 'rev-parse', 'HEAD') == SHA
assert git('-C', str(W), 'status', '--porcelain') == ''
counts = {}
for suite, expected in [('test', 221), ('functionalTest', 241)]:
    roots = [ET.parse(p).getroot() for p in (W/'build/test-results'/suite).glob('TEST-*.xml')]
    counts[suite] = {key: sum(int(r.get(key, 0)) for r in roots) for key in ['tests','failures','errors','skipped']}
    assert counts[suite] == dict(tests=expected, failures=0, errors=0, skipped=0)
execution = sorted(p.name for p in (W/'build/jacoco').glob('*.exec'))
assert execution == ['functionalTest.exec', 'test.exec']
coverage = {}
for suite in ['test','functionalTest','all']:
    data = list(csv.DictReader((W/'build/reports/jacoco'/suite/'jacocoTestReport.csv').open()))
    coverage[suite] = {k:sum(int(r[k]) for r in data) for k in ['LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED']}
assert coverage['all'] == dict(LINE_MISSED=0, LINE_COVERED=580, BRANCH_MISSED=0, BRANCH_COVERED=206)
hashes = read(Q/'report-hashes.json')
assert all(digest(ROOT/p) == h for p,h in hashes.items())
prov = read(Q/'artifact-provenance.json')
assert prov['candidate'] == SHA and digest(pathlib.Path(prov['jarPath'])) == prov['jarSha256']
manifest = read(Q/'raw-capture-manifest.json')
assert digest(Q/'raw-captures.tar.gz') == manifest['archiveSha256']
with tarfile.open(Q/'raw-captures.tar.gz') as archive:
    for name, expected in manifest['files'].items():
        raw = archive.extractfile(name).read()
        assert hashlib.sha256(raw).hexdigest() == expected, name
        p = ROOT/name
        if p.exists() and p.suffix in ['.body','.headers','.xml','.txt']:
            assert norm(p.read_text()) == norm(raw.decode()), name
ledger = read(Q/'http-ledger.json')
head = read(Q/'head-wire.json')
assert len(ledger) == len({r['name'] for r in ledger}) == 713
assert [r['status'] for r in head] == [200,200,410] and all(r['bodyBytes'] == 0 for r in head)
for r in ledger:
    rawhead = (Q/'http'/(r['name']+'.headers')).read_text()
    rawbody = (Q/'http'/(r['name']+'.body')).read_text()
    assert norm(rawhead) == norm(r['headers']) and norm(rawbody) == norm(r['body']), r['name']
    parsed = dict((k.lower(), v.strip()) for k,v in (line.split(':',1) for line in rawhead.splitlines()[1:] if ':' in line))
    assert parsed == r['headerMap'] and int(rawhead.splitlines()[0].split()[1]) == r['status']
    assert re.fullmatch('[0-9a-f-]{36}', parsed['x-request-id'])
    if r['method'] != 'HEAD' and 'json' in parsed.get('content-type',''):
        assert json.loads(rawbody) == r['json']
    if r['status'] >= 400 and r['method'] != 'HEAD':
        assert parsed['content-type'] == 'application/problem+json'
        assert r['json']['status'] == r['status'] and r['json']['instance'] == 'urn:uuid:'+parsed['x-request-id']
byname = {r['name']:r for r in ledger}
def body(name): return byname[name]['json']
allhashes = {r['client_hash'] for p in Q.glob('*rows.json') for r in read(p).get('click',[])}
private = ['uacanary','refpathcanary','refquerycanary','reffragcanary','SECRET-UA','SECRET-PATH','SECRET-QUERY','SECRET-FRAGMENT','QA-ANALYTICS-WRITE-CANARY','must-not-be-logged.example','203.0.113.','198.51.100.','192.0.2.','10.9.9.9']
joins = {}
for label, port in [('installed',18170),('default',18171),('trusted',18172)]:
    responses = {r['headerMap']['x-request-id']:r['status'] for r in ledger if f':{port}/' in r['url']}
    responses.update({r['headers']['x-request-id'][0]:r['status'] for r in head if f':{port}/' in r['url']})
    for suffix in ['.jsonl','-console.jsonl']:
        text = (Q/(label+suffix)).read_text()
        events = [json.loads(line.removeprefix('^C')) for line in text.splitlines() if line.removeprefix('^C').strip()]
        complete = [e for e in events if e.get('message') == 'request completed']
        assert len(complete) == len(responses)
        assert {e['requestId']:e['status'] for e in complete} == responses
        assert not any(x in text for x in private + list(allhashes))
        lost = [e for e in events if e.get('message') == 'click lost']
        assert len(lost) == (0 if label == 'installed' else 2)
        assert all(e['requestId'] in responses and e['reason'] == 'write failed' and e['log']['level'] == 'WARN' for e in lost)
    joins[label] = len(responses)
assert sum(joins.values()) == 716
stats_verified = []
for label in ['default','trusted']:
    snapshot = read(Q/(label+'-final-rows.json'))
    for r in ledger:
        stat = r.get('json')
        if not isinstance(stat,dict) or 'totalClicks' not in stat or r['method'] == 'HEAD': continue
        if r['name'].startswith('installed-'): continue
        # Polls 0..2 precede the scheduled purge; poll 3 observes its committed deletion.
        observed = read(Q/(label+'-purged-rows.json')) if r['name'].endswith('-purge-check-3') else snapshot
        link = next((v for v in observed['link'] if v['code'] == stat['code']),None)
        if link is None: continue
        rows = [v for v in observed['click'] if v['link_id'] == link['id']]
        # These links receive no later GETs after their stats response; HEAD/OPTIONS add no clicks.
        days = sorted({v['clicked_on'] for v in rows})
        figures = []
        for day in days:
            group = [v for v in rows if v['clicked_on'] == day]
            figures.append(dict(date=day, clicks=len(group), uniqueVisitors=len({v['client_hash'] for v in group}), botClicks=sum(v['user_agent_class']=='bot' for v in group)))
        refs = collections.Counter(v['referrer'] for v in rows if v['referrer'] is not None)
        expected = dict(code=stat['code'],totalClicks=len(rows),clicksPerDay=figures,topReferrers=[dict(referrer=k,clicks=v) for k,v in sorted(refs.items(),key=lambda kv:(-kv[1],kv[0]))[:10]])
        assert stat == expected, r['name']
        stats_verified.append(r['name'])
    assert read(Q/(label+'-readonly-before.json')) == read(Q/(label+'-readonly-after.json'))
    assert read(Q/(label+'-purged-rows.json'))['click'] == []
    assert read(Q/(label+'-purged-rows.json'))['link'] == snapshot['link']
    for metric, delta in [('recorded',3),('lost',2)]:
        assert body(label+'-after-'+metric)['measurements'][0]['value'] - body(label+'-metric-before-'+metric)['measurements'][0]['value'] == delta
    for mode, n in [('slow-retry',20),('concurrent',200)]:
        replies = [r for r in ledger if r['name'].startswith(label+'-'+mode+'-burst-')]
        assert len(replies) == n and all(r['status']==302 and r['headerMap']['cache-control']=='no-store' and r['headerMap']['location']=='https://qa-analytics.example/'+label+'-'+mode for r in replies)
        assert body(label+'-'+mode+'-stats')['totalClicks'] == n
        if mode == 'slow-retry': assert all(r['durationSeconds'] < .250 for r in replies)
    assert all(re.fullmatch('[0-9a-f]{64}',r['client_hash']) for r in snapshot['click'])
    assert not any(v in json.dumps(snapshot['click']) for v in private)
    series = [s for s in byname[label+'-prometheus']['body'].splitlines() if s.startswith('urlshort_clicks_')]
    assert len(series) == 6 and all(re.fullmatch(r'urlshort_clicks_(recorded_total [0-9.]+|lost_total\{reason="[^"]+"\} [0-9.]+)',s) for s in series)
candidate_api = read(W/'docs/api/openapi.json')
assert candidate_api == body('installed-openapi')
baseline_api = json.loads(git('show','2566c38:docs/api/openapi.json'))
for p in baseline_api['paths']:
    if p != '/api/links/{code}/stats': assert candidate_api['paths'][p] == baseline_api['paths'][p]
inventory = read(Q/'source-test-inventory.json')
invocations = read(Q/'invocation-attribution.json')
assert len(inventory) == 282 and len(invocations) == 462
assert all(r['mapsTo'] for r in inventory+invocations)
assert all(re.search(r'\b'+re.escape(r['method'])+r'\s*\(', (W/r['path']).read_text()) for r in inventory)
for suite, folder in [('unit','test'),('functional','functionalTest')]:
    claimed = collections.Counter((r['class'],r['name']) for r in invocations if r['suite']==suite)
    saved = collections.Counter((c.get('classname'),c.get('name')) for f in (Q/'test-results'/suite).glob('TEST-*.xml') for c in ET.parse(f).getroot().findall('testcase'))
    assert saved == claimed
    def stable_name(c):
        name = re.sub(r'MockHttpServletRequestBuilder@[0-9a-f]+','MockHttpServletRequestBuilder@INSTANCE',c.get('name'))
        if c.get('classname') == 'dev.urlshort.web.ObservabilityJourneyTest':
            name = re.sub(r'canary = "[^"]+"','canary = "GENERATED"',name)
        return c.get('classname'),name
    fresh = collections.Counter(stable_name(c) for f in (W/'build/test-results'/folder).glob('TEST-*.xml') for c in ET.parse(f).getroot().findall('testcase'))
    previous = collections.Counter(stable_name(c) for f in (Q/'test-results'/suite).glob('TEST-*.xml') for c in ET.parse(f).getroot().findall('testcase'))
    assert fresh == previous
assert {ac for r in inventory if r['suite']=='functional' for ac in r['mapsTo'] if ac.startswith('AC-')} == {f'AC-{i}' for i in range(1,16)}
assert read(Q/'v1-replay-summary.json')['failures'] == 2
assert read(Q/'v1-authorized-summary.json') == dict(tests=155,failures=0,errors=0,skipped=0,classes=22)
live = read(OUT/'live-proof-ec466da.json')
refs = [ref for item in live['items'] if item['judgment'] for ref in item['judgment']['evidence']]
assert all(digest(ROOT/r['ref']) == r['sha256'] for r in refs)
assert [item['index'] for item in live['items'] if item['state']=='pending'] == [11,12]
assert all(item['judgment']['subject']['ref'] == SHA for item in live['items'] if item['judgment'])
result = dict(candidate=SHA,base=git('merge-base','main',SHA),changedFiles=git('diff','--name-only','main...'+SHA).splitlines(),freshTests=counts,freshCoverage=coverage,executionData=execution,qaReportHashes=len(hashes),rawArchiveFiles=len(manifest['files']),curlResponses=len(ledger),headWireResponses=len(head),bothLogSinks=joins,recomputedStats=stats_verified,sourceMethods=len(inventory),mappedInvocations=len(invocations),proofReferenceHashes=len(refs),pendingProof=[11,12],allChecksPassed=True)
(OUT/'evidence-reconciliation-ec466da.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
