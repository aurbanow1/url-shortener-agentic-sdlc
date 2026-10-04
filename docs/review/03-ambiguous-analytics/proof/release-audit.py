"""Independent release review: derive claims from archived wire bytes and source inputs."""
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit, unquote
import csv
import gzip
import hashlib
import json
import re
import subprocess
import tarfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).parent
REL = ROOT / "missions/03-ambiguous-analytics/release"
DOG = ROOT / "docs/qa/dogfood/03-ambiguous-analytics/50ad9c3"
SHA = "50ad9c3ab9e65baa4100ede1772b514322957fa5"
def read(p): return json.loads(p.read_text())
def digest(b): return hashlib.sha256(b).hexdigest()
def git(*args): return subprocess.check_output(["git", *args], cwd=ROOT)
def headers(s): return {k.lower(): v.strip() for k, v in (line.split(":", 1) for line in s.splitlines()[1:] if ":" in line)}
def events(s): return [json.loads(line) for line in s.splitlines() if line.startswith("{")]
result = {"reviewer": "review-agent@urlshort-factory", "candidate": SHA, "auditedAt": datetime.now(timezone.utc).isoformat()}

# Archive identity and all request/response/log joins, using the raw archive rather than display files.
manifest = read(DOG / "raw-manifest.json")
archive = (DOG / "raw-captures.tar.gz").read_bytes()
assert digest(archive) == manifest["archiveSha256"]
with tarfile.open(DOG / "raw-captures.tar.gz") as t:
    raw = {m.name: t.extractfile(m).read() for m in t.getmembers() if m.isfile()}
assert set(raw) == set(manifest["files"])
assert all(digest(b) == manifest["files"][name] for name, b in raw.items())
for name in ("http-ledger.json", "raw-manifest.json", "runtime-settings.json"):
    assert (DOG / name).read_bytes() == git("show", "8cf894a:" + str((DOG / name).relative_to(ROOT)))
ledger = read(DOG / "http-ledger.json")
responses = {}
canaries = ["m03-ua-canary", "m03-path-canary", "m03-query-canary", "m03-frag-canary", "m03-user-canary", "m03-password-canary", "m03-cursor-canary", "m03-invalid-canary", "inbound-requestid-m03-private", "203.0.113.7", "203.0.113.8", "203.0.113.9", "198.51.100.44", "192.0.2.20", "192.0.2.21"]
expected = defaultdict(dict)
all_ids = []
for row in ledger:
    name = row["name"]
    h = raw[f"http/{name}.headers"].decode()
    b = raw[f"http/{name}.body"].decode()
    status = int(h.splitlines()[0].split()[1])
    assert status == row["status"] and b == row["body"], name
    hdr = headers(h)
    rid = hdr["x-request-id"]
    assert re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}", rid)
    body = json.loads(b) if b.startswith("{") else None
    responses[name] = (status, hdr, body)
    expected[urlsplit(row["url"]).port][rid] = status
    all_ids.append(rid)
    if status >= 400:
        assert hdr["content-type"].startswith("application/problem+json")
        assert body["status"] == status and body["instance"] == "urn:uuid:" + rid
        assert not any(c in b for c in canaries), name
heads = [json.loads(b) for name, b in raw.items() if name.endswith(".wire.json")]
assert len(heads) == 4
for head in heads:
    assert head["bodyBytes"] == 0 and head["status"] in (200, 302)
    expected[urlsplit(head["url"]).port][head["requestId"]] = head["status"]
    all_ids.append(head["requestId"])
assert len(set(all_ids)) == len(all_ids) == 405
log_counts = {}
for port, mode in ((18232, "default"), (18233, "trusted")):
    for sink in ("console", "file"):
        text = raw[f"{mode}-{sink}.jsonl"].decode()
        assert not any(c in text for c in canaries)
        completed = [e for e in events(text) if e.get("message") == "request completed"]
        assert len(completed) == len(expected[port])
        assert {e["requestId"]: e["status"] for e in completed} == expected[port]
        log_counts[f"{mode}-{sink}"] = len(completed)

# Reconstruct every observed statistics body from successful prior GETs and the documented proxy/UA/referrer rules.
def sent(row): return {k.lower(): v.strip() for k, v in (h.split(":", 1) for h in row["headersSent"] if ":" in h)}
restart_at = next(r["before"] for r in ledger if r["name"] == "restart-after-campaign")
def fact(row):
    h = sent(row)
    client = "127.0.0.1"
    if urlsplit(row["url"]).port == 18233:
        client = next((x.strip() for x in reversed(h.get("x-forwarded-for", "").split(",")) if x.strip() and x.strip() != "127.0.0.1"), client)
    ua = h.get("user-agent", "").lower()
    bot = any(word in ua for word in ("bot", "crawler", "spider"))
    origin = None
    ref = urlsplit(h.get("referer", ""))
    if ref.scheme in ("http", "https") and ref.hostname:
        port = ref.port
        origin = ref.scheme + "://" + ref.hostname + (f":{port}" if port and (ref.scheme, port) not in (("http", 80), ("https", 443)) else "")
    epoch = int(urlsplit(row["url"]).port == 18233 and row["before"] >= restart_at)
    return (client, epoch), bot, origin
