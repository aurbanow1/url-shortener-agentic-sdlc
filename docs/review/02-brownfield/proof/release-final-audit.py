#!/usr/bin/env python3
"""Independent release-package reconciliation; writes only this review's result."""
import collections, hashlib, io, json, pathlib, re, subprocess, tarfile, xml.etree.ElementTree as ET, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[4]
HERE=ROOT/'docs/review/02-brownfield/proof'
PACKAGE='c9aedabd'; CANDIDATE='30f8de4e647b05ff54cde09f1019ae519b00069d'
CORRECTION='ad0c01f6b517340ad3fb5a64fcc143c078196232'
REL='missions/02-brownfield/release/final-30f8de4e/'
WT=ROOT/'.worktrees/review-release02-30f8de4e'
cache={}
def git(*args): return subprocess.check_output(['git',*args],cwd=ROOT)
def blob(path,pin=PACKAGE):
    k=(path,pin)
    if k not in cache: cache[k]=git('show',pin+':'+path)
    return cache[k]
def obj(path):return json.loads(blob(path))
def sha(data):return hashlib.sha256(data).hexdigest()
def archive(path):
    with tarfile.open(fileobj=io.BytesIO(blob(path))) as t:return {m.name:t.extractfile(m).read() for m in t.getmembers() if m.isfile()}
def totals(files):
    result={};cases={}
    for name,b in files.items():
        x=ET.fromstring(b);suite=name.split('/')[0]
        r=result.setdefault(suite,{k:0 for k in ['tests','failures','errors','skipped']})
        for k in r:r[k]+=int(x.attrib[k])
        cases[name]=sorted((e.get('classname'),e.get('name')) for e in x.findall('testcase'))
    return result,cases
out={'package':PACKAGE,'product':CANDIDATE,'checks':{}};c=out['checks']
inputs=['src','build.gradle.kts','settings.gradle.kts','gradle.properties','gradle','gradlew','gradlew.bat','scripts','Dockerfile','compose.yaml','.github']
assert not git('diff','e40b095',CANDIDATE,'--',*inputs)
assert not git('diff',CANDIDATE,PACKAGE,'--',*inputs)
delta=git('diff','--name-only','e227acf',CANDIDATE,'--',*inputs).decode().splitlines();assert len(delta)==10
c['product_delta']={'paths':delta,'equal_to_reviewed_candidate':'e40b095','package_product_diff_empty':True}
for line in blob(REL+'candidate-input-tree.txt').decode().splitlines():
    meta,path=line.split('\t',1);assert meta.split()[2]==git('rev-parse',CANDIDATE+':'+path).decode().strip(),path
c['input_tree_rows']=len(blob(REL+'candidate-input-tree.txt').splitlines())
# Both actual release jars; compare their classes/resources to our freshly compiled exact pin.
a=obj(REL+'artifact-comparison.json');checks=[]
for jar in a['jars']:
    data=pathlib.Path(jar['path']).read_bytes();assert sha(data)==jar['sha256'] and len(data)==jar['bytes'];checks.append({'path':jar['path'],'sha256':sha(data),'bytes':len(data)})
with zipfile.ZipFile(a['jars'][0]['path']) as z:
    packaged={n.removeprefix('BOOT-INF/classes/'):z.read(n) for n in z.namelist() if n.startswith('BOOT-INF/classes/') and not n.endswith('/')}
    compiled={}
    for path in [WT/'build/classes/java/main',WT/'build/resources/main']:
        compiled.update({str(p.relative_to(path)):p.read_bytes() for p in path.rglob('*') if p.is_file()})
    assert packaged==compiled,{'missing':list(packaged.keys()-compiled.keys()),'extra':list(compiled.keys()-packaged.keys()),'different':[p for p in packaged.keys()&compiled.keys() if packaged[p]!=compiled[p]]}
