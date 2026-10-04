#!/usr/bin/env python3
"""Read-only reconciliation of the historical release records, not a new gate."""
import collections, csv, datetime, hashlib, io, json, math, pathlib, re, subprocess, tarfile, xml.etree.ElementTree as ET
ROOT = pathlib.Path(__file__).resolve().parents[4]
PIN = '8d3c536'
PRE = 'missions/02-brownfield/release/pre-run-e227acf/'
OUT = ROOT / 'docs/review/02-brownfield/proof/release-preread.json'
cache = {}
def blob(path, pin=PIN):
    key=(pin,path)
    if key not in cache: cache[key]=subprocess.check_output(['git','show',pin+':'+path],cwd=ROOT)
    return cache[key]
def obj(path,pin=PIN): return json.loads(blob(path,pin))
def sha(b): return hashlib.sha256(b).hexdigest()
def rows(b): return list(csv.DictReader(io.StringIO(b.decode())))
def ts(s): return datetime.datetime.fromisoformat(s.replace('Z','+00:00')).timestamp()
def rnd(n): return math.floor(n+0.5)
result={'historical_package':'47e3a07','metrics_pin':PIN,'checks':{},'limitations':[]}
c=result['checks']
# Custody: immutable Git inputs, actual retained artifacts, canonical report.
gate=obj(PRE+'gate-artifact.json'); product=gate['candidate']; inventories=[]
for name in ['source-before.json','source-after-gate.json']:
    inv=obj(PRE+name)
    for p in inv['paths']:
        b=blob(p['path'],product); assert sha(b)==p['committedSha256'],p
        assert p['workingSha256']==sha(b) or (p['path']=='gradlew.bat' and p['workingSha256']==sha(b.replace(b'\n',b'\r\n'))),p
    inventories.append(len(inv['paths']))
c['source_inventory_rows']=inventories
c['artifact_hashes']={}
for k,a in obj(PRE+'artifact-comparison.json').items():
    if isinstance(a,dict):
        b=pathlib.Path(a['path']).read_bytes(); assert len(b)==a['bytes'] and sha(b)==a['sha256']; c['artifact_hashes'][k]={'bytes':len(b),'sha256':sha(b)}
for r in obj(PRE+'coverage-report-hashes.json'): assert sha(blob(r['path']))==r['sha256']
x=ET.fromstring(blob(PRE+'canonical-jacocoTestReport.xml')); counters={r.attrib['type']:{k:int(r.attrib[k]) for k in ['covered','missed']} for r in x.findall('counter')}
assert counters==obj(PRE+'coverage-canonical.json')['counters']; c['coverage']=counters
assert b'BUILD SUCCESSFUL' in blob(PRE+'check-bootjar.txt')
c['gate_log_success']=True
result['limitations'].append('Historical suite counts are the authored gate-artifact summary; no retained per-test XML is in this pre-run directory. No new build is claimed.')
# Four rollback stages, complete rows, schema and constraints; use archive without extraction.
archive=ROOT/'missions/02-brownfield/release/final-30f8de4e/rollback-raw-csv.tar.gz'
with tarfile.open(archive) as tar:
    files={m.name:tar.extractfile(m).read() for m in tar.getmembers() if m.isfile()}
    assert len(files)==28
    def data(stage,name):
        found=[b for n,b in files.items() if n.endswith(stage+'/'+name)]
        assert len(found)==1,(stage,name);return found[0]
    stages=['before','after-v4','after-v3','reapplied']; domains=['link.csv','audit_log.csv','click.csv','user_agent_class.csv']
    c['rollback']={'archive_sha256':sha(archive.read_bytes()),'stages':{},'unchanged_hashes':{}}
    for s in stages:
        d={n:rows(data(s,n)) for n in domains+['columns.csv','constraints.csv','flyway.csv']}
        c['rollback']['stages'][s]={'rows_excluding_header':{n:len(d[n]) for n in domains},'columns':len(d['columns.csv']),'constraints':len(d['constraints.csv']),'versions':[r['version'] for r in d['flyway.csv'] if r['version']]}
        for n in domains:
            if s in ['after-v4','after-v3']: assert data('before',n)==data(s,n),n
            if s=='reapplied': assert d[n][:len(rows(data('before',n)))]==rows(data('before',n)),n
        assert data(s,'constraints.csv')==data('before','constraints.csv'),s
    for n in domains: c['rollback']['unchanged_hashes'][n]=sha(data('before',n))
    for s,versions in zip(stages,[['1','2','3','4'],['1','2','3'],['1','2'],['1','2','3','4']]): assert c['rollback']['stages'][s]['versions']==versions
    assert data('before','columns.csv')==data('reapplied','columns.csv')
    for prev,s,expected in [('before','after-v4',7),('after-v4','after-v3',8)]:
        a={(r['TABLE_NAME'],r['COLUMN_NAME']) for r in rows(data(prev,'columns.csv'))};b={(r['TABLE_NAME'],r['COLUMN_NAME']) for r in rows(data(s,'columns.csv'))};assert not b-a and len(a-b)==expected
        c['rollback']['stages'][s]['removed_columns']=sorted(a-b)
    links=rows(data('before','link.csv')); clicks=rows(data('before','click.csv'))
    c['clicks_per_link']=dict(collections.Counter(r['LINK_ID'] for r in clicks))
    probe=next(r for r in links if r['CODE']=='snUv3xWU');pr=[r for r in clicks if r['LINK_ID']==probe['ID']]
    c['stored_probe']={'clicks':len(pr),'unique_hashes':len({r['CLIENT_HASH'] for r in pr}),'classes':dict(collections.Counter(r['USER_AGENT_CLASS'] for r in pr))}
