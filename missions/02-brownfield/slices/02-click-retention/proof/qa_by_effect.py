"""Independent QA effects driver; localhost only, disposable directories, unchanged product."""
import datetime as dt
import hashlib
import json
import os
import pathlib
import shutil
import signal
import subprocess
import sys
import time
import traceback
import tempfile

ROOT = pathlib.Path('/Users/andrzej/Documents/projekty/test/openrig/url-shortener')
PROOF = ROOT / 'missions/02-brownfield/slices/02-click-retention/proof'
OUT = PROOF / 'qa-effects-a8fc8b6'
if '--all' in sys.argv: OUT = PROOF / 'qa-effects-final-a8fc8b6'
TMP = pathlib.Path('/private/tmp/urlshort-qa2-retention-96nqv4hx')
JAVA = '/opt/homebrew/opt/openjdk@21/bin/java'
CP = (TMP / 'classpath.txt').read_text()
JAR = ROOT / '.worktrees/02-click-retention/build/libs/urlshort.jar'
BASE_JAR = TMP / 'baseline/build/libs/urlshort.jar'
RUN = pathlib.Path(tempfile.mkdtemp(prefix='effects-', dir=TMP))
OUT.mkdir(exist_ok=True)
PROCESSES = []
EVIDENCE = {'candidate': 'a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1', 'observations': [], 'checks': []}

def save(name, value):
    (OUT / name).write_text(json.dumps(value, indent=2, default=str) + '\n')

def observed(name, **data):
    EVIDENCE['observations'].append({'name': name, **data})
    save('effects.json', EVIDENCE)

def check(name, predicate):
    EVIDENCE['checks'].append({'name': name, 'passed': bool(predicate)})
    save('effects.json', EVIDENCE)
    if not predicate:
        raise AssertionError(name)
    print('PASS', name, flush=True)

def jdbc_url(directory):
    return 'jdbc:h2:file:' + str(directory / 'urlshort') + ';MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000'

def start(name, port, directory, jar=None, instant='2026-12-15T12:00:00Z', env=None, normal_budgets=False):
    control = RUN / (name + '-control')
    control.mkdir(exist_ok=True)
    (control / 'ready').unlink(missing_ok=True)
    log = OUT / (name + '.jsonl')
    options = ['--server.address=127.0.0.1', '--server.port=' + str(port), '--spring.datasource.url=' + jdbc_url(directory)]
    # Each shifted-clock group uses a different peer; trust applies only to this loopback fixture.
    options += ['--urlshort.rate-limit.trusted-proxies=127.0.0.1']
    if not jar and not normal_budgets: options += ['--urlshort.rate-limit.create-per-minute=100000', '--urlshort.rate-limit.redirect-per-minute=100000']
    argv = [JAVA, '-jar', str(jar)] + options if jar else [JAVA, '-Dqa.dir=' + str(control), '-Dqa.instant=' + instant, '-cp', CP, 'dev.urlshort.click.QaRetentionRunner'] + options
    fh = log.open('w')
    proc = subprocess.Popen(argv, cwd=ROOT, stdout=fh, stderr=subprocess.STDOUT, env={**os.environ, **(env or {})})
    info = {'name': name, 'proc': proc, 'log': log, 'control': control, 'fh': fh, 'port': port, 'argv': argv, 'env': env or {}, 'directory': directory}
    PROCESSES.append(info)
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            raise RuntimeError(name + ' stopped during startup: ' + log.read_text()[-4000:])
        p = subprocess.run([str(ROOT / 'scripts/http'), '-sS', '--max-time', '1', '-o', '/dev/null', '-w', '%{http_code}', f'http://localhost:{port}/actuator/health/readiness'], capture_output=True, text=True)
        if p.stdout == '200' and (jar or (control / 'ready').exists()):
            observed(name + ' ready', pid=proc.pid, argv=argv, env=env or {}, at=dt.datetime.now(dt.timezone.utc).isoformat())
            return info
        time.sleep(.1)
    raise TimeoutError(name + ' readiness')