c['jars']=checks;c['fresh_compiled_entries_equal_jar']=len(compiled)
# Archived producer results versus independent fresh gate, including exact case identities.
saved=archive(REL+'fresh-test-xml.tar.gz');current={str(p.relative_to(WT/'build/test-results')):p.read_bytes() for p in (WT/'build/test-results').rglob('TEST-*.xml')}
producer,pcases=totals(saved);reviewer,rcases=totals(current)
assert producer==reviewer==obj(REL+'gate-artifact.json')['suites'] and pcases.keys()==rcases.keys()
display_differences=[n for n in pcases if pcases[n]!=rcases[n]]
assert set(display_differences)=={'functionalTest/TEST-dev.urlshort.link.LinkReadRetireJourneyTest.xml','functionalTest/TEST-dev.urlshort.web.ObservabilityJourneyTest.xml'}
for n in display_differences:
    assert len(pcases[n])==len(rcases[n])
    def stable(case):
        klass,name=case;name=re.sub(r'MockHttpServletRequestBuilder@[0-9a-f]+','MockHttpServletRequestBuilder@<runtime>',name);name=re.sub(r'canary = "[^"]*"','canary = "<generated>"',name);return klass,name
    assert sorted(map(stable,pcases[n]))==sorted(map(stable,rcases[n])),n
c['gate']={'producer':producer,'reviewer':reviewer,'matching_reports':len(saved),'exact_case_display_matches':len(saved)-len(display_differences),'runtime_display_qualifications':display_differences,'normalization':'only MockHttpServletRequestBuilder identity hex and generated canary parameter; all other name/status/index bytes retained'}
xml=ET.fromstring(blob(REL+'canonical-jacocoTestReport.xml'));fresh=ET.parse(WT/'build/reports/jacoco/all/jacocoTestReport.xml').getroot()
def counters(x):return {v.attrib['type']:{k:int(v.attrib[k]) for k in ['missed','covered']} for v in x.findall('counter')}
assert counters(xml)==counters(fresh)==obj(REL+'gate-artifact.json')['canonicalCounters'] and all(v['missed']==0 for v in counters(fresh).values())
c['coverage']=counters(fresh)
assert set(archive(REL+'coverage-exec.tar.gz'))=={'test.exec','functionalTest.exec'}
(HERE/'release-coverage-30f8de4e.csv').write_bytes((WT/'build/reports/jacoco/all/jacocoTestReport.csv').read_bytes())
# Raw final runtime exchanges correlate by request id and status to the retained console.
lograw=blob(REL+'container-console.jsonl').decode();events=[json.loads(l) for l in lograw.splitlines() if l.startswith('{')]
nonjson=[l for l in lograw.splitlines() if not l.startswith('{')];assert nonjson==['Picked up JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75']
request={e['requestId']:e for e in events if e.get('message')=='request completed'}
records=obj(REL+'installed-wire.json')['records'];joined=[];parsed={}
for r in records:
    txt=r['output'].replace('\r\n','\n');headers,body=txt.split('\n\n',1);status=int(re.search(r'HTTP/1.1 (\d+)',headers)[1]);rid=re.search(r'X-Request-Id: ([^\n]+)',headers)[1]
    assert request[rid]['status']==status;joined.append({'label':r['label'],'status':status,'requestId':rid})
    parsed[r['label']]={'status':status,'body':json.loads(body) if body else None,'headers':headers}
assert parsed['create']['body']==parsed['read']['body']
for label in ['redirect','head']:
    assert parsed[label]['status']==302 and 'Cache-Control: no-store' in parsed[label]['headers'] and 'Location: https://example.com/mission02-final' in parsed[label]['headers']