for version in [3,4]:
    migration=next(p for p in subprocess.check_output(['git','ls-tree','-r','--name-only',product,'src/main/resources/db/migration'],cwd=ROOT,text=True).splitlines() if '/V'+str(version)+'__' in p)
    sql=blob(PRE+'rollback-v'+str(version)+'.sql').decode().strip().splitlines(); original=blob(migration,product).decode()
    for line in sql:
        if line.strip() and not line.startswith('--'): assert line.strip() in original,(migration,line)
assert b'SMOKE OK' in blob(PRE+'rollback-reapplied-smoke.txt')
# Wire observations and jar request-log joins. Wrong-name empty responses remain excluded.
observed=obj(PRE+'installed-v2.json');log=[json.loads(line) for line in blob(PRE+'jar.jsonl').splitlines()]
requests={r['requestId']:r for r in log if r.get('message')=='request completed'};matches=0;outputs=[]
for r in [observed['createJar'],observed['createContainer']]+observed['observations']:
    text=r['output']; status=re.search(r'HTTP/1\.1 (\d+)',text);rid=re.search(r'X-Request-Id: ([^\r\n]+)',text)
    if not status: continue
    if rid and rid[1] in requests: assert requests[rid[1]]['status']==int(status[1]);matches+=1
    if '/stats' in r.get('command',''):
        body=json.loads(text.split('\r\n\r\n',1)[1]); assert body['totalClicks']==2 and body['clicksPerDay']==[{'date':'2026-10-04','clicks':2,'uniqueVisitors':1,'botClicks':1}]; outputs.append(body)
c['wire']={'jar_response_log_joins':matches,'stats':outputs,'request_log_status_counts':dict(collections.Counter(r['status'] for r in requests.values())),'probe':c['stored_probe']}
assert len(outputs)==2 and c['stored_probe']['clicks']==2 and c['stored_probe']['unique_hashes']==1
for name in ['jar-prometheus.txt','container-prometheus.txt']:
    t=blob(PRE+name).decode(); assert 'urlshort_clicks_recorded_total 3.0' in t
    loss=[s for s in t.splitlines() if s.startswith('urlshort_clicks_lost_total{')]; assert len(loss)==5 and all(s.endswith(' 0.0') for s in loss)
c['bench_summary']=blob(PRE+'bench.txt').decode()
result['limitations'].append('Benchmark percentiles are tool output, not independently recomputed from per-request latency samples. Database confirms 12003 stored GET clicks; per-run log counts are corroborative only.')
# Independently recompute metrics from pinned traces and transitions; capture semantic discrepancy.
m=obj('docs/metrics/metrics.json');now=ts(m['totals']['generatedAt']);paths=subprocess.check_output(['git','ls-tree','-r','--name-only',PIN,'docs/evidence'],cwd=ROOT,text=True).splitlines()
traces={};trans={}
for p in paths:
    if re.fullmatch(r'docs/evidence/[^/]+/instances/[^/]+\.trace\.json',p):
        v=obj(p);traces[v['instance']['instanceId']]=v
    if re.fullmatch(r'docs/evidence/[^/]+/packets/[^/]+\.transitions\.json',p): trans[pathlib.Path(p).name.removesuffix('.transitions.json')]=p
