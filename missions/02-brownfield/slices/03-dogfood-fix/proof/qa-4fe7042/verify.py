#!/usr/bin/env python3
"""Independent QA reconciliation; reads retained candidate effects and Git history."""
import csv, hashlib, json, math, pathlib, re, subprocess, tarfile, xml.etree.ElementTree as ET
P=pathlib.Path(__file__).resolve().parent
R=P.parents[5]
W=R/".worktrees/03-dogfood-fix"
SHA="4fe70427bd0d182e886d6a19b217daa1d9e39f5d"
BASE="15db6c5"
checks=[]
def check(name,ok,detail=None):
    checks.append({"check":name,"pass":bool(ok),"detail":detail})
    if not ok: raise AssertionError(name+": "+str(detail))
def git(*args): return subprocess.check_output(["git",*args],cwd=R,text=True).strip()
def dump(name,data): (P/name).write_text(json.dumps(data,indent=2)+"\n")
def body(name): return json.loads((P/"http"/(name+".body")).read_text())
def headers(name):
    return dict((x.split(":",1)[0].lower(),x.split(":",1)[1].strip()) for x in (P/"http"/(name+".headers")).read_text().splitlines() if ":" in x)
def status(name): return int((P/"http"/(name+".headers")).read_text().splitlines()[0].split()[1])
def displayed(data): return ("\n".join(x.rstrip() for x in data.decode().splitlines()).rstrip("\n")+"\n").encode()
check("exact candidate",git("-C",str(W),"rev-parse","HEAD")==SHA)
# Preserve the original transport/JUnit/console bytes before whitespace-only display cleanup.
raw=P/"raw-captures.tar.gz"
if not raw.exists():
    files=sorted(f for f in P.rglob("*") if f.is_file() and f.suffix in [".headers",".body",".request",".xml",".txt",".jsonl"] and f!=raw)
    manifest=[{"path":str(f.relative_to(P)),"sha256":hashlib.sha256(f.read_bytes()).hexdigest()} for f in files]
    with tarfile.open(raw,"w:gz") as archive:
        for f in files: archive.add(f,arcname=str(f.relative_to(P)))
    dump("raw-capture-hashes.json",manifest)
    for f in files:
        if f.suffix in [".headers",".xml",".txt"] or f.name.endswith("-prometheus.body"):
            f.write_bytes(displayed(f.read_bytes()))
with tarfile.open(raw) as archive:
    for row in json.loads((P/"raw-capture-hashes.json").read_text()):
        data=archive.extractfile(row["path"]).read()
        check("retained raw hash "+row["path"],hashlib.sha256(data).hexdigest()==row["sha256"])
        f=P/row["path"]
        display=displayed(data) if f.suffix in [".headers",".xml",".txt"] or f.name.endswith("-prometheus.body") else data
        check("display matches raw "+row["path"],f.read_bytes()==display)
base_doc=body("base-openapi"); live=body("candidate-openapi")
committed=json.loads(git("show",SHA+":docs/api/openapi.json"))
original=json.loads(git("show",BASE+":docs/api/openapi.json"))
check("AC3 baseline live equals committed",base_doc==original)
check("AC3 candidate live equals committed",live==committed)
(P/"committed-vs-live.diff").write_text("" if live==committed else "DRIFT\n")
schemas=live["components"]["schemas"]
problem=schemas["ProblemDetail"]
old=original["components"]["schemas"]["ProblemDetail"]
check("AC1 exact problem member set",set(problem["properties"])=={"type","title","status","detail","instance","errors"})
check("AC1 errors optional","errors" not in problem.get("required",[]))
errors=problem["properties"]["errors"]
check("AC1 errors array reference",errors["type"]=="array" and errors["items"]=={"$ref":"#/components/schemas/ProblemFieldError"})
field=schemas["ProblemFieldError"]
check("AC1 item exactly required strings",field["type"]=="object" and set(field["properties"])==set(field["required"])=={"field","rule","message"} and all(p["type"]=="string" for p in field["properties"].values()))
check("AC1 old members unchanged",all(problem["properties"][k]==old["properties"][k] for k in ["type","title","status","detail","instance"]))
clean_old=json.loads(json.dumps(original)); clean_new=json.loads(json.dumps(committed))
for d in [clean_old,clean_new]:
    d["components"]["schemas"].pop("ProblemDetail");d["components"]["schemas"].pop("ProblemFieldError",None)
check("AC3 only problem components differ",clean_old==clean_new)
dump("schema-before-after.json",{"before":old,"after":problem,"errorsItem":field,"unchangedDocumentOutsideThoseComponents":True})
for k in ["before","after","errorsItem"]:
    (P/(k+"-schema.json")).write_text(json.dumps(json.loads((P/"schema-before-after.json").read_text())[k],indent=2)+"\n")