def stop(app):
    if app['proc'].poll() is None:
        app['proc'].send_signal(signal.SIGTERM)
        app['proc'].wait(timeout=20)
    app['fh'].close()
    observed(app['name'] + ' stopped', pid=app['proc'].pid, exit=app['proc'].returncode)

def http(app, path, method='GET', body=None, peer='198.51.100.10', label=None, headers=None):
    argv = [str(ROOT / 'scripts/http'), '-sS', '--max-time', '8', '-i', '-X', method, '-H', 'X-Forwarded-For: ' + peer]
    for header in (headers or []): argv += ['-H', header]
    if body is not None: argv += ['-H', 'Content-Type: application/json', '-d', json.dumps(body)]
    argv += [f'http://localhost:{app["port"]}{path}']
    run = subprocess.run(argv, capture_output=True, text=True, timeout=10)
    if run.returncode: raise RuntimeError(run.stderr)
    head, raw = run.stdout.split('\n\n', 1)
    status = int(head.splitlines()[0].split()[1])
    fields = dict((key.lower(), value.strip()) for key, value in (line.split(':', 1) for line in head.splitlines()[1:] if ':' in line))
    try: parsed = json.loads(raw) if raw else None
    except json.JSONDecodeError: parsed = raw
    data = {'method': method, 'path': path, 'status': status, 'headers': fields, 'body': parsed, 'peer': peer, 'argv': argv, 'raw': run.stdout}
    EVIDENCE['observations'].append({'name': label or method + ' ' + path, **data})
    save('effects.json', EVIDENCE)
    return data

def cmd(app, op, **data):
    control = app['control']
    (control / 'ack.json').unlink(missing_ok=True)
    value = {'op': op, **data}
    (control / 'command.tmp').write_text(json.dumps(value))
    (control / 'command.tmp').replace(control / 'command.json')
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        if (control / 'ack.json').exists():
            result = json.loads((control / 'ack.json').read_text())
            observed(app['name'] + ' ' + op, command=value, result=result)
            if isinstance(result, dict) and 'error' in result: raise RuntimeError(result['error'])
            return result
        time.sleep(.01)
    raise TimeoutError(op)

def sql(app, query): return cmd(app, 'sql', sql=query)

def offline(directory, queries, name):
    src, dst = OUT / (name + '-sql.json'), OUT / (name + '.json')
    save(src.name, queries)
    p = subprocess.run([JAVA, '-cp', CP, 'dev.urlshort.click.QaRetentionRunner', 'offline', jdbc_url(directory), str(src), str(dst)], capture_output=True, text=True, timeout=20)
    if p.returncode: raise RuntimeError(p.stdout + p.stderr)
    return json.loads(dst.read_text())

def events(app, message=None):
    data = []
    for line in app['log'].read_text().splitlines():
        if not line.startswith('{'): continue
        try: value = json.loads(line)
        except json.JSONDecodeError: continue
        if message is None or value.get('message') == message: data.append(value)
    return data

def create(app, url):
    response = http(app, '/api/links', 'POST', {'url': url})
    check('created ' + url, response['status'] == 201)
    return response['body']['code'], response['body']

def shift(app, day, at='12:00:00'):
    cmd(app, 'clock', instant=str(day) + 'T' + at + 'Z')

def rows(app, code): return sql(app, "SELECT c.* FROM click c JOIN link l ON c.link_id=l.id WHERE l.code='" + code + "' ORDER BY c.id")
def link_id(app, code): return int(sql(app, "SELECT id FROM link WHERE code='" + code + "'")[0]['id'])
def statistics(app, code):
    r = http(app, '/api/links/' + code + '/stats')
    check('statistics 200 ' + code, r['status'] == 200)
    return r['body']

def redirects_on(app, code, day, count=1, peer='198.51.100.11', origin='https://qa-retained.example'):
    shift(app, day)
    for _ in range(count): check('redirect 302', http(app, '/' + code, peer=peer, headers=['Referer: '+origin+'/private','User-Agent: QA_BROWSER_SECRET'])['status'] == 302)
    cmd(app, 'settle')

