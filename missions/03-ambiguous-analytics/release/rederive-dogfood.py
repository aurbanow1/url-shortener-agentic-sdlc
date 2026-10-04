"""Re-derive the release-relevant claims in 8cf894a from preserved raw bytes."""
from pathlib import Path
import collections, datetime, hashlib, json, re, tarfile

repo = Path(__file__).resolve().parents[3]
d = repo / 'docs/qa/dogfood/03-ambiguous-analytics/50ad9c3'
ledger = json.loads((d/'http-ledger.json').read_text())
manifest = json.loads((d/'raw-manifest.json').read_text())
assert hashlib.sha256((d/'raw-captures.tar.gz').read_bytes()).hexdigest() == manifest['archiveSha256']
with tarfile.open(d/'raw-captures.tar.gz') as tar:
    raw = {m.name: tar.extractfile(m).read() for m in tar.getmembers() if m.isfile()}
assert set(raw) == set(manifest['files'])
assert all(hashlib.sha256(v).hexdigest() == manifest['files'][k] for k,v in raw.items())
responses = {}
ids = set()
for x in ledger:
    name = x['name']
    h = raw['http/'+name+'.headers'].decode()
    b = raw['http/'+name+'.body'].decode()
    status = int(re.match(r'HTTP/\S+ (\d+)', h)[1])
    assert status == x['status'] and b == x['body']
    headers = {k.lower():v.strip() for k,v in (l.split(':',1) for l in h.splitlines()[1:] if ':' in l)}
    rid = headers['x-request-id']; assert rid not in ids; ids.add(rid)
    responses[name] = {'status':status,'headers':headers,'body':b,'json':json.loads(b) if b.startswith('{') else None}
    if status >= 400:
        assert headers['content-type'].startswith('application/problem+json')
        assert responses[name]['json']['status'] == status
        assert responses[name]['json']['instance'] == 'urn:uuid:'+rid

def j(name): return responses[name]['json']
def triplet(name):
    v=j(name); day=v['clicksPerDay'][0]
    assert set(v)=={'code','totalClicks','clicksPerDay','topReferrers'}
    assert set(day)=={'date','clicks','uniqueVisitors','botClicks'} and day['date']=='2026-10-04'
    assert v['totalClicks']==day['clicks']
    return day['clicks'],day['uniqueVisitors'],day['botClicks']

assert triplet('default-stats-mixed')==(7,1,3)
assert triplet('trusted-stats-mixed')==(7,4,3)
assert triplet('trusted-stats-chains')==(12,5,3)
assert triplet('trusted-after-concurrent')==(112,15,23)
assert triplet('restart-before-campaign')==(142,17,33)
assert triplet('restart-same-client-stats')==(143,18,33)
assert triplet('trusted-final-stats')==(145,18,34)
assert len([n for n in responses if n.startswith('concurrent-')])==100
assert all(v['status']==302 and v['body']=='' and v['headers']['cache-control']=='no-store' for n,v in responses.items() if n.startswith('concurrent-'))
for mode in ('default','trusted'):
    assert j(mode+'-create')['code']==j(mode+'-replay')['code']
    assert responses[mode+'-mismatch']['status']==422
    assert j(mode+'-stats-empty')['totalClicks']==0 and j(mode+'-stats-empty')['clicksPerDay']==[]
    for suffix in ('xff','forwarded'):
        assert responses[mode+'-audit-spoof-'+suffix]['status']==403
for suffix in ('campaign','ranking','audit','link'):
    assert j('restart-before-'+suffix)==j('restart-after-'+suffix)
assert j('restart-before-recorded')['measurements'][0]['value']==157
assert j('restart-after-recorded')['measurements'][0]['value']==0
assert j('trusted-final-recorded')['measurements'][0]['value']==3
assert j('trusted-final-lost')['measurements'][0]['value']==0
ranking=j('trusted-ranking-stats')['topReferrers']
assert ranking==[{'referrer':f'https://ref{i:02d}.example.org','clicks':3 if i==0 else 2 if i==1 else 1} for i in range(10)]
limited=[v for n,v in responses.items() if n.startswith('limit-api-') and n!='limit-api-recovered']
assert collections.Counter(v['status'] for v in limited)=={200:60,429:15}
assert all(int(v['headers']['retry-after'])>0 for v in limited if v['status']==429)
assert responses['limit-api-recovered']['status']==200
assert responses['limit-different-forwarded-client']['status']==200
assert responses['default-retire']['status']==204 and responses['default-retired-visitor']['status']==410
assert j('default-retired-stats-json')['totalClicks']==7
heads=[json.loads(v) for k,v in raw.items() if k.endswith('.wire.json')]
assert len(heads)==4 and all(v['bodyBytes']==0 for v in heads)
logs={}
canaries=['m03-ua-canary','m03-path-canary','m03-query-canary','m03-frag-canary','m03-user-canary','m03-password-canary','m03-cursor-canary','m03-invalid-canary','inbound-requestid-m03-private','203.0.113.7','203.0.113.8','203.0.113.9','198.51.100.44','192.0.2.20','192.0.2.21']
for mode,port in [('default',18232),('trusted',18233)]:
    expected={x['headerMap']['x-request-id']:x['status'] for x in ledger if ':'+str(port)+'/' in x['url']}
    expected.update({x['requestId']:x['status'] for x in heads if ':'+str(port)+'/' in x['url']})
    for sink in ('console','file'):
        text=(d/(mode+'-'+sink+'.jsonl')).read_text();assert not any(c in text for c in canaries)
        events=[json.loads(l) for l in text.splitlines() if l.startswith('{')]
        done=[e for e in events if e.get('message')=='request completed']
        assert len(done)==len(expected) and {e['requestId']:e['status'] for e in done}==expected
        logs[mode+'-'+sink]=len(done)
assert hashlib.sha256(Path('/private/tmp/urlshort-mission03-50ad9c3.jar').read_bytes()).hexdigest()=='fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2'
out={'auditedCommit':'8cf894ab1db83d1476e11c8e424cb929f7ef518e','author':'qa-agent@urlshort-factory','auditor':'release2-agent@urlshort-factory','auditedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'modelWindow':'2026-10-04 01:27–01:33Z, Luna Reserve, lead record 4891097','result':'PASS for listed raw-derived installed claims; no correction required','rawMembersHashed':len(raw),'curlResponsesDerived':len(responses),'headResponses':len(heads),'requestLogMatches':logs,'scope':'API/error/aggregate/concurrent/limiter/restart claims above; no proof12, salt memory, induced failures, natural midnight, or hosted-run judgment'}
(Path(__file__).parent/'dogfood-rederivation-8cf894a.json').write_text(json.dumps(out,indent=2)+'\n')
print(json.dumps(out,indent=2))