calc=[];human={};bad_mttr=[];event_mttr=[];metric_differences=[];first_success=[]
for row in m['instances']:
    v=traces[row['instanceId']]; trail=v.get('trail',[]); inst=v['instance'];counts=collections.Counter(t['stepId'] for t in trail if t['closureReason']!='waiting');fail=[t for t in trail if t['closureReason']=='failed'];reentry=sum(n-1 for n in counts.values());mt=[];rb=inst.get('resumeCount',0)
    for t in fail:
        later=[z for z in trail if z['stepId']==t['stepId'] and ts(z['closedAt'])>ts(t['closedAt']) and z['closureReason']!='failed']
        if later:
            fix=later[0];mt.append(ts(fix['closedAt'])-ts(t['closedAt']))
            successes=[z for z in later if z['closureReason'] in ['handoff','done']]
            if successes:
                first=min(successes,key=lambda z:z['closedAt'])
                if first['closedAt']!=fix['closedAt']: first_success.append({'instance':row['instanceId'],'step':t['stepId'],'failedAt':t['closedAt'],'toolEndpoint':fix['closedAt'],'firstSuccess':first['closedAt']})
            if fix['closureReason'] not in ['handoff','done']: bad_mttr.append({'instance':row['instanceId'],'step':t['stepId'],'failedAt':t['closedAt'],'selectedReason':fix['closureReason'],'selectedAt':fix['closedAt']})
    event_mttr.extend(mt)
    rb+=sum(bool(re.search('revert|rollback',str((t.get('closureEvidence') or {}).get('evidence_ref','')),re.I)) for t in trail)
    hw=0
    for p in row['packets']:
        history=sorted(obj(trans[p['qitemId']]),key=lambda t:t['ts']);wait=0
        for i,t in enumerate(history):
            if t['state']=='blocked' and (t.get('blockedOn')=='human@kernel' or t.get('transitionNote','').startswith('workflow gate: parked on human@kernel')):
                end=next((u for u in history[i+1:] if u['actorSession']=='human@kernel' or u['state']!='blocked'),None);wait+=(ts(end['ts']) if end else now)-ts(t['ts'])
        wait=rnd(wait);assert wait==p['humanWaitSec'];human[p['qitemId']]=wait;hw+=wait
        notes=sum(bool(re.search('revert|rollback|rolled back',t.get('transitionNote','') or '',re.I)) for t in history);
        if notes!=p['rollbackNotes']: metric_differences.append({'packet':p['qitemId'],'field':'rollbackNotes','from_raw':notes,'reported':p['rollbackNotes'],'source':trans[p['qitemId']]})
        rb+=notes
    derived={'stepClosures':len(trail),'failedClosures':len(fail),'reentries':reentry,'retries':len(fail)+reentry,'rollbacks':rb,'humanWaitSec':hw,'mttrSec':rnd(sum(mt)/len(mt)) if mt else None,'e2eLatencySec':rnd((ts(inst['completedAt']) if inst.get('completedAt') else now)-ts(inst['createdAt']))}
    for k,val in derived.items():
        if val!=row[k]: metric_differences.append({'instance':row['instanceId'],'field':k,'from_raw':val,'reported':row[k]})
    calc.append({'instance':row['instanceId'],'status':inst['status'],**derived})
for k in ['stepClosures','failedClosures','retries','rollbacks']:
    if sum(z[k] for z in calc)!=m['totals'][k]: metric_differences.append({'field':k,'from_raw':sum(z[k] for z in calc),'reported':m['totals'][k]})
if sum(human.values())!=m['totals']['humanWaitTotalSec']: metric_differences.append({'field':'humanWaitTotalSec','from_raw':sum(human.values()),'reported':m['totals']['humanWaitTotalSec']})
mt=[z['mttrSec'] for z in calc if z['mttrSec'] is not None]
if rnd(sum(mt)/len(mt))!=m['totals']['mttrMeanSec']: metric_differences.append({'field':'mttrMeanSec','from_raw':rnd(sum(mt)/len(mt)),'reported':m['totals']['mttrMeanSec']})
lat=sorted(z['e2eLatencySec'] for z in calc if z['status']=='completed')
for pct,key in [(0.5,'e2eP50Sec'),(0.95,'e2eP95Sec')]:
    if lat[math.floor(pct*(len(lat)-1))]!=m['totals'][key]: metric_differences.append({'field':key,'from_raw':lat[math.floor(pct*(len(lat)-1))],'reported':m['totals'][key]})