COLS = "SELECT table_name,column_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema='PUBLIC' AND table_name IN ('CLICK','USER_AGENT_CLASS') ORDER BY table_name,ordinal_position"
CONSTRAINTS = "SELECT table_name,constraint_type FROM information_schema.table_constraints WHERE table_schema='PUBLIC' AND table_name IN ('CLICK','USER_AGENT_CLASS') ORDER BY table_name,constraint_type"
CLICK_V1 = 'id,link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash'
SNAPSHOT = ['SELECT * FROM link ORDER BY id', 'SELECT * FROM audit_log ORDER BY id', 'SELECT ' + CLICK_V1 + ' FROM click ORDER BY id', 'SELECT token FROM user_agent_class ORDER BY token', COLS, CONSTRAINTS, 'SELECT "version","success" FROM "flyway_schema_history" ORDER BY "installed_rank"']

def upgrade():
    directory = RUN / 'installed-upgrade'
    app = start('baseline-f6dd29e', 18141, directory, jar=BASE_JAR)
    code, body = create(app, 'https://qa.example/installed')
    retired, _ = create(app, 'https://qa.example/retired')
    http(app, '/' + code)
    check('original retire 204', http(app, '/api/links/' + retired, 'DELETE')['status'] == 204)
    time.sleep(.2)
    stop(app)
    T = dt.datetime.now(dt.timezone.utc).date()
    seed = ["INSERT INTO click (link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash) SELECT id,TIMESTAMP WITH TIME ZONE '" + str(T-dt.timedelta(days=age)) + " 10:00:00+00',DATE '" + str(T-dt.timedelta(days=age)) + "','https://upgrade-age" + str(age) + ".example','browser','" + 'a'*64 + "' FROM link WHERE code='" + code + "'" for age in [10,90,91,120]]
    offline(directory, seed, 'upgrade-seed')
    before = offline(directory, SNAPSHOT, 'upgrade-before')
    check('real shipped V1 V2 directory', [r['version'] for r in before[6] if r['version']] == ['1','2'])
    # Invalid starts get separate copies so no startup can delete another attempt's rows.
    for i, invalid in enumerate(['0','-5','ninety']):
        invalid_dir = RUN / ('invalid-' + str(i)); shutil.copytree(directory, invalid_dir)
        name = 'invalid-' + invalid.replace('-','minus')
        log = OUT / (name + '.jsonl')
        argv = [JAVA, '-jar', str(JAR), '--server.address=127.0.0.1', '--server.port=' + str(18145+i), '--spring.datasource.url=' + jdbc_url(invalid_dir)]
        with log.open('w') as fh:
            proc = subprocess.Popen(argv, cwd=ROOT, stdout=fh, stderr=subprocess.STDOUT, env={**os.environ,'URLSHORT_CLICK_RETENTIONDAYS':invalid})
            served = False
            while proc.poll() is None:
                r = subprocess.run([str(ROOT / 'scripts/http'), '-sS','--max-time','1','-o','/dev/null','-w','%{http_code}',f'http://localhost:{18145+i}/actuator/health/readiness'],capture_output=True,text=True)
                served |= r.stdout not in ['', '000']
                time.sleep(.05)
        after = offline(invalid_dir, ['SELECT ' + CLICK_V1 + ' FROM click ORDER BY id'], name + '-rows')[0]
        failure = [json.loads(l) for l in log.read_text().splitlines() if l.startswith('{') and 'APPLICATION FAILED TO START' in l]
        check('AC4 ' + invalid + ' no request served and rows unchanged', proc.returncode != 0 and not served and after == before[2])
        check('AC4 ' + invalid + ' failure setting/value only', len(failure)==1 and 'urlshort.click.retention' in failure[0]['message'] and invalid in failure[0]['message'] and 'jdbc:h2' not in failure[0]['message'] and 'password' not in failure[0]['message'])
        observed(name, exit=proc.returncode, served=served, failure=failure, argv=argv, env={'URLSHORT_CLICK_RETENTIONDAYS':invalid})
    app = start('candidate-installed',18142,directory,jar=JAR)
    purge = events(app,'clicks purged')
    check('AC7 AC9 startup automatic deletes two old rows',len(purge)==1 and purge[0]['deleted']==2 and purge[0]['retentionDays']==90 and purge[0]['cutoff']==str(T-dt.timedelta(days=90)))
    at_readiness=statistics(app,code)
    before_days={}
    for row in before[2]:before_days[row['clicked_on']]=before_days.get(row['clicked_on'],0)+1
    save('startup-counts.json',{'beforeDays':before_days,'afterDays':at_readiness['clicksPerDay'],'startupEvent':purge[0],'noPostReadinessRedirectYet':True})
    check('AC7 exact before after UTC counts at readiness',at_readiness['clicksPerDay']==[{'date':day,'clicks':count} for day,count in sorted(before_days.items()) if day>=str(T-dt.timedelta(days=90))])
    check('AC13 installed read body unchanged', http(app,'/api/links/'+code)['body']==body)
    check('AC13 installed redirect unchanged',http(app,'/'+code)['headers'].get('location')==body['url'])
    time.sleep(.3)
    stop(app)
    after = offline(directory, SNAPSHOT + ['SELECT * FROM click ORDER BY id','SELECT * FROM user_agent_class ORDER BY token'], 'upgrade-after')
    kept = [r for r in before[2] if r['clicked_on']>=str(T-dt.timedelta(days=90))]
    kept_ids = {r['id'] for r in kept}
    check('AC13 links and all audit rows unchanged',before[:2]==after[:2])
    check('AC13 surviving v1 clicks unchanged',kept==[r for r in after[2] if r['id'] in kept_ids] and len(after[2])==len(kept)+1)
    check('AC13 classes unchanged V3 applied',before[3]==after[3] and [r['version'] for r in after[6] if r['version']]==['1','2','3'] and all(r['success']=='TRUE' for r in after[6]))
    old_cols={(r['table_name'],r['column_name']):(r['data_type'],r['is_nullable']) for r in before[4]}
    new_cols={(r['table_name'],r['column_name']):(r['data_type'],r['is_nullable']) for r in after[4]}
    check('AC16 expand only schema and original constraints',all(new_cols.get(k)==v for k,v in old_cols.items()) and before[5]==after[5])
    for table in ['CLICK','USER_AGENT_CLASS']:
        for field in ['CREATED_AT','UPDATED_AT','CREATED_BY','UPDATED_BY']:
            check('AC16 upgraded '+table+'.'+field,new_cols.get((table,field))==('TIMESTAMP WITH TIME ZONE' if field.endswith('_AT') else 'CHARACTER VARYING','NO'))
    check('AC16 upgraded preexisting clicks backfilled',all(r['created_at']==r['updated_at']==r['clicked_at'] and r['created_by']==r['updated_by']=='anonymous' for r in after[7] if r['id'] in kept_ids))
    check('AC16 upgraded new clicks database audit values',all(r['created_at']==r['updated_at'] and r['created_by']==r['updated_by']=='anonymous' for r in after[7] if r['id'] not in kept_ids))
    check('AC16 classes system actors filled',all(r['created_at']==r['updated_at'] and r['created_by']==r['updated_by']=='system' for r in after[8]))
    return directory, code, T

