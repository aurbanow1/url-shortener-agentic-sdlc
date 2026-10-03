from pathlib import Path
import subprocess, re, json, hashlib, collections, datetime

root=Path('/Users/andrzej/Documents/projekty/test/openrig/url-shortener')
out=root/'docs/qa/01-greenfield-core/release-judgments'
out.mkdir(parents=True,exist_ok=True)
def git(*args):
    r=subprocess.run(['git',*args],cwd=root,capture_output=True)
    assert r.returncode in (0,1), (args,r.returncode,r.stderr.decode())
    return r
def blob(ref,path):
    r=git('show',ref+':'+path)
    assert r.returncode==0
    return r.stdout
merged='8e9c065589e53385f60d6be3ddbc3683260285df'
built=git('rev-parse','f090103').stdout.decode().strip()
head=git('rev-parse','HEAD').stdout.decode().strip()
product=['src','build.gradle.kts','settings.gradle.kts','gradle.properties','Dockerfile','compose.yaml','docs/api/openapi.json']
r=git('diff','--exit-code',merged,built,'--',*product)
assert r.returncode==0 and not r.stdout
assert git('merge-base','--is-ancestor','3ec7ab4',head).returncode==0

# Repeat the release method independently at the built commit; no matching value is written.
signature=r'-----BEGIN (RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-[0-9A-Za-z-]{20,}|sk-(ant-)?[0-9A-Za-z_-]{20,}|AIza[0-9A-Za-z_-]{35}|eyJ[0-9A-Za-z_-]{10,}[.][0-9A-Za-z_-]{10,}[.][0-9A-Za-z_-]{10,}'
scan=git('grep','-n','-E','-e',signature,built,'--','.')
assert scan.returncode==1 and not scan.stdout, 'Credential signature hit; inspect privately before verdict'
assignment=r'''(password|passwd|secret|token|api_key|credential)[[:space:]"']*[:=][[:space:]"']*[^[:space:]"'=]{6,}'''
scope=['src','build.gradle.kts','settings.gradle.kts','gradle.properties','Dockerfile','compose.yaml','scripts','tools','.claude','.codex','rig/rig.yaml','project.yaml']
scan2=git('grep','-n','-i','-E','-e',assignment,built,'--',*scope)
hits=[]
for line in scan2.stdout.decode().splitlines():
    match=re.match(r'[^:]+:(.*?):([0-9]+):(.*)',line)
    assert match, line[:80]
    path,num,content=match.groups()
    if re.search(r'firstString\(|process\.env\.|env\.OPENRIG_',content): kind='variable/environment read'
    elif 'token = parsed.token' in content: kind='variable read from parsed local endpoint metadata'
    elif 'transcriptTokens' in content and 'Math.round(' in content: kind='transcript token count computation'
    else: kind='REQUIRES PRIVATE INSPECTION'
    hits.append({'file':path,'line':int(num),'classification':kind})
assert all(h['classification']!='REQUIRES PRIVATE INSPECTION' for h in hits), hits
tracked=git('ls-tree','-r','--name-only',built).stdout.decode().splitlines()
assert not any(p=='.env' or p.endswith('/.env') for p in tracked)
props=blob(built,'src/main/resources/application.properties').decode()
assert re.search(r'^spring\.datasource\.password=\s*$',props,re.M)
assert 'urlshort.public-base-url=http://localhost:8080' in props

release=root/'missions/01-greenfield-core/release'
bench=(release/'bench-jar-f090103.txt').read_text()
expected=[('create POST',1200,20.0,2.8),('redirect GET /{code}:',6000,100.0,2.2),('redirect GET /{code} alone:',6000,100.0,1.7),('redirect HEAD /{code} alone:',6000,100.0,1.8)]
for prefix,n,rate,p95 in expected:
    line=next(x for x in bench.splitlines() if x.startswith(prefix))
    assert f'{n} requests' in line and f'achieved {rate:.1f} req/s' in line
    assert 'non-2xx/3xx 0' in line and f'p95 {p95} ms' in line
stats=json.loads((release/'bench-stats-7YBO6Hrx-f090103.json').read_text())
assert stats['code']=='7YBO6Hrx' and stats['totalClicks']==12000
assert sum(d['clicks'] for d in stats['clicksPerDay'])==12000
events=[json.loads(x) for x in (release/'jar-bench-log-f090103.jsonl').read_text().splitlines() if x.strip()]
levels=collections.Counter(e.get('log',{}).get('level') for e in events)
assert levels['ERROR']==0 and levels['WARN']==3
messages=collections.Counter(e.get('message') for e in events)
completions=[e for e in events if e.get('message')=='request completed']
completion_status=collections.Counter(str(e.get('status')) for e in completions)
assert completion_status=={'200':4,'201':1201,'302':18000}
warn_messages=[e['message'] for e in events if e.get('log',{}).get('level')=='WARN']
assert all('Flyway' in m or 'springdoc' in m for m in warn_messages), warn_messages