assert parsed['stats']['body']=={'code':'lgGaomzq','totalClicks':1,'clicksPerDay':[{'date':'2026-10-04','clicks':1,'uniqueVisitors':1,'botClicks':0}],'topReferrers':[{'referrer':'https://example.net','clicks':1}]}
assert parsed['bad-create']['status']==400 and parsed['bad-create']['body']['errors']==[{'field':'url','rule':'scheme','message':'url must start with http:// or https://'}]
for label,status in [('unknown',404),('retire',204),('retired-redirect',410)]:assert parsed[label]['status']==status
# Preserve/exclude the known wrong-URL 404 and verify the separate actual audit request.
audit=blob(REL+'audit-forbidden.txt').decode();rid=re.search(r'X-Request-Id: ([^\n]+)',audit)[1];body=json.loads(audit[audit.index('{'):])
assert body=={'instance':'urn:uuid:'+rid,'status':403,'title':'Forbidden'} and request[rid]['status']==403
joined.append({'label':'actual-audit','status':403,'requestId':rid})
for canary in ['private?q=canary','javascript:','192.0.2.10','X-Forwarded-For: 127.0.0.1']:assert canary not in lograw
assert any(e.get('message')=='Graceful shutdown complete' for e in events)
c['runtime']={'joined_exchanges':joined,'console_events':len(events),'non_json_launcher_line':nonjson,'historical_wrong_audit_url_excluded':True}
inspect=obj(REL+'container-inspect.json')[0];stopped=obj(REL+'container-stopped.json')[0];image=obj(REL+'image-inspect.json')[0]
assert inspect['Image']==a['image']==image['Id'] and stopped['Id']==inspect['Id']
assert inspect['HostConfig']['ReadonlyRootfs'] and inspect['HostConfig']['PortBindings']=={'8080/tcp':[{'HostIp':'127.0.0.1','HostPort':'18241'}]}
assert inspect['Config']['StopTimeout']==20 and not stopped['State']['Running']
assert any(m['Destination']=='/app/data' and m['RW'] and m['Name']=='urlshort-mission02-final-30f8de4e-data' for m in inspect['Mounts'])
assert image['Config']['User']=='urlshort' and b'--uid 10001' in blob('Dockerfile',CANDIDATE)
c['container']={'id':inspect['Id'],'image':image['Id'],'loopback_readonly_data_mount':True,'stopped':stopped['State']}
# Current ready receipts must have exact committed evidence, not just accepted status.
proof=json.loads((HERE/'release-proof-live-c9aedabd.json').read_text());assert proof['state']=='ready' and not proof['issues']
savedproof=obj(REL+'proof-ready.json');assert proof['revision']==savedproof['revision']
refs={};counts={};subjects=collections.Counter();judges=collections.Counter()
for sl in proof['slices']:
    ready=sl['readiness'];assert ready['state']=='ready' and not ready['issues'];counts[sl['id']]=len(ready['items'])
    for item in ready['items']:
        assert item['state']=='accepted';j=item['judgment'];assert j['verdict']=='accept' and j['provenance']=='transport:v1';judges[j['actor']]+=1;subjects[j['subject']['ref']]+=1
        for r in j['evidence']:
            candidates=[ROOT/r['ref'],ROOT/'missions/02-brownfield/slices'/sl['id']/r['ref']];p=next(p for p in candidates if p.is_file());relative=str(p.relative_to(ROOT))
            assert sha(p.read_bytes())==r['sha256']==sha(blob(relative)),relative;refs[relative]=r['sha256']