def manual_journeys():
    T = dt.date(2026,12,15)
    app = start('clock-controlled',18143,RUN/'controlled')
    code, body = create(app,'https://qa.example/boundary')
    for i, age in enumerate([91,90,89,0]): redirects_on(app,code,T-dt.timedelta(days=age),2,peer=f'198.51.100.{20+i}')
    before = rows(app,code); shift(app,T)
    cmd(app,'purge'); after = rows(app,code)
    check('AC1 strict boundary six rows byte unchanged',after==[r for r in before if r['clicked_on']>=str(T-dt.timedelta(days=90))] and len(after)==6)
    shift(app,T+dt.timedelta(days=1));cmd(app,'purge')
    check('AC2 next day four rows remain',rows(app,code)==[r for r in after if r['clicked_on']>=str(T-dt.timedelta(days=89))])
    shift(app,T)
    stats_code,_=create(app,'https://qa.example/stats')
    for age in range(95,-1,-1):
        redirects_on(app,stats_code,T-dt.timedelta(days=age),peer=f'198.51.100.{100+(95-age)%100}',origin='https://only-old.example' if age>90 else 'https://only-kept.example')
    before_stats=statistics(app,stats_code);shift(app,T);cmd(app,'purge');after_stats=statistics(app,stats_code)
    expected=[r for r in before_stats['clicksPerDay'] if r['date']>=str(T-dt.timedelta(days=90))]
    check('AC5 four fields retained counts referrers',set(after_stats)==set(before_stats) and len(after_stats)==4 and after_stats['clicksPerDay']==expected and after_stats['totalClicks']==sum(r['clicks'] for r in expected) and 'https://only-old.example' not in json.dumps(after_stats['topReferrers']) and after_stats['totalClicks']==91)
    empty,created=create(app,'https://qa.example/empty')
    redirects_on(app,empty,T-dt.timedelta(days=100),peer='198.51.100.220')
    shift(app,T);audit=sql(app,'SELECT * FROM audit_log ORDER BY id');cmd(app,'purge')
    s=statistics(app,empty)
    check('AC6 purged statistics empty',s['totalClicks']==0 and s['clicksPerDay']==s['topReferrers']==[])
    r=http(app,'/'+empty,peer='198.51.100.221');cmd(app,'settle');s=statistics(app,empty)
    check('AC6 redirect recorded, creation and audit unchanged',r['status']==302 and r['headers'].get('location')==created['url'] and s['totalClicks']==1 and s['clicksPerDay']==[{'date':str(T),'clicks':1}] and http(app,'/api/links/'+empty)['body']==created and sql(app,'SELECT * FROM audit_log ORDER BY id')==audit)
    # Zero-delete run and all prior run event shapes, without request/client values.
    n=len(events(app,'clicks purged'));cmd(app,'purge');p=events(app,'clicks purged')
    forbidden={'requestId','clientHash','client_hash','referrer','userAgentClass','user_agent_class','code','linkId','link_id','error','errorType'}
    check('AC9 zero and positive single JSON INFO',len(p)==n+1 and p[-1]['deleted']==0 and any(e['deleted']>0 for e in p) and all(e['log']['level']=='INFO' and e['cutoff']==str(dt.date.fromisoformat(e['cutoff'])) and e['retentionDays']==90 and not forbidden.intersection(e) for e in p))
    # Actual SQL driver exception; the product converts it and emits only the exception class.
    redirects_on(app,empty,T-dt.timedelta(days=100),peer='198.51.100.222');shift(app,T)
    old=rows(app,empty);n=len(events(app,'click purge failed'));cmd(app,'fail');cmd(app,'purge')
    failed=events(app,'click purge failed')
    check('AC10 single WARN class only old rows remain',len(failed)==n+1 and failed[-1]['log']['level']=='WARN' and 'errorType' in failed[-1] and not {'requestId','error','stack_trace','stackTrace','exception'}.intersection(failed[-1]) and 'QA_PURGE_DRIVER_SECRET_CANARY' not in json.dumps(failed[-1]) and rows(app,empty)==old)
    check('AC10 redirect works during failed purge',http(app,'/'+empty,peer='198.51.100.223')['status']==302);cmd(app,'settle');statistics(app,empty)
    cmd(app,'purge');check('AC10 healthy retry deletes old',all(r['clicked_on']==str(T) for r in rows(app,empty)));statistics(app,empty)
    # Physical old-row delete blocks the real global DELETE in H2; new-row write must proceed.
    concurrent,_=create(app,'https://qa.example/concurrent');lid=link_id(app,concurrent)
    sql(app,"INSERT INTO click (link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash) SELECT "+str(lid)+",TIMESTAMP WITH TIME ZONE '2026-09-01 10:00:00+00',DATE '2026-09-01','https://bulk.example','unknown',REPEAT('b',64) FROM SYSTEM_RANGE(1,5000)")
    cmd(app,'hold',linkId=lid,cutoff=str(T-dt.timedelta(days=90)));cmd(app,'purgeAsync')
    deadline=time.monotonic()+2
    while time.monotonic()<deadline:
        state=cmd(app,'state')
        if state['enteredExecuteUpdate']:break
        time.sleep(.02)
    check('AC11 actual DELETE entered and unfinished',state['enteredExecuteUpdate'] and not state['finished'])
    redirect=http(app,'/'+concurrent,peer='198.51.100.224');cmd(app,'settle')
    state=cmd(app,'state');check('AC11 302 and writer settle while actual DELETE pending',redirect['status']==302 and not state['finished'])
    cmd(app,'release');s=statistics(app,concurrent)
    check('AC11 bulk old deleted and today click counted',s['totalClicks']==1 and s['clicksPerDay']==[{'date':str(T),'clicks':1}])
    # Genuine SecretKeySpec rejects an empty key before the writer queue; no handler/store mock.
    n=len(events(app,'click lost'));before_count=len(rows(app,empty));cmd(app,'breakSalt')
    redirect=http(app,'/'+empty,peer='198.51.100.225');cmd(app,'restoreSalt');cmd(app,'settle')
    lost=events(app,'click lost')
    check('AC12 reduction fails once, 302 correlated distinct WARN',redirect['status']==302 and len(lost)==n+1 and lost[-1]['log']['level']=='WARN' and lost[-1]['reason']=='reduction failed' and lost[-1]['requestId']==redirect['headers']['x-request-id'] and lost[-1]['errorType']=='java.lang.IllegalArgumentException' and 'Empty key' not in json.dumps(lost[-1]) and len(rows(app,empty))==before_count)
    # New columns use physical DB write time; clicked_at uses far-future application clock.
    fresh=rows(app,empty);new=fresh[-1]
    check('AC16 fresh DB new click database clock distinct from application clock',new['created_at']==new['updated_at'] and new['created_by']==new['updated_by']=='anonymous' and not new['created_at'].startswith(str(T)) and new['clicked_at'].startswith(str(T)))
    schema=sql(app,COLS);classes=sql(app,'SELECT * FROM user_agent_class ORDER BY token')
    save('fresh-schema.json',{'columns':schema,'newClick':new,'classes':classes})
    check('AC16 fresh eight not null audit columns',len([r for r in schema if r['column_name'] in ['CREATED_AT','UPDATED_AT','CREATED_BY','UPDATED_BY'] and r['is_nullable']=='NO'])==8 and all(r['created_by']==r['updated_by']=='system' for r in classes))
    # No-trigger 00:10Z crossing. Insert through real redirect on boundary day, then move forward.
    scheduled,_=create(app,'https://qa.example/scheduled')
    redirects_on(app,scheduled,T-dt.timedelta(days=90),peer='198.51.100.226')
    shift(app,T+dt.timedelta(days=1),'00:09:59');n=len(events(app,'clicks purged'))
    time.sleep(5.3)
    check('rule3 not before scheduled time',len(rows(app,scheduled))==1 and len(events(app,'clicks purged'))==n)
    began=time.monotonic();shift(app,T+dt.timedelta(days=1),'00:10:01')
    while time.monotonic()-began<8 and rows(app,scheduled):time.sleep(.15)
    elapsed=time.monotonic()-began
    check('AC8 no trigger automatic within 60 service seconds',rows(app,scheduled)==[] and elapsed<8 and len(events(app,'clicks purged'))==n+1)
    observed('AC8 autonomous clock crossing',realSeconds=elapsed,serviceSecondsAfterDue=1,triggerSent=False)
    time.sleep(5.2);check('rule3 one automatic run on same day',len(events(app,'clicks purged'))==n+1)
    # Public failures, on dedicated peers, before touching links used by purge assertions.
    check('bad URL ProblemDetail',http(app,'/api/links','POST',{'url':'javascript:qa-secret'},peer='198.51.100.230')['status']==400)
    check('unknown code ProblemDetail',http(app,'/api/links/noSuchQaCode',peer='198.51.100.231')['status']==404)
    duplicate,_=create(app,'https://qa.example/duplicate');duplicate2,_=create(app,'https://qa.example/duplicate');check('duplicate URL remains distinct creations',duplicate!=duplicate2)
    check('retire failure preserves 410',http(app,'/api/links/'+duplicate,'DELETE')['status']==204 and http(app,'/'+duplicate,peer='198.51.100.232')['status']==410)
    stop(app)