c['metrics']={'totals':m['totals'],'instance_rows':calc,'non_success_mttr_endpoints':bad_mttr,'event_weighted_mttr_seconds':rnd(sum(event_mttr)/len(event_mttr)),'raw_export_differences':metric_differences,'not_first_success_endpoints':first_success}
# Re-run the bounded tracked-tree signature scan with positive controls; never emit values.
scan=obj(PRE+'secret-scan.json');patterns={k:re.compile(v) for k,v in scan['patterns'].items()}
canaries={'private key':'-----BEGIN PRIVATE KEY-----','AWS access key':'AKIA'+'A'*16,'GitHub token':'ghp_'+'A'*36}
for k,pat in patterns.items(): assert pat.search(canaries[k]),k
text_count=0;binary_count=0;findings=[]
archive_data=subprocess.check_output(['git','archive',product],cwd=ROOT)
with tarfile.open(fileobj=io.BytesIO(archive_data)) as tar:
    for item in tar:
        if not item.isfile(): continue
        data_bytes=tar.extractfile(item).read()
        try:
            content=data_bytes.decode('utf-8')
            if '\x00' in content: raise UnicodeError('NUL byte')
        except UnicodeError:
            binary_count+=1;continue
        text_count+=1
        for kind,pat in patterns.items():
            if pat.search(content): findings.append({'path':item.name,'kind':kind})
c['secret_scan']={'text_blobs':text_count,'binary_blobs_not_scanned':binary_count,'positive_controls':list(patterns),'findings':findings}
assert not findings
assert text_count==scan['textFilesChecked'] and binary_count==len(scan['binaryFilesNotScanned'])
a=obj(PRE+'advisories.json');assert len(a['dependencies'])==len(a['raw']['results'])==97 and all(not r for r in a['raw']['results']) and not a['findings']
c['historical_advisory_response']={'queriedAt':a['queriedAt'],'queries':97,'empty_results':97,'network_requery':False}
# Governance drills: compare durable transitions and candidate contents, not the prose verdict.
D='docs/evidence/02-brownfield/drills/'
def drill(name): return obj(D+name)
initial=drill('drill1/initial/drill1-candidate.json'); corrected=drill('drill1/corrected/drill1-candidate.json')
assert initial['status']=='BROKEN' and corrected['status']=='READY' and initial['drill']==corrected['drill']=='DRILL 1'
d1=drill('drill1/instances/01M4212A8BKA6JRZHZQBD90D07.trace.json')
assert d1['instance']['status']=='completed' and d1['instance']['hopCount']==3 and not d1['instance']['currentFrontier']
trail=sorted(d1['trail'],key=lambda t:t['closedAt'])
assert [(t['stepId'],t['closureReason']) for t in trail]==[('implement','handoff'),('qa_check','failed'),('implement','handoff'),('qa_check','done')]
for t in trail:
    history=drill('drill1/packets/'+t['priorQitemId']+'.transitions.json')
    closed=[h for h in history if h['state'] in ['handed-off','done']]
    assert len(closed)==1 and 0<=ts(closed[0]['ts'])-ts(t['closedAt'])<1
    assert closed[0]['actorSession']==('qa-agent@urlshort-factory' if t['stepId']=='qa_check' else 'release-agent@urlshort-factory')