assert sum(counts.values())==64 and len(refs)==174
assert blob(REL+'GAPS-snapshot.md')==blob('docs/qa/GAPS.md')
c['proof']={'revision':proof['revision'],'counts':counts,'evidence_files':len(refs),'subjects':dict(subjects),'judges':dict(judges),'evidence':refs}
# New rollback executable and unchanged raw historical stage archive.
r=obj(REL+'rollback-artifact.json');prior=pathlib.Path('/private/tmp/urlshort-mission02-rollback-f090103');priorjar=prior/'build/libs/urlshort.jar';assert sha(priorjar.read_bytes())==r['sha256'] and priorjar.stat().st_size==r['bytes']
oldpaths=git('ls-tree','-r','--name-only',r['commit'],'--','src/main','build.gradle.kts','settings.gradle.kts').decode().splitlines()
for path in oldpaths:assert (prior/path).read_bytes()==blob(path,r['commit']),path
oldlog=blob(REL+'rollback-prior.jsonl').decode();assert 'Successfully validated 2 migrations' in oldlog and 'Current version of schema "PUBLIC": 2' in oldlog and 'No migration necessary' in oldlog and 'Graceful shutdown complete' in oldlog
assert b'SMOKE OK' in blob(REL+'rollback-prior-smoke.txt')
assert sha(blob(REL+'rollback-raw-csv.tar.gz'))=='f9c180b250c2e136b0361d313f4589acfa0b3cceb1269036e95a27f2f786ee24'
c['rollback']={'jar':r,'prior_source_files_match':len(oldpaths),'schema2_startup_and_shutdown_log':True,'raw_stages_unchanged':True,'old_image_separately_smoked':False,'old_log_format':'plain file log despite .jsonl suffix'}
# Frozen metrics: validate every archive input and derive raw closure/retry counts independently.
files=archive(REL+'final-metrics-inputs.tar.gz');prov=obj(REL+'final-metrics-provenance.json');metrics=obj(REL+'final-metrics.json')
assert sha(files['tools/sdlc-metrics.mjs'])==prov['generatorSha256']==sha(blob('tools/sdlc-metrics.mjs',CANDIDATE))
for r in prov['inputs']:assert sha(files[r['path']])==r['sha256'],r['path']
assert json.loads(files['expected-metrics.json'])==metrics
traces={json.loads(b)['instance']['instanceId']:json.loads(b) for n,b in files.items() if n.endswith('.trace.json')}
summary=[]
for row in metrics['instances']:
    t=traces[row['instanceId']]['trail'];counts_=collections.Counter(e['stepId'] for e in t if e['closureReason']!='waiting');fail=sum(e['closureReason']=='failed' for e in t);retry=fail+sum(n-1 for n in counts_.values())
    assert len(t)==row['stepClosures'] and fail==row['failedClosures'] and retry==row['retries']
    summary.append({'instance':row['instanceId'],'closures':len(t),'failed':fail,'retries':retry})
assert sum(r['closures'] for r in summary)==249 and sum(r['failed'] for r in summary)==18 and sum(r['retries'] for r in summary)==55
c['metrics']={'manifest_inputs':len(prov['inputs']),'archive_members':len(files),'rows':summary,'totals':metrics['totals'],'all_field_generator_replay':'separately executed verify-metrics.py; pass'}
# Check all current package JSON and local references; retain hashes of all final files.
filelist=git('ls-tree','-r','--name-only',PACKAGE,'--',REL).decode().splitlines()
for p in filelist:
    if p.endswith('.json'):
        raw=blob(p)
        if p==REL+'hosted-runs.json':
            assert raw.startswith(b'+{')
            hosted=json.loads(raw[1:])
            assert hosted['candidate']==CANDIDATE and hosted['response']=={'total_count':0,'workflow_runs':[]}
            c['RR-01']={'severity':'LOW','file':p+':1','evidence':'Committed leading + makes JSON invalid; content read after explicitly removing that one byte for diagnosis only. Producer correction requested; original preserved at package pin.'}
        else:json.loads(raw)