problem_refs=[]
for path,item in live["paths"].items():
    for method,operation in item.items():
        if not isinstance(operation,dict):continue
        for code,response in operation.get("responses",{}).items():
            for media,description in response.get("content",{}).items():
                if media=="application/problem+json":
                    check("BR3 same schema "+method+" "+path+" "+code,description["schema"]=={"$ref":"#/components/schemas/ProblemDetail"})
                    problem_refs.append([path,method,code])
cases=[("bad-create",400,True),("mismatch",422,True),("unknown",404,False),("gone",410,False),("burst-89",429,False),("bad-audit",400,True)]
comparisons=[]
for name,code,has_errors in cases:
    pair={}
    for label in ["base","candidate"]:
        n=label+"-"+name;b=body(n);h=headers(n)
        check("AC2 status "+n,status(n)==code)
        check("AC2 media "+n,h.get("content-type")=="application/problem+json")
        check("AC2 every member documented "+n,set(b)<=set(problem["properties"]))
        for key,value in b.items():
            if key!="errors":
                typ=problem["properties"][key].get("type")
                check("AC2 property type "+n+" "+key,(typ=="string" and isinstance(value,str)) or (typ=="integer" and isinstance(value,int)) or typ is None)
        check("AC2 errors presence "+n,("errors" in b)==has_errors)
        if has_errors:
            check("AC2 one exact required item "+n,len(b["errors"])==1 and set(b["errors"][0])=={"field","rule","message"} and all(isinstance(v,str) for v in b["errors"][0].values()))
        check("instance matches response request ID "+n,b["instance"]=="urn:uuid:"+h["x-request-id"])
        if code==429:check("create budget retry header "+n,int(h["retry-after"])>0)
        pair[label]={"status":status(n),"contentType":h["content-type"],"body":{k:v for k,v in b.items() if k!="instance"}}
    check("AC4 full normalized wire equality "+name,pair["base"]==pair["candidate"],pair)
    comparisons.append({"case":name,**pair})
dump("wire-comparison.json",comparisons)
for label in ["base","candidate"]:
    a=body(label+"-create");b=body(label+"-reuse")
    check("idempotent replay same response "+label,a==b and status(label+"-reuse")==201)
    check("retirement effect "+label,status(label+"-retire")==204 and status(label+"-gone")==410)
    audit=body(label+"-audit")
    check("functional audit effect "+label,len(audit["items"])==2 and {r["action"] for r in audit["items"]}=={"link.create","link.retire"})
    for row in audit["items"]:
        n=label+("-create" if row["action"]=="link.create" else "-retire")
        check("audit correlation "+n,row["requestId"]==headers(n)["x-request-id"] and row["entityId"]==a["code"])
    for name in ["disk-free","disk-total"]:
        metric=body(label+"-"+name)
        check("AC6 gauges remain numeric "+label+" "+name,status(label+"-"+name)==200 and len(metric["measurements"])>0 and all(isinstance(m["value"],(int,float)) and math.isfinite(m["value"]) and m["value"]>0 for m in metric["measurements"]))
        if label=="candidate":
            check("AC6 no path metric "+name,all(t["tag"]!="path" for t in metric["availableTags"]) and "/private/tmp/urlshort-dogfood-qa-4fe7042" not in json.dumps(metric))
    prom=(P/"http"/(label+"-prometheus.body")).read_text()
    samples=[x for x in prom.splitlines() if x and not x.startswith("#")]
    check("AC6 free/total scrape values "+label,all(any(x.startswith(n) and math.isfinite(float(x.rsplit(" ",1)[1])) for x in samples) for n in ["disk_free_bytes","disk_total_bytes"]))
    if label=="candidate":
        check("AC6 no sample path label or known cwd",all(not re.search(r'(?:\{|,)path=',x) for x in samples) and "/private/tmp/urlshort-dogfood-qa-4fe7042" not in prom)
        check("documented old path selector break",status(label+"-disk-selector")==404)
    else:
        check("baseline reproduces W2-03","path=" in prom and "/private/tmp/urlshort-dogfood-qa-4fe7042" in prom and status(label+"-disk-selector")==200)
    # 90 rapid POSTs: default create budget admits first 60 after replenishment, refuses 30.
    codes=[status(label+"-burst-"+str(i).zfill(2)) for i in range(90)]
    check("actual default create limit "+label,codes==[400]*60+[429]*30,codes)
