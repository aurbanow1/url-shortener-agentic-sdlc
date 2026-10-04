"""Independent QA of exact 305f804, outside product/tests. All HTTP uses scripts/http."""
from pathlib import Path
import datetime as dt, hashlib, json, os, re, shutil, subprocess, time

ROOT=Path(__file__).resolve().parents[5]
PROOF=Path(__file__).resolve().parent
RUN='run-'+dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
OUT=PROOF/'qa-305f804'/RUN
OUT.mkdir(parents=True)
SCRATCH_ROOT=Path((PROOF/'qa-scratch.txt').read_text().strip())
SCRATCH=SCRATCH_ROOT/RUN
SCRATCH.mkdir()
(PROOF/'qa-last-run.txt').write_text(str(OUT.relative_to(ROOT))+'\n')
JAVA='/opt/homebrew/opt/openjdk@21/bin/java'
CP=(SCRATCH_ROOT/'classpath.txt').read_text()
WT=ROOT/'.worktrees/04-audit-columns'
BASE=Path('/private/tmp/urlshort-qa2-retention-96nqv4hx/baseline')
SHA='305f8045d45b19a9e3287d5fe3508af6e04db9a4'
CHECKS=[]; REQUESTS=[]; APPS=[]; SEQ=0

def save(name,value):
    (OUT/(name+'.json')).write_text(json.dumps(value,indent=2)+'\n')
def check(name,truth,detail=None):
    CHECKS.append({'claim':name,'pass':bool(truth),'detail':detail})
    save('assertions',CHECKS)
    if not truth: raise AssertionError(name+': '+str(detail))
def stamp(value):
    normalized=re.sub(r'([+-]\d\d)$',r'\1:00',value.replace('Z','+00:00'))
    normalized=re.sub(r'\.(\d+)',lambda m:'.'+m.group(1).ljust(6,'0')[:6],normalized)
    return dt.datetime.fromisoformat(normalized)
def dburl(directory):
    return 'jdbc:h2:file:'+str(directory/'urlshort')+';MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE'

class App:
    def __init__(self,name,port,directory,fixture=False,jar=None,limits=10000):
        self.name=name;self.port=port;self.directory=directory;self.fixture=fixture
        directory.mkdir(parents=True,exist_ok=True)
        self.control=SCRATCH/(name+'-control');self.control.mkdir(exist_ok=True)
        cmd=[JAVA]
        if fixture: cmd+=['-Dqa.dir='+str(self.control),'-cp',CP,'dev.urlshort.click.QaAuditColumnsRunner']
        else: cmd+=['-jar',str(jar or WT/'build/libs/urlshort.jar')]
        cmd+=['--server.address=127.0.0.1','--server.port='+str(port),'--spring.datasource.url='+dburl(directory),
              '--urlshort.public-base-url=http://localhost:'+str(port),
              '--urlshort.rate-limit.create-per-minute='+str(limits),
              '--urlshort.rate-limit.redirect-per-minute='+str(600 if limits==60 else limits)]
        self.logpath=OUT/(name+'.jsonl');self.log=self.logpath.open('w')
        self.process=subprocess.Popen(cmd,cwd=directory,stdout=self.log,stderr=subprocess.STDOUT)
        APPS.append(self);save(name+'-launch',{'argv':cmd,'pid':self.process.pid,'directory':str(directory)})
        deadline=time.monotonic()+50
        while time.monotonic()<deadline:
            if self.process.poll() is not None: raise RuntimeError(name+' stopped: '+self.logpath.read_text()[-1800:])
            probe=subprocess.run([str(ROOT/'scripts/http'),'-sS','--max-time','1','http://localhost:'+str(port)+'/actuator/health/readiness'],capture_output=True,text=True)
            if probe.returncode==0 and '"UP"' in probe.stdout and (not fixture or (self.control/'ready').exists()): break
            time.sleep(.15)
        else: raise TimeoutError(name+' readiness')
    def command(self,op,**args):
        ack=self.control/'ack.json'
        if ack.exists(): ack.unlink()
        tmp=self.control/'command.tmp';tmp.write_text(json.dumps({'op':op,**args}));tmp.rename(self.control/'command.json')
        deadline=time.monotonic()+15
        while not ack.exists():
            if time.monotonic()>deadline: raise TimeoutError(op)
            time.sleep(.01)
        value=json.loads(ack.read_text());ack.unlink()
        if isinstance(value,dict) and 'error' in value: raise RuntimeError(value['error'])
        return value
    def sql(self,query): return self.command('sql',sql=query)
    def clock(self,instant): return self.command('clock',instant=instant)
    def stop(self):
        if self.process.poll() is None:
            if self.fixture: self.command('stop')
            else: self.process.terminate()
            self.process.wait(timeout=20)
        self.log.close()
        check(self.name+' process stopped',self.process.poll() is not None,{'pid':self.process.pid,'exit':self.process.returncode})

