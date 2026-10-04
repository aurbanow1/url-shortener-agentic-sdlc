from pathlib import Path
import json, hashlib, re, collections, tarfile
D=Path(__file__).resolve().parent
def read(name): return json.loads((D/name).read_text())
ledger=read('http-ledger.json')
checks=read('checks.json')
settings=read('runtime-settings.json')
result={'httpResponses':len(ledger),'checks':len(checks),'failures':[x for x in checks if not x['pass']],'jarDigest':hashlib.sha256(Path(settings['installedJar']).read_bytes()).hexdigest(),'statusCounts':dict(collections.Counter(str(x['status']) for x in ledger))}
assert not result['failures'], result['failures']
assert result['jarDigest']==settings['sha256']
head=[json.loads(p.read_text()) for p in (D/'http').glob('*.wire.json')]
assert {p.stem for p in (D/'http').glob('*.headers')}=={x['name'] for x in ledger}
assert {p.stem for p in (D/'http').glob('*.body')}=={x['name'] for x in ledger}
assert len(head)==4
assert all(x['bodyBytes']==0 and x['status'] in (200,302) for x in head)
result['headWireResponses']=len(head)
canaries=['m03-ua-canary','m03-path-canary','m03-query-canary','m03-frag-canary','m03-user-canary','m03-password-canary','m03-cursor-canary','m03-invalid-canary','inbound-requestid-m03-private','203.0.113.7','203.0.113.8','203.0.113.9','198.51.100.44','192.0.2.20','192.0.2.21']
byPort={18232:'default',18233:'trusted'}
ids=[]
correlations={}
for port,mode in byPort.items():
 subset=[x for x in ledger if int(x['url'].split(':')[2].split('/')[0])==port]
 heads=[x for x in head if int(x['url'].split(':')[2].split('/')[0])==port]
 for sink in ('console','file'):
  text=(D/(mode+'-'+sink+'.jsonl')).read_text()
  events=[json.loads(l) for l in text.splitlines() if l.startswith('{')]
  assert not any(c in text for c in canaries),(mode,sink,'canary')
  done=collections.defaultdict(list)
  for e in events:
   if e.get('message')=='request completed': done[e['requestId']].append(e)
  for x in subset:
   rid=x['headerMap'].get('x-request-id','')
   assert re.fullmatch(r'[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}',rid),x['name']
   assert len(done[rid])==1 and done[rid][0]['status']==x['status'],(x['name'],sink,done[rid])
  for x in heads:
   assert len(done[x['requestId']])==1 and done[x['requestId']][0]['status']==x['status']
  assert sum(map(len,done.values()))==len(subset)+len(heads),(mode,sink,'unaccounted response')
  correlations[mode+'-'+sink]={'responsesMatched':len(subset)+len(heads),'completedEvents':sum(map(len,done.values())),'canariesPresent':[]}
 ids.extend(x['headerMap']['x-request-id'] for x in subset)
 ids.extend(x['requestId'] for x in heads)
assert len(set(ids))==len(ids)
result['logCorrelation']=correlations
for x in ledger:
 hdr=(D/'http'/(x['name']+'.headers')).read_bytes().decode()
 body=(D/'http'/(x['name']+'.body')).read_bytes().decode()
 normalize_header=lambda v:'\n'.join(l.rstrip() for l in v.splitlines()).rstrip()+'\n'
 assert normalize_header(hdr)==normalize_header(x['headers']),x['name']
 if '/actuator/prometheus' in x['url']:
  assert normalize_header(body)==normalize_header(x['body']),x['name']
 else:
  assert body==x['body'],x['name']
 if x['status']>=400:
  assert x['json']['status']==x['status'],x['name']
  assert x['headerMap']['content-type'].startswith('application/problem+json'),x['name']
  assert x['json']['instance']=='urn:uuid:'+x['headerMap']['x-request-id'],x['name']
  assert not any(c in body for c in canaries),x['name']
 if x['name'].endswith('analyst') or '/stats' in x['url']:
  if x['status']==200 and 'json' in x:
   v=x['json']
   assert set(v)=={'code','totalClicks','clicksPerDay','topReferrers'}
   assert v['totalClicks']==sum(y['clicks'] for y in v['clicksPerDay'])
   assert all(set(y)=={'date','clicks','uniqueVisitors','botClicks'} for y in v['clicksPerDay'])
   assert not any(c in body for c in canaries)
result['responsesWithUniqueServerRequestId']=len(ids)
manifest=read('raw-manifest.json')
archive=D/'raw-captures.tar.gz'
assert hashlib.sha256(archive.read_bytes()).hexdigest()==manifest['archiveSha256']
with tarfile.open(archive,'r:gz') as tar:
 members={m.name:m for m in tar.getmembers() if m.isfile()}
 assert set(members)==set(manifest['files'])
 for name,digest in manifest['files'].items():
  raw=tar.extractfile(members[name]).read()
  assert hashlib.sha256(raw).hexdigest()==digest,name
  if name.endswith('.headers'):
   wire=next(x for x in ledger if x['name']==Path(name).stem)
   assert raw.decode().replace('\r\n','\n')==wire['headers'].replace('\r\n','\n'),name
  display=(D/name).read_bytes()
  is_scrape=name.endswith('.body') and '/actuator/prometheus' in next(x['url'] for x in ledger if x['name']==Path(name).stem)
  if name.endswith('.body'):
   wire=next(x for x in ledger if x['name']==Path(name).stem)
   assert raw.decode()==wire['body'],name
  if (name.endswith('.body') and not is_scrape) or name.endswith('.request') or name.endswith('.wire.json'):
   assert display==raw,name
  else:
   norm=lambda v:'\n'.join(l.rstrip() for l in v.decode().splitlines()).rstrip()+'\n'
   assert norm(display)==norm(raw),name
result['rawArchive']={'sha256':manifest['archiveSha256'],'membersVerified':len(manifest['files'])}
(D/'verification-summary.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