# Inspect retained JSON file and default console independently.
correlations={}
for label in ["base","candidate"]:
    file_rows=[json.loads(x) for x in (P/(label+".jsonl")).read_text().splitlines() if x.startswith("{")]
    console_rows=[json.loads(x) for x in (P/(label+"-console.txt")).read_text().splitlines() if x.startswith("{")]
    file_req={r["requestId"]:r for r in file_rows if r.get("message")=="request completed"}
    con_req={r["requestId"]:r for r in console_rows if r.get("message")=="request completed"}
    captures=sorted((P/"http").glob(label+"-*.headers"))
    check("each request exactly one completion "+label,len(file_req)==len(captures)==len(con_req))
    for f in captures:
        n=f.stem;rid=headers(n)["x-request-id"]
        check("requestId UUID "+n,bool(re.fullmatch(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}",rid)))
        check("file and console code correlation "+n,file_req[rid]["status"]==con_req[rid]["status"]==status(n))
    for text in [(P/(label+".jsonl")).read_text(),(P/(label+"-console.txt")).read_text()]:
        check("whole-run privacy canaries absent "+label,all(v not in text for v in ["qa-dogfood-privacy-canary-4fe7042","https://qa-dogfood.example/canary-4fe7042","ftp://invalid.example/","127.0.0.1"]))
    correlations[label]={"requests":len(captures),"fileCompletions":len(file_req),"defaultConsoleCompletions":len(con_req)}
dump("request-log-correlation.json",correlations)
# Test history red controls, fresh green gate, and original suites.
suite_results={}
for suite in ["unit","functional"]:
    roots=[ET.parse(f).getroot() for f in (P/"test-results"/suite).glob("TEST-*.xml")]
    suite_results[suite]={k:sum(int(r.get(k,"0")) for r in roots) for k in ["tests","failures","errors","skipped"]}
    check("fresh candidate suite "+suite,all(suite_results[suite][k]==0 for k in ["failures","errors","skipped"]))
check("fresh counts",suite_results["unit"]["tests"]==204 and suite_results["functional"]["tests"]==207,suite_results)
shipped=json.loads((P/"shipped-suites-summary.json").read_text())
check("AC9 unchanged original suites",shipped["unit"]=={"tests":203,"failures":0,"errors":0,"skipped":0} and shipped["functional"]=={"tests":202,"failures":0,"errors":0,"skipped":0})
source_hashes=json.loads((P/"shipped-source-hashes.json").read_text())
check("50 original source/resource files",len(source_hashes)==50)
for path,digest in source_hashes.items():
    raw_source=subprocess.check_output(["git","show",BASE+":"+path],cwd=R)
    check("baseline source snapshot "+path,hashlib.sha256(raw_source).hexdigest()==digest)
provenance=json.loads((P/"jar-provenance.json").read_text())
for path,digest in provenance["baseInputHashes"].items():
    raw_input=subprocess.check_output(["git","show",BASE+":"+path],cwd=R)
    check("baseline jar input equals merge base "+path,hashlib.sha256(raw_input).hexdigest()==digest)
reds={}
for defect,folder,terms in [("W2-01","red-openapi-results",["errors","properties"]),("W2-03","red-metrics-results",["path"])]:
    failures=[{"class":t.get("classname"),"method":t.get("name"),"message":t.find("failure").get("message","")} for f in (P/folder).glob("TEST-*.xml") for t in ET.parse(f).getroot().findall("testcase") if t.find("failure") is not None]
    check("expected two fresh red assertions "+defect,len(failures)==2 and all(any(x in f["message"] for x in terms) for f in failures),failures)
    reds[defect]=failures
dump("regression-red-controls.json",reds)
history={"schemaTest":"cce7cf7a327f8a048ae056d82152804f2271022b","schemaFix":"a28a20ab9be7cde413d137cac7bdf149c1f79924","metricsTest":"72dfffbc97baf2608f2cd2111ecf7e6a3f7c5585","metricsFix":SHA,"design":"0d000da","amendmentClarification":"0982cb5"}
for before,after in [("schemaTest","schemaFix"),("metricsTest","metricsFix"),("design","schemaTest"),("amendmentClarification","schemaTest")]:
    check("chronological ancestry "+before+" before "+after,subprocess.run(["git","merge-base","--is-ancestor",history[before],history[after]],cwd=R).returncode==0)
for sha in [history["design"],history["amendmentClarification"]]:
    design=git("show",sha+":docs/DESIGN.md")
    for name in ["0010-committed-openapi-document.md","0016-metrics-and-health-exposure.md"]:
        check("ADR indexed before dependent code "+name,name in design and "03-dogfood-fix" in git("show",sha+":docs/adr/"+name))
design=(R/"docs/DESIGN.md").read_text();readme=(R/"README.md").read_text()
check("AC8 DESIGN optional errors and pathless meters","optional" in design and "errors" in design and 'ignoreTags("path")' in design and "without the installation path" in design)
check("AC8 README consistent","/actuator/prometheus" in readme and "path=" not in readme and "ProblemDetail" not in readme)
history["log"]=git("log","--format=%h %aI %s",BASE+".."+SHA)
dump("history-and-document-checks.json",history)
changed=git("diff","--numstat",BASE,SHA,"--","src/test","src/functionalTest").splitlines()
check("AC9 only two shipped tests receive additions",set(x.split("\t")[2] for x in changed)=={"src/test/java/dev/urlshort/web/MetricsConfigTest.java","src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java","src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java"} and all(x.split("\t")[1]=="0" for x in changed),changed)
product_files=git("diff","--name-only",BASE,SHA,"--","src/main","build.gradle.kts","settings.gradle.kts","gradle").splitlines()
check("BR5 metadata plus one meter rule only",product_files==["src/main/java/dev/urlshort/web/MetricsConfig.java","src/main/java/dev/urlshort/web/OpenApiConfig.java"],product_files)
dump("source-change-check.json",{"testChanges":changed,"productChanges":product_files})
coverage={}
for suite in ["unit","functional","all"]:
    fs=list((R/"docs/qa/coverage/03-dogfood-fix"/suite).glob("*.csv"))
    check("one report CSV "+suite,len(fs)==1)
    with fs[0].open() as f: rows=list(csv.DictReader(f))
    coverage[suite]={k:sum(int(r[k]) for r in rows) for k in ["LINE_MISSED","LINE_COVERED","BRANCH_MISSED","BRANCH_COVERED"]}
check("100 percent merged CSV",coverage["all"]=={"LINE_MISSED":0,"LINE_COVERED":508,"BRANCH_MISSED":0,"BRANCH_COVERED":194},coverage)
dump("coverage.json",coverage)
report_hashes=json.loads((P/"report-hashes.json").read_text())
check("all 354 copied report files",len(report_hashes)==354)
for path,digest in report_hashes.items():
    check("committed report hash "+path,hashlib.sha256((R/path).read_bytes()).hexdigest()==digest)
# Inventory source methods, including nested test classes, joined to fresh JUnit invocations.
methods=[];unmapped=[]
for suite,source in [("unit","test"),("functional","functionalTest")]:
    cases=[t for f in (P/"test-results"/suite).glob("TEST-*.xml") for t in ET.parse(f).getroot().findall("testcase")]
    for path in sorted((W/"src"/source/"java").rglob("*.java")):
        s=path.read_text();package=re.search(r"package\s+([\w.]+);",s).group(1)
        for m in re.finditer(r"@(Test|ParameterizedTest|RepeatedTest)\b(?:(?!\bvoid\b).)*?\bvoid\s+(\w+)\s*\(",s,re.S):
            annotation,name=m.groups();matches=[t for t in cases if t.get("classname","").startswith(package+"."+path.stem) and (t.get("name","")==name or t.get("name","").startswith(name+"(") or t.get("name","").startswith(name+"["))]
            exact=bool(matches)
            if not exact and annotation in ["ParameterizedTest","RepeatedTest"]:
                matches=[t for t in cases if t.get("classname","")==package+"."+path.stem and re.match(r"^\[\d+\]",t.get("name",""))]
            check("method or parameterized class actually invoked "+suite+" "+path.stem+"#"+name,len(matches)>0)
            classes=set(t.get("classname") for t in matches);check("method class unambiguous "+name,len(classes)==1)
            methods.append({"suite":suite,"cls":next(iter(classes)),"method":name,"annotation":annotation,"path":str(path.relative_to(R)),"invocations":len(matches) if exact else None,"resultAttribution":"method" if exact else "green class parameterized group; default JUnit display omits method"})
    for t in cases:
        if not any(m["suite"]==suite and m["cls"]==t.get("classname") and (t.get("name")==m["method"] or t.get("name","").startswith(m["method"]+"(") or t.get("name","").startswith(m["method"]+"[") or (m["invocations"] is None and re.match(r"^\[\d+\]",t.get("name","")))) for m in methods):unmapped.append([suite,t.attrib])
check("every invocation maps back to source",not unmapped,unmapped)
check("all 411 invocations green",sum(s["tests"] for s in suite_results.values())==411)
dump("source-methods.json",methods)
summary={"candidate":SHA,"baseline":BASE,"assertions":len(checks),"allPass":True,"httpCaptures":sum(x["requests"] for x in correlations.values()),"logCorrelation":correlations,"suites":suite_results,"originalSuites":shipped,"sourceMethodCounts":{s:sum(m["suite"]==s for m in methods) for s in ["unit","functional"]},"coverage":coverage,"problemReferences":len(problem_refs),"scope":"SPEC six problem cases; one default disk path; doc/history checks; no new Docker/load/migration claim","checks":checks}
dump("verification.json",summary)
print(json.dumps({k:v for k,v in summary.items() if k!="checks"},indent=2))
