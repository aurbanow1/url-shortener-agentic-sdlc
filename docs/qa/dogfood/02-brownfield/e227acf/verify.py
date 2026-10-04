from pathlib import Path
import json,hashlib,collections,re,tarfile,datetime,subprocess
D=Path(__file__).resolve().parent
def read(n): return json.loads((D/n).read_text())
def norm(b): return '\n'.join(x.rstrip() for x in b.decode().splitlines()).rstrip()+'\n'
equiv=read('jar-equivalence.json')
a=Path(equiv['mission02Path']).read_bytes()
b=Path(equiv['mission03Path']).read_bytes()
assert a==b
digest=hashlib.sha256(a).hexdigest()
assert digest==equiv['mission02Sha256']==equiv['mission03Sha256']=='fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2'
assert read('pinned-openapi.json')==read('live-openapi.json')
runbook=subprocess.check_output(['git','show',equiv['mission02']+':docs/RUNBOOK.md'])
assert (D/'RUNBOOK-e227acf.md').read_bytes()==runbook
ledger=read('http-ledger.json')
checks=read('checks.json')
assert len(ledger)==30 and len(checks)==32
assert all(c['pass'] for c in checks),[c for c in checks if not c['pass']]
assert {p.stem for p in (D/'http').glob('*.headers')}=={x['name'] for x in ledger}
assert {p.stem for p in (D/'http').glob('*.body')}=={x['name'] for x in ledger}
forbidden=['m02-ua-canary','m02-url-canary','m02-key-canary','m02-inbound-canary','m02-cursor-canary','m02-code-canary','m02-refpath-canary','m02-refquery-canary','m02-reffrag-canary','127.0.0.2']
correlation={}
ids=[]
for sink in ('console','file'):
 text=(D/(sink+'.jsonl')).read_text()
 assert not any(x in text for x in forbidden),sink
 events=[json.loads(l) for l in text.splitlines() if l.startswith('{')]
 done=collections.defaultdict(list)
 for e in events:
  if e.get('message')=='request completed': done[e['requestId']].append(e)
 assert sum(map(len,done.values()))==len(ledger)
 for x in ledger:
  rid=x['headerMap']['x-request-id']
  assert re.fullmatch(r'[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}',rid)
  assert len(done[rid])==1 and done[rid][0]['status']==x['status'],x['name']
 correlation[sink]={'responses':len(ledger),'matched':len(ledger),'canariesPresent':[]}
 ids=[x['headerMap']['x-request-id'] for x in ledger]
assert len(set(ids))==len(ids)
for x in ledger:
 assert norm((D/'http'/(x['name']+'.headers')).read_bytes())==norm(x['headers'].encode())
 body=(D/'http'/(x['name']+'.body')).read_bytes()
 assert norm(body)==norm(x['body'].encode()) if x['name']=='scrape' else body.decode()==x['body']
 if x['status']>=400:
  assert x['json']['status']==x['status']
  assert x['headerMap']['content-type'].startswith('application/problem+json')
  assert x['json']['instance']=='urn:uuid:'+x['headerMap']['x-request-id']
  assert not any(c in x['body'] for c in forbidden)
manifest=read('raw-manifest.json')
archive=D/'raw-captures.tar.gz'
assert hashlib.sha256(archive.read_bytes()).hexdigest()==manifest['archiveSha256']
with tarfile.open(archive,'r:gz') as tar:
 members={m.name:m for m in tar.getmembers() if m.isfile()}
 assert set(members)==set(manifest['files'])
 for name,digest0 in manifest['files'].items():
  raw=tar.extractfile(members[name]).read()
  assert hashlib.sha256(raw).hexdigest()==digest0,name
  display=(D/name).read_bytes()
  if name.endswith('.headers'):
   x=next(x for x in ledger if x['name']==Path(name).stem)
   assert raw.decode().replace('\r\n','\n')==x['headers']
  if name.endswith('.body'):
   x=next(x for x in ledger if x['name']==Path(name).stem)
   assert raw.decode()==x['body']
  if name.endswith('.request') or (name.endswith('.body') and name!='http/scrape.body'):
   assert display==raw,name
  else:
   assert norm(display)==norm(raw),name
purge=read('purge-events.json')
assert len(purge)==2
paused,active=purge
assert paused['message']=='click purge paused, no click is deleted' and paused['log']['level']=='WARN' and paused['retentionDays']==7
assert active['message']=='clicks purged' and active['log']['level']=='INFO' and active['retentionDays']==7 and active['deleted']==0
cutoff=datetime.datetime.fromisoformat(active['@timestamp'].replace('Z','+00:00')).date()-datetime.timedelta(days=7)
assert active['cutoff']==cutoff.isoformat()
assert all('requestId' not in p for p in purge)
bad=(D/'invalid-retention-console.txt').read_text()
assert 'urlshort.click.retention-days' in bad and 'must be greater than 0' in bad and 'Value: \\"0\\"' in bad
assert all(p['exitCode']==7 for p in read('shutdown.json')['ports'])
result={'verifiedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'candidate':equiv['mission02'],'jarSha256':digest,'byteIdenticalToMission03':True,'newHttpResponses':len(ledger),'newChecks':len(checks),'newFailures':[],'correlation':correlation,'pinnedApiMatchesLive':True,'runbookPinnedSha256':hashlib.sha256(runbook).hexdigest(),'retentionHoldAndReleaseEvents':2,'invalidZeroRefused':True,'rawMembers':len(manifest['files']),'rawArchiveSha256':manifest['archiveSha256'],'stoppedPorts':[18234,18235]}
(D/'verification-summary.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