redirects = [r for r in ledger if r["method"] == "GET" and r["status"] == 302]
derived_stats = []
for row in ledger:
    body = responses[row["name"]][2]
    if row["status"] != 200 or not isinstance(body, dict) or "clicksPerDay" not in body:
        continue
    assert set(body) == {"code", "totalClicks", "clicksPerDay", "topReferrers"}
    prior = [r for r in redirects if urlsplit(r["url"]).port == urlsplit(row["url"]).port and urlsplit(r["url"]).path == "/" + body["code"] and r["after"] < row["before"]]
    facts = [fact(r) for r in prior]
    origins = Counter(f[2] for f in facts if f[2])
    days = [] if not prior else [{"date": "2026-10-04", "clicks": len(prior), "uniqueVisitors": len({f[0] for f in facts}), "botClicks": sum(f[1] for f in facts)}]
    assert body["totalClicks"] == len(prior) and body["clicksPerDay"] == days, (row["name"], body, days)
    assert body["topReferrers"] == [{"referrer": k, "clicks": v} for k, v in sorted(origins.items(), key=lambda x: (-x[1], x[0]))[:10]], row["name"]
    derived_stats.append({"name": row["name"], "clicks": len(prior), "days": days})
def body(name): return responses[name][2]
for mode in ("default", "trusted"):
    assert body(mode + "-create") == body(mode + "-replay")
    assert responses[mode + "-mismatch"][0] == 422
    assert body(mode + "-create")["shortUrl"].startswith("http://127.0.0.1:")
    for suffix in ("xff", "forwarded"):
        assert responses[mode + "-audit-spoof-" + suffix][0] == 403
for kind in ("campaign", "ranking", "audit", "link"):
    assert body("restart-before-" + kind) == body("restart-after-" + kind)
assert body("restart-before-recorded")["measurements"][0]["value"] == 157
assert body("restart-after-recorded")["measurements"][0]["value"] == 0
assert body("trusted-final-recorded")["measurements"][0]["value"] == 3
assert body("trusted-final-lost")["measurements"][0]["value"] == 0
concurrent = [r for r in ledger if r["name"].startswith("concurrent-")]
assert len(concurrent) == 100
for row in concurrent:
    status, h, b = responses[row["name"]]
    assert status == 302 and b is None and row["body"] == "" and h["cache-control"] == "no-store"
    assert h["location"] == body("trusted-create")["url"]
limits = [r for r in ledger if re.fullmatch(r"limit-api-\d+", r["name"])]
assert Counter(r["status"] for r in limits) == {200: 60, 429: 15}
assert all(int(responses[r["name"]][1]["retry-after"]) > 0 for r in limits if r["status"] == 429)
assert responses["limit-api-recovered"][0] == responses["limit-different-forwarded-client"][0] == 200
result["dogfood"] = {"rawMembers": len(raw), "wireResponses": len(ledger), "headResponses": len(heads), "logJoins": log_counts, "statsDerivedFromRequests": derived_stats, "concurrentRedirects": len(concurrent), "quotaStatuses": dict(Counter(r["status"] for r in limits)), "restartPersistence": True}

# Packaged artifact and source identity; actual files rather than manifest assertions alone.
art = read(REL / "artifact-manifest-50ad9c3.json")
jar_paths = [Path("/private/tmp/urlshort-mission03-50ad9c3.jar"), Path("/private/tmp/urlshort-mission03-container-50ad9c3.jar"), Path(read(DOG / "runtime-settings.json")["installedJar"])]
result["jarHashes"] = {str(p): digest(p.read_bytes()) for p in jar_paths}
assert set(result["jarHashes"].values()) == {art["jarSha256"]}
assert digest((REL / "GAPS-snapshot.md").read_bytes()) == art["gapSnapshotSha256"]
assert not git("diff", SHA, art["packageHeadSourceCheck"], "--", "src", "build.gradle.kts", "settings.gradle.kts", "gradle", "gradlew", "gradlew.bat", "scripts", "tools", "Dockerfile", "compose.yaml", ".github", "docs/api/openapi.json")
cov = ET.parse(REL / "coverage-canonical-50ad9c3.xml").getroot()
counters = {e.get("type"): {k: int(e.get(k)) for k in ("missed", "covered")} for e in cov.findall("counter")}
assert counters["LINE"] == {"missed": 0, "covered": 581} and counters["BRANCH"] == {"missed": 0, "covered": 206}
result["coverage"] = counters
osv = read(REL / "osv-50ad9c3.json")
coords = {}
for line in (REL / "dependencies-50ad9c3.txt").read_text().splitlines():
    m = re.search(r"[+\\]--- ([\w.-]+):([\w.-]+)(?::(\S+))?(?: -> (\S+))?", line)
    if m and (m[4] or m[3]): coords[m[1] + ":" + m[2]] = m[4] or m[3]