def http(app,name,method,path,data=None,headers=None,expected=None):
    global SEQ
    cmd=[str(ROOT/'scripts/http'),'-sS','--max-time','8','-i','-X',method]
    for key,value in (headers or {}).items(): cmd+=['-H',key+': '+value]
    if data is not None: cmd+=['-H','Content-Type: application/json','--data',json.dumps(data)]
    cmd+=['http://localhost:'+str(app.port)+path]
    r=subprocess.run(cmd,capture_output=True,text=True);check(name+' curl exit',r.returncode==0,r.stderr)
    head,body=r.stdout.split('\n\n',1);lines=head.splitlines();status=int(lines[0].split()[1]);hs={}
    for line in lines[1:]:
        k,v=line.split(':',1);hs[k.lower()]=v.strip()
    try: parsed=json.loads(body)
    except json.JSONDecodeError: parsed=None
    SEQ+=1;record={'sequence':SEQ,'name':name,'app':app.name,'argv':cmd,'status':status,'headers':hs,'body':body,'json':parsed}
    REQUESTS.append(record);save('http-%03d-%s'%(SEQ,name),record)
    if expected is not None: check(name+' status',status==expected,status)
    check(name+' requestId',bool(hs.get('x-request-id')),hs)
    check(name+' no added-column headers',not any('updated' in k or 'created' in k for k in hs),hs)
    if status>=400:
        check(name+' RFC9457',hs.get('content-type','').startswith('application/problem+json') and parsed.get('status')==status,parsed)
        check(name+' problem requestId',parsed.get('instance')=='urn:uuid:'+hs['x-request-id'],parsed)
    return record

LINK_OLD='id,code,url,created_at,retired_at,idempotency_key'
AUDIT_OLD='id,occurred_at,actor,action,entity,entity_id,request_id,before_state,after_state'
COLS="SELECT table_name,column_name,data_type,is_nullable,column_default,character_maximum_length,datetime_precision FROM information_schema.columns WHERE table_schema='PUBLIC' AND table_name IN ('LINK','AUDIT_LOG') ORDER BY table_name,ordinal_position"
CONS="SELECT c.table_name,c.constraint_type,k.column_name,k.ordinal_position,cc.check_clause FROM information_schema.table_constraints c LEFT JOIN information_schema.key_column_usage k ON c.constraint_schema=k.constraint_schema AND c.constraint_name=k.constraint_name LEFT JOIN information_schema.check_constraints cc ON c.constraint_schema=cc.constraint_schema AND c.constraint_name=cc.constraint_name WHERE c.table_schema='PUBLIC' AND c.table_name IN ('LINK','AUDIT_LOG') ORDER BY c.table_name,c.constraint_type,k.column_name,k.ordinal_position,cc.check_clause"
QUERIES=[COLS,CONS,'SELECT '+LINK_OLD+' FROM link ORDER BY id','SELECT '+AUDIT_OLD+' FROM audit_log ORDER BY id',
         'SELECT "version","description","success" FROM "flyway_schema_history" WHERE "version" IS NOT NULL ORDER BY "installed_rank"']
def offline(name,directory,queries):
    inp=SCRATCH/(name+'-sql.json');output=SCRATCH/(name+'-result.json');inp.write_text(json.dumps(queries))
    cmd=[JAVA,'-cp',CP,'dev.urlshort.click.QaAuditColumnsRunner','offline',dburl(directory),str(inp),str(output)]
    r=subprocess.run(cmd,capture_output=True,text=True);check(name+' JDBC exit',r.returncode==0,r.stdout+r.stderr)
    result=json.loads(output.read_text());save(name,{'sql':queries,'results':result});return result
def link(app,code): return app.sql("SELECT * FROM link WHERE code='"+code+"'")[0]
def audit(app,code,action):return app.sql("SELECT * FROM audit_log WHERE entity_id='"+code+"' AND action='"+action+"'")[0]
def rowstamps(row,name):
    check(name+' equal filled audit row clocks',bool(row['created_at']) and row['created_at']==row['updated_at'],row)
    check(name+' actor equality',row['created_by']==row['updated_by']==row['actor']=='anonymous',row)