env=(release/'smoke-jar-ac26-f090103.txt').read_text()
assert 'SMOKE JAR OK' in env and 'https://sho.rt.example/dpMkbHzk' in env
assert '10 admitted, then 429' in env
env_events=[json.loads(x) for x in (release/'jar-ac26-log-f090103.jsonl').read_text().splitlines() if x.strip()]
env_text=json.dumps(env_events)
assert all(x not in env_text for x in ['192.0.2.10','alert(','referrer.example','smoke-journey'])

# Compare the entire analytics table rather than selecting only favorable rows.
trace=(root/'docs/qa/TRACEABILITY.md').read_text()
oldtrace=blob('5b2cdb2','docs/qa/TRACEABILITY.md').decode()
marker='## 02-analytics — candidate 5b3490c65915cf42594a4720350950bcefd2d7d0'
def section(s): return s.split(marker,1)[1].split('\n## ',1)[0].rstrip()
current_section=section(trace)
assert current_section==section(oldtrace)
rows=[l for l in current_section.splitlines() if l.startswith('|') and '#' in l and '| PASS ' in l]
assert len(rows)==138
assert collections.Counter(l.split('|')[3].strip() for l in rows)=={'unit':69,'functional':69}
functional=[l for l in rows if '| functional |' in l]
assert all(any(re.search(r'(?<!\d)AC-'+str(n)+r'(?!\d)',l) for l in functional) for n in range(1,23))
assert all(any(re.search(r'BR-'+str(n)+r'(?!\d)',l) for l in rows) for n in range(1,10))
assert all('PASS on 5b3490c' in l for l in rows)
index=(root/'docs/DESIGN.md').read_text()
adrs=['docs/adr/0011-click-handoff-bounded-single-writer.md','docs/adr/0012-client-hash-daily-salt.md','docs/adr/0013-click-events-and-request-time-statistics.md']
for path in adrs:
    assert blob('5b2cdb2',path)==(root/path).read_bytes()
    assert path.removeprefix('docs/') in index
    assert int(git('show','-s','--format=%ct','4cfb745').stdout) < int(git('show','-s','--format=%ct','2499505').stdout)
oldindex=blob('4cfb745','docs/DESIGN.md').decode()
assert all(p.removeprefix('docs/') in oldindex for p in adrs)
oldgaps=blob('5b2cdb2','docs/qa/GAPS.md').decode()
gaps=(root/'docs/qa/GAPS.md').read_text()
assert all(l in gaps for l in oldgaps.splitlines() if l.startswith('| 02-analytics'))

paths=['missions/01-greenfield-core/RELEASE.md','docs/qa/GAPS.md','docs/qa/TRACEABILITY.md','docs/DESIGN.md','tools/bench.mjs','scripts/smoke.sh',
       'docs/review/01-create-redirect/01-code-review.md','docs/review/01-create-redirect/02-security-review.md',
       'missions/01-greenfield-core/release/bench-jar-f090103.txt','missions/01-greenfield-core/release/bench-jar-f090103-discarded-run1.txt',
       'missions/01-greenfield-core/release/bench-stats-7YBO6Hrx-f090103.json','missions/01-greenfield-core/release/jar-bench-log-f090103.jsonl',
       'missions/01-greenfield-core/release/bench-prometheus-f090103.txt','missions/01-greenfield-core/release/smoke-jar-ac26-f090103.txt',
       'missions/01-greenfield-core/release/jar-ac26-log-f090103.jsonl','missions/01-greenfield-core/release/check-bootjar-f090103.txt',*adrs]
record={'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'read_at_commit':head,'merged_subject':merged,'built_commit':built,
        'product_input_comparison':'empty diff for '+', '.join(product),'after_3ec7ab4':True,
        'secret_scan':{'signature_regex':signature,'assignment_regex':assignment,'scope':scope,'signature_git_exit':scan.returncode,
                       'signature_hits':0,'assignment_hits':hits,'tracked_paths':len(tracked),'tracked_dotenv':False,
                       'empty_h2_password':True,'limitations':'Tracked commit contents and stated signatures/assignments only; no universal secret absence, full history, untracked files, binary image or dedicated scanner claim.'},
        'bench':{'scenarios':expected,'stats_clicks':12000,'json_events':len(events),'levels':dict(levels),
                 'completion_messages':len(completions),'completion_status':dict(completion_status),
                 'warn_messages':warn_messages,
                 'limitation':'GET and HEAD are sequential 60-second proxy runs, not an isolated counterfactual click-on/off comparison. Difference of independent p95s is not an isolated added-cost quantile; no causal or capacity guarantee.'},
        'env_smoke':{'events':len(env_events),'shortUrl':'https://sho.rt.example/dpMkbHzk','reported_full_smoke':True,'searched_canaries_absent':True},
        'reaffirmations':{'analytics_table_unchanged':True,'mapped_methods':138,'unit_methods':69,'functional_methods':69,'all_22_acs_and_9_rules':True,
                          'analytics_old_gap_rows_retained':True,'adrs_unchanged':True,'adr_index_in_4cfb745_before_product_2499505':True},
        'hashes':{p:hashlib.sha256((root/p).read_bytes()).hexdigest() for p in paths}}
(out/'audit-8e9c065.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({k:v for k,v in record.items() if k!='hashes'},indent=2))