assert [k + ":" + v for k, v in coords.items()] == osv["dependencies"]
assert len(coords) == len(osv["raw"]["results"]) == 97
assert all(not r for r in osv["raw"]["results"]) and not osv["findings"] and not osv["details"]
result["osv"] = {"queriedAt": osv["queriedAt"], "coordinates": len(coords), "rawEmptyResults": 97}

# Installed release observations: parse the original complete HTTP messages and join to logs.
def wire(path):
    h, b = path.read_text().split("\n\n", 1)
    return int(h.splitlines()[0].split()[1]), headers(h), b
jar_bytes = gzip.decompress((REL / "jar-installed-log-50ad9c3.jsonl.gz").read_bytes())
teardown = read(REL / "teardown-50ad9c3.json")
assert digest(jar_bytes) == teardown["jarLogUncompressedSha256"]
installed = {}
for mode, logtext, unique in (("trusted", jar_bytes.decode(), 3), ("default", (REL / "container-log-50ad9c3.jsonl").read_text(), 1)):
    r = read(REL / f"installed-{mode}-50ad9c3.json")
    completed = [e for e in events(logtext) if e.get("message") == "request completed"]
    joins = defaultdict(list)
    for e in completed: joins[e["requestId"]].append(e["status"])
    wires = {}
    for exchange in r["exchanges"]:
        status, hdr, b = wire(REL / f"http-{mode}-50ad9c3" / (exchange["label"] + ".txt"))
        assert status == exchange["status"] and hdr["x-request-id"] == exchange["requestId"]
        assert joins[exchange["requestId"]] == [status]
        wires[exchange["label"]] = status, hdr, b
    stats = json.loads(wires["stats-0"][2])
    assert stats == r["stats"] and stats["totalClicks"] == 4
    assert stats["clicksPerDay"] == [{"date": "2026-10-04", "clicks": 4, "uniqueVisitors": unique, "botClicks": 1}]
    assert json.loads(wires["openapi"][2]) == json.loads(git("show", SHA + ":docs/api/openapi.json"))
    meter = lambda label: json.loads(wires[label][2])["measurements"][0]["value"]
    assert meter("recorded-8") - meter("recorded-2") == 4
    assert all(line.endswith(" 0.0") for line in wires["prometheus"][2].splitlines() if line.startswith("urlshort_clicks_lost_total"))
    for i in range(4):
        status, hdr, b = wires[f"redirect-{i}"]
        assert status == 302 and not b and hdr["cache-control"] == "no-store"
        assert hdr["location"] == json.loads(wires["create"][2])["url"]
    installed[mode] = {"responseLogJoins": len(wires), "stats": stats, "recordedDelta": 4}
result["installed"] = installed
container = read(REL / "container-inspect-50ad9c3.json")
assert container["Image"] == art["imageId"] and container["User"] == "urlshort" and container["ReadonlyRootfs"]
assert container["PortBindings"] == {"8080/tcp": [{"HostIp": "127.0.0.1", "HostPort": "18231"}]}
assert container["StopTimeout"] == 20 and any(m["Destination"] == "/app/data" and m["Type"] == "volume" for m in container["Mounts"])
assert all(p["exit_code"] == 7 for p in read(REL / "ports-after-teardown.json"))
for logtext, count in ((jar_bytes.decode(), 19216), ((REL / "container-log-50ad9c3.jsonl").read_text(), 27), ((REL / "rollback-installed-log-50ad9c3.jsonl").read_text(), 22)):
    ee = events(logtext)
    assert sum(e.get("message") == "request completed" for e in ee) == count
    assert any(e.get("message") == "Graceful shutdown complete" for e in ee)
result["containerAndTeardown"] = {"loopbackBinding": container["PortBindings"], "nonRootReadOnly": True, "gracefulLogsAndClosedPorts": True}

# Benchmark record and actual stored result. The retained log has no latency field;
# percentiles remain attributed to the reviewed due-time load generator, not recomputed.
bench = (REL / "bench-50ad9c3.txt").read_text()
assert "Exit: 0" in bench and "non-2xx/3xx 0, p50 1.7 ms, p95 3.7 ms, p99 9.5 ms" in bench
assert bench.count("offered 100 req/s for 60 s") == 3 and "1200 requests" in bench
bs = read(REL / "bench-stats-50ad9c3.json")
assert bs["totalClicks"] == 12000 and bs["clicksPerDay"][0]["clicks"] == 12000
result["bench"] = {"getClicks": 12000, "mixedRedirectP95Ms": 3.7, "mixedRedirectP99Ms": 9.5, "latencySamplesNotRetained": True, "L3IsUnisolatedProxy": True}