def create(app,name,url,key=None,headers=None):
    hs=dict(headers or {})
    if key:hs['Idempotency-Key']=key
    return http(app,name,'POST','/api/links',{'url':url},hs,201)
FIELDS={'code','shortUrl','url','state','createdAt'}
AFIELDS={'occurredAt','actor','action','entity','entityId','requestId','before','after'}
def response_shape(record,fields):check(record['name']+' exact shipped fields',set(record['json'])==fields,record['json'])
def audit_response(app,name):
    r=http(app,name,'GET','/api/audit?limit=100',expected=200);response_shape(r,{'items','next'})
    for i,row in enumerate(r['json']['items']):check(name+' row%d eight fields'%i,set(row)==AFIELDS,row)
    return r

try:
    check('exact candidate',subprocess.check_output(['git','rev-parse','HEAD'],cwd=WT,text=True).strip()==SHA)
    check('candidate worktree clean',subprocess.check_output(['git','status','--porcelain'],cwd=WT,text=True).strip()=='')
    check('f6 baseline exact',subprocess.check_output(['git','rev-parse','HEAD'],cwd=BASE,text=True).strip().startswith('f6dd29e'))
    save('jar-hashes',{str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in [BASE/'build/libs/urlshort.jar',WT/'build/libs/urlshort.jar']})

    # Controlled-clock HTTP, real service/repositories/DataSource, fresh H2 directory.
    app=App('controlled',18161,SCRATCH/'controlled-db',True)
    fresh=[app.sql(q) for q in QUERIES];save('fresh-schema',fresh)
    check('AC1 fresh history V1-V4',[r['version'] for r in fresh[4]]==['1','2','3','4'])
    t0='2026-08-01T12:00:00Z';t1='2026-08-01T12:02:00Z';t2='2026-08-02T13:00:00Z'
    url='https://example.com/qa04-A';key='QA04-IDEMPOTENCY-CANARY';ua='QA04-USERAGENT-CANARY'
    canaries={'User-Agent':ua,'X-Forwarded-For':'192.0.2.10'}
    c=create(app,'create-A',url,key,canaries);response_shape(c,FIELDS);code=c['json']['code']
    before=link(app,code);save('AC2-created-row',before);rowstamps(audit(app,code,'link.create'),'AC2')
    check('AC2 link business clocks',stamp(before['created_at'])==stamp(before['updated_at'])==stamp(t0),before)
    check('AC2 actors',before['created_by']==before['updated_by']=='anonymous',before)
    check('AC2 audit requestId',audit(app,code,'link.create')['request_id']==c['headers']['x-request-id'])
    app.clock(t1)
    for name,method,path,data,hs,status in [
        ('replay-A','POST','/api/links',{'url':url},{'Idempotency-Key':key},201),
        ('mismatch-A','POST','/api/links',{'url':url+'-different'},{'Idempotency-Key':key},422),
        ('read-A','GET','/api/links/'+code,None,None,200),
        ('redirect-A','GET','/'+code,None,None,302),
        ('stats-A','GET','/api/links/'+code+'/stats',None,None,200)]:
        rows=app.sql('SELECT * FROM audit_log ORDER BY id')
        r=http(app,name,method,path,data,hs,status)
        check('AC5 '+name+' link unchanged',link(app,code)==before)
        check('AC6 '+name+' trail unchanged',app.sql('SELECT * FROM audit_log ORDER BY id')==rows)
        if name in ['replay-A','read-A']:response_shape(r,FIELDS)
        if name=='replay-A':check('replay same body',r['json']==c['json'])
        if name=='redirect-A':check('AC8 redirect location and empty body',r['headers']['location']==url and r['body']=='')
        if name=='stats-A':response_shape(r,{'code','totalClicks','clicksPerDay','topReferrers'})
    app.command('settle')
    r=http(app,'retire-A','DELETE','/api/links/'+code,headers=canaries,expected=204)
    after=link(app,code);save('AC3-retired-row',after)
    check('AC3 retirement business clock',stamp(after['updated_at'])==stamp(after['retired_at'])==stamp(t1),after)
    check('AC3 creation unchanged',after['created_at']==before['created_at'] and after['created_by']==before['created_by'])
    rowstamps(audit(app,code,'link.retire'),'AC3');check('AC3 audit requestId',audit(app,code,'link.retire')['request_id']==r['headers']['x-request-id'])
    trail=app.sql('SELECT * FROM audit_log ORDER BY id');app.clock('2026-08-01T12:03:00Z')
    http(app,'retire-again','DELETE','/api/links/'+code,expected=410)
    check('AC5 repeat retire unchanged',link(app,code)==after and app.sql('SELECT * FROM audit_log ORDER BY id')==trail)
    retiredread=http(app,'read-retired','GET','/api/links/'+code,expected=200);response_shape(retiredread,FIELDS)
    check('AC8 retired createdAt preserved',retiredread['json']['createdAt']==c['json']['createdAt'])
    http(app,'redirect-retired','GET','/'+code,expected=410)
    app.clock(t2);b=create(app,'expired-key-create-B',url+'-B',key);bcode=b['json']['code']
    a2=link(app,code);brow=link(app,bcode);save('AC4-key-release',{'before':after,'A':a2,'B':brow})
    check('AC4 released key',a2['idempotency_key'] is None and brow['idempotency_key']==key)
    check('AC4 A updated equals B created',stamp(a2['updated_at'])==stamp(brow['created_at'])==stamp(brow['updated_at'])==stamp(t2))
    check('AC4 prior data preserved',all(a2[k]==after[k] for k in ['created_at','created_by','url','retired_at','code','id']))
    rowstamps(audit(app,bcode,'link.create'),'AC4-B')
    # Actual database CHECK rejects the retire audit INSERT after its link UPDATE.
    d=create(app,'create-D','https://example.com/qa04-D');dc=d['json']['code'];dbefore=link(app,dc)
    app.sql("ALTER TABLE audit_log ADD CONSTRAINT qa04_fail_retire CHECK (action <> 'link.retire' OR entity_id <> '"+dc+"')")
    trail=app.sql('SELECT * FROM audit_log ORDER BY id');app.clock('2026-08-02T13:02:00Z')
    fail=http(app,'audit-insert-fails','DELETE','/api/links/'+dc,expected=500)
    dafter=link(app,dc);save('AC5-real-jdbc-rollback',{'constraint':'qa04_fail_retire','before':dbefore,'after':dafter,'auditBefore':trail,'auditAfter':app.sql('SELECT * FROM audit_log ORDER BY id')})
    check('AC5 actual failed INSERT rolls back every link column',dafter==dbefore)
    check('AC5 no failed audit appended',app.sql('SELECT * FROM audit_log ORDER BY id')==trail)
    app.sql('ALTER TABLE audit_log DROP CONSTRAINT qa04_fail_retire')
    http(app,'D-still-redirects','GET','/'+dc,expected=302)
    audit_response(app,'audit-eight-fields')
    http(app,'audit-forwarded-refused','GET','/api/audit',headers={'X-Forwarded-For':'127.0.0.1'},expected=403)
    http(app,'audit-invalid-limit','GET','/api/audit?limit=0',expected=400)
    old=app.sql('SELECT * FROM audit_log ORDER BY id')
    for name,data in [('bad-url',{'url':'javascript:alert(1)'}),('missing-url',{}),('bad-key',{'url':'https://example.com/bad-key'})]:
        http(app,name,'POST','/api/links',data,{'Idempotency-Key':'bad key'} if name=='bad-key' else None,400)
    http(app,'unknown-code','GET','/zzzzzzzz',expected=404)
    check('bad inputs do not change trail',app.sql('SELECT * FROM audit_log ORDER BY id')==old)
    columns=app.sql('SELECT created_at,updated_at,created_by,updated_by FROM link UNION ALL SELECT created_at,updated_at,created_by,updated_by FROM audit_log')
    save('AC10-audit-columns',columns)
    for i,row in enumerate(columns):
        check('AC10 actors row%d'%i,row['created_by']==row['updated_by']=='anonymous')
        check('AC10 client values absent row%d'%i,not any(v in json.dumps(row) for v in [key,ua,'192.0.2.10',url]+[r['headers']['x-request-id'] for r in REQUESTS]))
    for row in app.sql('SELECT * FROM audit_log ORDER BY id'):rowstamps(row,'AC6-row-'+row['id'])
    app.stop()

    # Default 60 API/min limiter with frozen service clock, so no token refills during curl.
    # The loopback-only audit route is intentionally exempt; ordinary link reads consume API budget.
    limited=App('default-limiter',18162,SCRATCH/'limiter-db',fixture=True,limits=60)
    for i in range(61):
        http(limited,'rate-%02d'%i,'GET','/api/links/zzzzzzzz',expected=404 if i<60 else 429)
    check('429 retry header',int(REQUESTS[-1]['headers']['retry-after'])>0)
    limited.stop()

    # The real shipped f6 jar writes the directory. Backdate only one creation-time fixture
    # while stopped so its unchanged 24h release path can be exercised immediately.
    upgrade=SCRATCH/'shipped-db'
    shipped=App('f6-writer-1',18163,upgrade,jar=BASE/'build/libs/urlshort.jar')
    ar=create(shipped,'f6-create-release-A','https://example.com/shipped-A','shipped-key');ac=ar['json']['code']
    rr=create(shipped,'f6-create-retired','https://example.com/shipped-R');rc=rr['json']['code']
    http(shipped,'f6-retire-R','DELETE','/api/links/'+rc,expected=204);shipped.stop()
    offline('f6-clock-fixture',upgrade,["UPDATE link SET created_at=DATEADD('DAY',-2,created_at) WHERE code='"+ac+"'"])
    shipped=App('f6-writer-2',18163,upgrade,jar=BASE/'build/libs/urlshort.jar')
    br=create(shipped,'f6-create-release-B','https://example.com/shipped-B','shipped-key');bc=br['json']['code']
    cr=create(shipped,'f6-create-active','https://example.com/shipped-C');cc=cr['json']['code']
    http(shipped,'f6-redirect-active','GET','/'+cc,expected=302);shipped.stop()
    # One explicitly synthetic future event verifies LEAST caps its backfill, preserving occurred_at.
    offline('f6-future-event-fixture',upgrade,["INSERT INTO audit_log (occurred_at,actor,action,entity,entity_id,request_id,before_state,after_state) SELECT DATEADD('DAY',2,CURRENT_TIMESTAMP),actor,action,entity,entity_id,'qa04-future-event',before_state,after_state FROM audit_log ORDER BY id FETCH FIRST 1 ROW ONLY"])
    pre=offline('upgrade-before-f6',upgrade,QUERIES)
    oldA=next(r for r in pre[2] if r['code']==ac);check('AC7 real shipped key release',oldA['idempotency_key'] is None)
    upgraded=App('candidate-upgrade',18164,upgrade)
    http(upgraded,'upgraded-active-read','GET','/api/links/'+cc,expected=200)
    http(upgraded,'upgraded-retired-redirect','GET','/'+rc,expected=410)
    audit_response(upgraded,'upgraded-audit-eight-fields');upgraded.stop()
    post=offline('upgrade-after-candidate',upgrade,QUERIES+['SELECT * FROM link ORDER BY id','SELECT * FROM audit_log ORDER BY id'])
    check('AC7 every legacy link/audit value preserved',pre[2:4]==post[2:4])
    check('AC7 history V1-V4 successful',[r['version'] for r in post[4]]==['1','2','3','4'] and all(r['success']=='TRUE' for r in post[4]))
    newsets={'LINK':{'UPDATED_AT','CREATED_BY','UPDATED_BY'},'AUDIT_LOG':{'CREATED_AT','UPDATED_AT','CREATED_BY','UPDATED_BY'}}
    check('AC1 all legacy column metadata preserved',pre[0]==[r for r in post[0] if r['column_name'] not in newsets[r['table_name']]])
    check('AC1 constraints unchanged',pre[1]==post[1],{'before':pre[1],'after':post[1]})
    for table,names in newsets.items():
        added=[r for r in post[0] if r['table_name']==table and r['column_name'] in names]
        check('AC1 '+table+' added exactly',len(added)==len(names))
        for row in added:
            check('AC1 '+table+'.'+row['column_name']+' type/nullability',row['is_nullable']=='NO' and row['data_type']==('TIMESTAMP WITH TIME ZONE' if row['column_name'].endswith('_AT') else 'CHARACTER VARYING'),row)
    for row in post[5]:
        check('AC7 '+row['code']+' latest known write',row['updated_at']==(row['retired_at'] or row['created_at']),row)
        check('AC7 '+row['code']+' anonymous actors',row['created_by']==row['updated_by']=='anonymous')
    upgradeEnd=dt.datetime.now(dt.timezone.utc)
    for row in post[6]:
        rowstamps(row,'AC7-audit-'+row['id'])
        check('AC7 row time <= upgrade',stamp(row['created_at'])<=upgradeEnd)
        if row['request_id']=='qa04-future-event':check('AC7 future event capped without changing occurred_at',stamp(row['created_at'])<stamp(row['occurred_at']))
        else:check('AC7 past event backfilled exactly',row['created_at']==row['occurred_at'])
    # New writes on the same upgraded directory, then literal rollback on its stopped copy.
    upgraded=App('candidate-new-writes',18164,upgrade)
    nr=create(upgraded,'upgraded-new-create','https://example.com/upgraded-new');nc=nr['json']['code']
    http(upgraded,'upgraded-new-retire','DELETE','/api/links/'+nc,expected=204);upgraded.stop()
    full=offline('before-rollback-copy',upgrade,QUERIES+['SELECT * FROM link ORDER BY id','SELECT * FROM audit_log ORDER BY id'])
    for row in full[6]:rowstamps(row,'AC6-upgraded-audit-'+row['id'])
    copy=SCRATCH/'rollback-copy';shutil.copytree(upgrade,copy)
    migration=(WT/'src/main/resources/db/migration/V4__add_link_audit_columns.sql').read_text()
    header=migration.split('-- rollback',1)[1].split('\n\n',1)[0]
    rollback=[]
    for line in header.splitlines()[1:]:
        rollback += [s.strip() for s in re.sub(r'^--\s*','',line).split(';') if s.strip()]
    check('AC11 literal header eight statements',len(rollback)==8,rollback)
    offline('rollback-literal-header',copy,rollback)
    rolled=offline('rollback-after',copy,QUERIES)
    check('AC11 exact pre-V4 metadata/constraints',rolled[0:2]==pre[0:2])
    check('AC11 every legacy value preserved',rolled[2:4]==full[2:4])
    check('AC11 keeps V1-V3',[r['version'] for r in rolled[4]]==['1','2','3'])
    reapplied=App('candidate-reapply',18165,copy);audit_response(reapplied,'reapplied-audit-read');reapplied.stop()
    reapply=offline('rollback-reapplied',copy,QUERIES)
    check('AC11 reapply exact expanded schema',reapply[0:2]==post[0:2])
    check('AC11 reapply preserves legacy values',reapply[2:4]==full[2:4])
    check('AC11 reapply V1-V4 successful',[r['version'] for r in reapply[4]]==['1','2','3','4'] and all(r['success']=='TRUE' for r in reapply[4]))
    # Join every response to its own structured request event; inspect failure log as well.
    matches=[]
    for req in REQUESTS:
        lines=[]
        for line in (OUT/(req['app']+'.jsonl')).read_text().splitlines():
            try: log=json.loads(line)
            except json.JSONDecodeError:continue
            if log.get('requestId')==req['headers']['x-request-id']: lines.append(log)
        check('log match '+req['name'],len(lines)>=1,req['headers']['x-request-id'])
        matches.append({'http':req['sequence'],'name':req['name'],'requestId':req['headers']['x-request-id'],'logs':lines})
    save('request-log-matches',matches)
    logtext='\n'.join(p.read_text() for p in OUT.glob('*.jsonl'))
    check('AC10 no client canaries or raw IP in logs',not any(v in logtext for v in [key,ua,'192.0.2.10',url]))
    check('AC8 no added audit columns in logs',not any(v in logtext for v in ['updated_at','created_by','updated_by','updatedAt','createdBy','updatedBy']))
    check('AC5 actual JDBC constraint failure observed', 'DataIntegrityViolationException' in (OUT/'controlled.jsonl').read_text())
    check('candidate left exact SHA',subprocess.check_output(['git','rev-parse','HEAD'],cwd=WT,text=True).strip()==SHA)
    save('summary',{'candidate':SHA,'assertions':len(CHECKS),'passed':sum(c['pass'] for c in CHECKS),'httpRequests':len(REQUESTS),'apps':[{'name':a.name,'pid':a.process.pid,'exit':a.process.poll()} for a in APPS],'scratch':str(SCRATCH),
        'fixtures':['external primary Clock, JDBC controls via files; no HTTP controls', 'audit CHECK temporarily rejects one actual retire INSERT', 'f6 link creation timestamp moved back 2d while stopped; real f6 key release endpoint then invoked', 'one synthetic future audit event inserted on stopped f6 directory', 'rollback only on stopped directory copy'],
        'notChecked':['PostgreSQL','large-directory migration timing','Docker']})
    print(json.dumps({'candidate':SHA,'assertions':len(CHECKS),'httpRequests':len(REQUESTS),'result':'PASS'}),flush=True)
finally:
    for app in APPS:
        if app.process.poll() is None:
            try: app.stop()
            except Exception:
                app.process.terminate();app.process.wait(timeout=20);app.log.close()