auth=next(t for t in drill('drill1/tracking-item.transitions.json') if t['transitionId']==1602)
assert auth['actorSession']=='orchestration-lead@urlshort-factory'
assert b'SMOKE FAIL: redirect has no Cache-Control: no-store' in blob(D+'drill2-smoke-failed-3a75746.txt')
assert b'SMOKE OK' in blob(D+'drill2-smoke-passed-ef2272e.txt') and b'BUILD SUCCESSFUL' in blob(D+'drill2-check-ef2272e.txt')
assert subprocess.check_output(['git','diff','ad83fb6','ef2272e'],cwd=ROOT)==b''
stopped=next(r for r in drill('drill3-ps-after-stop.json') if r['logicalId']=='requirements.agent')
restored=next(r for r in drill('drill3-ps-after-relaunch.json') if r['logicalId']=='requirements.agent')
assert stopped['sessionStatus']=='exited' and stopped['lifecycleState']=='recoverable'
assert restored['sessionStatus']=='running' and restored['startupStatus']=='ready'
stranded=drill('drill3-entry-packet-stranded.json');assert stranded['state']=='pending' and stranded['claimedAt'] is None
route=next(t for t in drill('drill3-old-packet-transitions.json') if t['state']=='handed-off')
assert route['transitionId']==986 and route['actorSession']=='orchestration-lead@urlshort-factory'
d3=drill('drill3-instance-trace.json');assert d3['instance']['hopCount']==0 and not d3['trail']
assert d3['instance']['currentFrontier']==['qitem-20261003165532-4a59045f']
d4=drill('drill4-instance-show.json');assert d4['status']=='aborted' and d4['resumeCount']==1 and not d4['currentFrontier']
assert any(t['state']=='canceled' for t in drill('drill4-redrive-packet-transitions.json'))
lead=next(t for t in drill('drill4-lead-receipt.json') if t['state']=='done')
assert lead['actorSession']=='orchestration-lead@urlshort-factory' and 'not in the failed state' in lead['transitionNote']
c['drills']={'drill1':{'closures':[(t['stepId'],t['closureReason'],t['closedAt']) for t in trail],'authorization':1602},'drill2':{'reverted_tree_identical_to':'ad83fb6','revert':'ef2272e','failed_and_passed_smoke_logs':True},'drill3':{'stranded_unclaimed':True,'route':986,'zero_step_advance':True,'restored_ready':True},'drill4':{'aborted':True,'resume_count':1,'redrive_canceled':True,'aborted_resume_rejection':'lead-attributed receipt 1006'}}
# Local evidence links at reviewed document pins. URLs are deliberately not re-queried.
checked=[];broken=[];anchors=[]
for path,pin in [('missions/02-brownfield/RELEASE.md',PIN),('docs/scenarios/drills.md','e425469e'),('README.md','30f8de4e')]:
    text=blob(path,pin).decode()
    for href in re.findall(r'\[[^\]]*\]\(([^)]+)\)',text):
        if re.match(r'(?:https?|mailto):',href): continue
        target,sep,anchor=href.partition('#'); resolved=(ROOT/path).parent/target if target else ROOT/path
        resolved=resolved.resolve(); checked.append({'from':path,'target':str(resolved.relative_to(ROOT)),'anchor':anchor})
        if not resolved.exists(): broken.append(checked[-1]);continue
        if anchor and resolved.suffix=='.md':
            headings=[]
            for line in resolved.read_text().splitlines():
                if line.startswith('#'):
                    heading=re.sub(r'[^\w\s-]','',line.lstrip('#').strip().lower());headings.append(re.sub(r'\s','-',heading))
            if anchor not in headings and ('id="'+anchor+'"') not in resolved.read_text(): anchors.append(checked[-1])
assert not broken,broken
c['links']={'count':len(checked),'missing_files':broken,'unmatched_anchors':anchors,'targets':checked}
result['limitations'] += ['The pre-run image inspect describes image metadata, not a retained container HostConfig inspection. Read-only root and host publishing need the final container inspection.', 'The pre-run wire JSON supports stats/counters; it does not retain full invalid-URL and forged-audit responses or container JSON log joins. Jar statuses corroborate 400/403 but cannot alone prove body hygiene. Final installed captures must supply that proof.', 'Metrics differ from the committed historical export: retain the original with a source-custody qualification, and verify the refreshed package against its own committed inputs.']
# Pin input manifest, including drills; changed final release inputs are reviewed separately.
c['historical_input_hashes']={p:sha(b) for (rev,p),b in cache.items() if rev==PIN}
OUT.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'historical_inputs':len(c['historical_input_hashes']),'source_rows':inventories,'coverage':counters,'rollback_stages':len(stages),'wire_log_joins':matches,'secret_scan':c['secret_scan'],'drills_checked':list(c['drills']),'local_links':len(checked),'missing_files':broken,'unmatched_anchors':anchors,'metric_differences':metric_differences,'output':str(OUT)},indent=2))