# The rehearsal is precisely the inverse analytics merge, preserving every other product path.
rollback = art["rollbackCommit"]
assert git("diff", "--binary", "c9b66dd", "c9b66dd^1", "--", "src", "docs/api/openapi.json") == git("diff", "--binary", SHA, rollback, "--", "src", "docs/api/openapi.json")
assert not git("diff", SHA, rollback, "--", "src/main/resources/db/migration", "src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java", "Dockerfile", "compose.yaml", "scripts", "build.gradle.kts", ".github")
status, hdr, b = wire(REL / "rollback-stats-before-50ad9c3.txt")
rb = json.loads(b)
assert status == 200 and rb["totalClicks"] == bs["totalClicks"] and rb["code"] == bs["code"]
assert rb["clicksPerDay"] == [{"date": "2026-10-04", "clicks": 12000}]
assert wire(REL / "rollback-counter-50ad9c3.txt")[0] == 404
assert "BUILD SUCCESSFUL" in (REL / "rollback-check-50ad9c3.txt").read_text()
assert "BUILD SUCCESSFUL" in (REL / "check-50ad9c3.txt").read_text()
result["rollback"] = {"commit": rollback, "exactInverseAnalyticsProductPatch": True, "migrationsAndW2F01Retained": True, "storedClicks": 12000}

# Export and engine-derived mission figures. No claim that a directory label is mission ownership.
export = ROOT / "docs/evidence/03-ambiguous-analytics"
export_files = list(export.rglob("*.json"))
for p in export_files: read(p)
assert len(export_files) == 407
metrics = read(REL / "metrics-relevant-8d3c536.json")
source_metrics = json.loads(git("show", "8d3c536:docs/metrics/metrics.json"))
assert metrics["factoryTotals"] == source_metrics["totals"]
date = lambda s: datetime.fromisoformat(s.replace("Z", "+00:00"))
for row in metrics["missionInstances"]:
    trace = read(export / "instances" / (row["instanceId"] + ".trace.json"))
    instance = trace["instance"]
    finish = instance["completedAt"] or metrics["generatedAt"]
    assert round((date(finish) - date(instance["createdAt"])).total_seconds()) == row["e2eLatencySec"]
    assert len(trace["trail"]) == row["stepClosures"] and not any(t["closureReason"] == "failed" for t in trace["trail"])
decisions = {}
for qitem, ident in (("qitem-20261003114944-9bd32a00", 830), ("qitem-20261003154347-19e96a75", 876)):
    ts = read(export / "packets" / (qitem + ".transitions.json"))
    chosen = next(t for t in ts if t["transitionId"] == ident)
    assert chosen["actorSession"] == "human@kernel"
    decisions[qitem] = chosen["transitionNote"]
result["governance"] = {"validExportJsonFiles": len(export_files), "missionLatenciesSec": [r["e2eLatencySec"] for r in metrics["missionInstances"]], "humanDecisions": decisions, "metricsSnapshot": "8d3c536"}
# File ledger: exported records are checked as source evidence, not new reviews of other missions.
paths = git("diff", "--name-only", "07f44e2^", "26cd845").decode().splitlines()
inventory = []
for name in paths:
    b = git("show", "26cd845:" + name)
    if name.endswith(".json"): json.loads(b)
    elif name.endswith(".xml"): ET.fromstring(b)
    elif name.endswith(".gz"): gzip.decompress(b)
    else: b.decode()
    exported = name.startswith("docs/evidence/03-ambiguous-analytics/") and name.endswith(".json")
    inventory.append({"path": name, "sha256": digest(b), "verdict": "PASS — exported record parsed; mission claims reconciled above; unrelated mission history contextual only" if exported else "PASS — package source/evidence read and reconciled within the release checklist; historical/pending records retain their scope"})
assert len(inventory) == 487
(OUT / "release-file-ledger-26cd845.json").write_text(json.dumps(inventory, indent=2) + "\n")
result["packagePaths"] = {"total": len(inventory), "exportJson": sum(p["path"].startswith("docs/evidence/") and p["path"].endswith(".json") for p in inventory)}
result["limitations"] = ["Archived observations re-derived; no new installed run or benchmark", "No salt-memory or natural lifecycle claim", "Same-runtime separate-author review under D17", "Current proof readiness checked separately"]
(OUT / "release-audit-50ad9c3.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps({k: v for k, v in result.items() if k != "dogfood"}, indent=2))
print("Raw audit:", len(raw), "members;", len(all_ids), "responses;", len(derived_stats), "statistics bodies independently recomputed")