def seven_and_paused(directory, T):
    seven = start('env-seven',18144,RUN/'seven',env={'URLSHORT_CLICK_RETENTIONDAYS':'7'})
    fixed=dt.date(2026,12,15);code,_=create(seven,'https://qa.example/seven')
    for i,age in enumerate([8,7,0]):redirects_on(seven,code,fixed-dt.timedelta(days=age),peer=f'198.51.100.{240+i}')
    before=rows(seven,code);shift(seven,fixed);cmd(seven,'purge')
    check('AC3 real environment seven-day boundary',rows(seven,code)==[r for r in before if r['clicked_on']>=str(fixed-dt.timedelta(days=7))] and len(rows(seven,code))==2 and events(seven,'clicks purged')[-1]['retentionDays']==7)
    stop(seven)
    paused_dir=RUN/'paused';shutil.copytree(directory,paused_dir)
    # Candidate-installed directory is now V3; seed expired click before paused start.
    offline(paused_dir,["INSERT INTO click (link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash) SELECT MIN(id),TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00',DATE '2026-01-01',NULL,'unknown',REPEAT('c',64) FROM link"],'paused-seed')
    before=offline(paused_dir,['SELECT * FROM click ORDER BY id'],'paused-before')[0]
    paused=start('env-paused',18148,paused_dir,instant=str(T)+'T12:00:00Z',env={'URLSHORT_CLICK_PURGEENABLED':'false'})
    shift(paused,T,'12:01:00');time.sleep(5.3)
    check('AC15 60 service seconds after readiness unchanged',sql(paused,'SELECT * FROM click ORDER BY id')==before)
    shift(paused,T+dt.timedelta(days=1),'00:11:00');time.sleep(5.3)
    warns=events(paused,'click purge paused, no click is deleted')
    check('AC15 environment pause 60s after daily time unchanged one WARN no INFO',sql(paused,'SELECT * FROM click ORDER BY id')==before and len(warns)==1 and warns[0]['log']['level']=='WARN' and warns[0]['setting']=='urlshort.click.purge-enabled' and not events(paused,'clicks purged'))
    stop(paused)