c['package_file_hashes']={p:sha(blob(p)) for p in filelist}
ad=obj(REL+'advisories.json');assert len(ad['dependencies'])==len(ad['raw']['results'])==97 and all(not r for r in ad['raw']['results']) and not ad['findings'];c['osv']={'at':ad['queriedAt'],'queries':97,'advisories':0}
links=[]
for doc in ['missions/02-brownfield/RELEASE.md','docs/metrics/README.md','README.md','docs/evidence/02-brownfield/INDEX.md','docs/evidence/02-brownfield/INDEX-notes.md',REL+'README.md',REL+'rollback.md']:
    for href in re.findall(r'\[[^\]]*\]\(([^)]+)\)',blob(doc).decode()):
        if re.match(r'(?:https?|mailto):',href):continue
        name,sep,anchor=href.partition('#');p=((ROOT/doc).parent/name).resolve() if name else ROOT/doc;assert p.exists(),(doc,href)
        if anchor:
            used=collections.Counter();heads=[]
            for line in p.read_text().splitlines():
                if re.match(r'^#{1,6} ',line):
                    v=re.sub(r'[^\w -]','',re.sub(r'^#{1,6} ','',line).strip().lower()).replace(' ','-');heads.append(v+(f'-{used[v]}' if used[v] else ''));used[v]+=1
            assert anchor in heads,(doc,href)
        links.append({'from':doc,'target':href})
c['links']={'count':len(links),'targets':links}
# Full export and changed-file coverage, with machine-consumed records distinguished.
export='docs/evidence/02-brownfield/'
export_paths=git('ls-tree','-r','--name-only',PACKAGE,'--',export).decode().splitlines()
export_json={p:json.loads(blob(p)) for p in export_paths if p.endswith('.json')}
validation=export_json[export+'prep-validation.json']
for r in validation['records']:assert sha(blob(export+r['path']))==r['sha256'],r['path']
assert len(validation['records'])==468 and len(export_json)==469
assert export_json[export+'proof-readiness.json']['revision']==proof['revision']
assert validation['proofEvidenceHashes']==refs
assert obj('docs/metrics/metrics.json')==metrics
changed=git('diff','--name-only',PACKAGE+'^',PACKAGE).decode().splitlines();assert len(changed)==263
ledger=[]
for p in changed:
    data=blob(p)
    if p.startswith(export) and p.endswith('.json'):
        method='Parsed entire exported JSON; checked preparation manifest/hash, current readiness or frozen engine metric context as applicable. Other mission records retain their own judgments.'
    elif p==REL+'hosted-runs.json':method='RR-01 LOW: invalid leading plus reproduced; content read, producer correction pending.'
    elif p.endswith('.tar.gz'):
        members=archive(p);assert members
        method=f'Archive read: {len(members)} files; test/coverage, historical rollback or metric inputs reconciled as described in report.'
    elif p.startswith(REL):method='Read/parsed complete release evidence; reconciled with pinned source, fresh gate, runtime exchanges, artifact bytes or historical checks as described in report.'
    else:method='Read final prose/data and diff; release claims, links, dispositions and frozen metrics checked.'
    ledger.append({'path':p,'sha256':sha(data),'verdict':'LOW RR-01' if p==REL+'hosted-runs.json' else 'PASS within stated scope','method':method})
c['file_coverage']={'changed_files':len(changed),'export_json_parsed':len(export_json),'manifest_hashes':len(validation['records']),'ledger':ledger}
corrected=REL+'hosted-runs.json'
assert git('diff','--name-only',PACKAGE,CORRECTION).decode().splitlines()==[corrected]
assert blob(corrected,CORRECTION)==blob(corrected)[1:]
assert json.loads(blob(corrected,CORRECTION))==hosted
c['RR-01'].update({'resolution':'fixed','correction_commit':CORRECTION,'corrected_sha256':sha(blob(corrected,CORRECTION)),'proof':'Exact single-file delta removes only the leading plus; corrected JSON parses; candidate, response and qualification unchanged.'})
out['final_package']=CORRECTION
(HERE/'release-final-audit-c9aedabd.json').write_text(json.dumps(out,indent=2)+'\n')
print(json.dumps({'product_delta_paths':len(delta),'compiled_entries':len(compiled),'gate':c['gate'],'coverage':c['coverage'],'runtime_joins':len(joined),'proof':{'counts':c['proof']['counts'],'hashes':len(refs)},'rollback_source_files':len(oldpaths),'metric_rows':len(summary),'local_links':len(links),'package_files':len(filelist),'result':'release-final-audit-c9aedabd.json'},indent=2))