def finish():
    EVIDENCE['stopped']=[{'name':a['name'],'pid':a['proc'].pid,'exit':a['proc'].poll()} for a in PROCESSES]
    save('effects.json',EVIDENCE)
    check('all owned app processes stopped',all(a['proc'].poll() is not None for a in PROCESSES))

def detail_and_public(directory):
    prior=json.loads((PROOF/'qa-effects-probe-history-correction/effects.json').read_text())
    ready=next(o for o in prior['observations'] if o['name']=='baseline-f6dd29e ready')
    url=next(a for a in ready['argv'] if a.startswith('--spring.datasource.url='))
    old_dir=pathlib.Path(url.split('jdbc:h2:file:',1)[1].split(';',1)[0]).parent
    columns="SELECT table_name,column_name,data_type,is_nullable,column_default,character_maximum_length,numeric_precision,numeric_scale,datetime_precision FROM information_schema.columns WHERE table_schema='PUBLIC' AND table_name IN ('CLICK','USER_AGENT_CLASS') ORDER BY table_name,ordinal_position"
    constraints="SELECT tc.table_name,tc.constraint_type,k.column_name,k.ordinal_position,cc.check_clause,rt.table_name AS referenced_table,rc.update_rule,rc.delete_rule FROM information_schema.table_constraints tc LEFT JOIN information_schema.key_column_usage k ON k.constraint_schema=tc.constraint_schema AND k.constraint_name=tc.constraint_name LEFT JOIN information_schema.check_constraints cc ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name LEFT JOIN information_schema.referential_constraints rc ON rc.constraint_schema=tc.constraint_schema AND rc.constraint_name=tc.constraint_name LEFT JOIN information_schema.table_constraints rt ON rt.constraint_schema=rc.unique_constraint_schema AND rt.constraint_name=rc.unique_constraint_name WHERE tc.table_schema='PUBLIC' AND tc.table_name IN ('CLICK','USER_AGENT_CLASS') ORDER BY tc.table_name,tc.constraint_type,k.ordinal_position,k.column_name"
    indexes="SELECT table_name,column_name,ordinal_position,ordering_specification,is_unique FROM information_schema.index_columns WHERE table_schema='PUBLIC' AND table_name IN ('CLICK','USER_AGENT_CLASS') ORDER BY table_name,column_name,ordinal_position,ordering_specification,is_unique"
    old=offline(old_dir,[columns,constraints,indexes],'v2-schema-detail')
    new=offline(directory,[columns,constraints,indexes],'v3-schema-detail')
    key=lambda r:(r['table_name'],r['column_name'])
    new_columns={key(r):r for r in new[0]}
    check('AC16 full v1 column defaults precision lengths unchanged',all(new_columns.get(key(r))==r for r in old[0]))
    check('AC16 v1 keys FKs checks indexes semantically unchanged',old[1:]==new[1:])
    app=start('public-failures',18149,RUN/'public-failures',normal_budgets=True)
    header=['Idempotency-Key: qa2-retention-replay']
    first=http(app,'/api/links','POST',{'url':'https://qa.example/keyed'},headers=header,peer='198.51.100.180')
    replay=http(app,'/api/links','POST',{'url':'https://qa.example/keyed'},headers=header,peer='198.51.100.180')
    mismatch=http(app,'/api/links','POST',{'url':'https://qa.example/keyed-other'},headers=header,peer='198.51.100.180')
    check('inherited duplicate replay and mismatch effects',first['status']==201 and replay['status']==201 and replay['body']==first['body'] and mismatch['status']==422)
    audit=sql(app,'SELECT * FROM audit_log ORDER BY id');check('replay mismatch no additional audit',len(audit)==1)
    shift(app,dt.date(2026,12,16),'12:00:01')
    expired=http(app,'/api/links','POST',{'url':'https://qa.example/keyed'},headers=header,peer='198.51.100.181')
    check('inherited idempotency expiry releases and creates new link',expired['status']==201 and expired['body']['code']!=first['body']['code'])
    for i in range(61):
        response=http(app,'/api/links/noSuchQaCode',peer='198.51.100.182',label='rate burst '+str(i+1))
        check('rate burst '+str(i+1),response['status']==(404 if i<60 else 429))
    check('inherited 429 correlated ProblemDetail Retry-After',response['headers'].get('content-type','').startswith('application/problem+json') and int(response['headers'].get('retry-after','0'))>0 and response['body']['instance'].endswith(response['headers']['x-request-id']))
    cmd(app,'clock',instant='2026-12-16T12:00:03Z');check('rate retry succeeds after advertised wait',http(app,'/api/links/noSuchQaCode',peer='198.51.100.182')['status']==404)
    stop(app)

if __name__=='__main__':
    try:
        if '--continue' in sys.argv or '--post-checks' in sys.argv:
            prior=json.loads((OUT/'effects.json').read_text())
            ready=next(o for o in prior['observations'] if o['name']=='candidate-installed ready')
            url=next(a for a in ready['argv'] if a.startswith('--spring.datasource.url='))
            directory=pathlib.Path(url.split('jdbc:h2:file:',1)[1].split(';',1)[0]).parent
            T=dt.datetime.now(dt.timezone.utc).date()
            OUT=PROOF/('qa-effects-public-detail-a8fc8b6' if '--post-checks' in sys.argv else 'qa-effects-continued-a8fc8b6');OUT.mkdir(exist_ok=True)
            EVIDENCE['priorUpgradeEvidence']='../qa-effects-a8fc8b6/effects.json'
        else:
            directory,code,T=upgrade()
        if '--post-checks' in sys.argv:
            detail_and_public(directory)
        else:
            manual_journeys()
            seven_and_paused(directory,T)
            if '--all' in sys.argv:detail_and_public(directory)
    except Exception as e:
        observed('driver stopped on error',error=str(e),traceback=traceback.format_exc())
        raise
    finally:
        for app in PROCESSES:
            if app['proc'].poll() is None:stop(app)
        finish()
