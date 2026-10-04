"""Reconcile the preserved release benchmark; run from the repository root."""
import collections
import datetime
import gzip
import hashlib
import json
from pathlib import Path
import re
import subprocess

candidate = "50ad9c3ab9e65baa4100ede1772b514322957fa5"
release = Path("missions/03-ambiguous-analytics/release")
output = Path(__file__).parent / "audit.json"
sha256 = lambda data: hashlib.sha256(data).hexdigest()
manifest = json.loads((release / "artifact-manifest-50ad9c3.json").read_text())
assert manifest["candidate"] == candidate
jar = Path("/private/tmp/urlshort-mission03-50ad9c3.jar")
assert sha256(jar.read_bytes()) == manifest["jarSha256"]
source_hashes = {}
for name in ("tools/bench.mjs", "scripts/smoke.sh"):
    pinned = subprocess.check_output(["git", "show", candidate + ":" + name])
    assert Path(name).read_bytes() == pinned, name + " differs from candidate"
    source_hashes[name] = sha256(pinned)

bench = (release / "bench-50ad9c3.txt").read_text()
assert "Command: scripts/smoke.sh --bench http://127.0.0.1:18230\nExit: 0" in bench
pattern = re.compile(
    r"^(.*): (\d+) requests in (\d+) ms \(offered (\d+) req/s for (\d+) s\), "
    r"achieved ([\d.]+) req/s, non-2xx/3xx (\d+), p50 ([\d.]+) ms, "
    r"p95 ([\d.]+) ms, p99 ([\d.]+) ms$", re.M)
phases = {}
for label, count, elapsed, rate, seconds, achieved, bad, p50, p95, p99 in pattern.findall(bench):
    phases[label] = dict(requests=int(count), elapsedMs=int(elapsed),
        offeredRate=int(rate), seconds=int(seconds), achievedRate=float(achieved),
        bad=int(bad), p50Ms=float(p50), p95Ms=float(p95), p99Ms=float(p99))
assert len(phases) == 4
for label, phase in phases.items():
    assert phase["seconds"] == 60 and phase["bad"] == 0
    assert phase["requests"] == phase["offeredRate"] * 60
    assert round(phase["requests"] * 1000 / phase["elapsedMs"], 1) == phase["achievedRate"]
redirect = phases["redirect GET /{code}"]
assert redirect["requests"] == 6000 and redirect["offeredRate"] == 100
assert redirect["achievedRate"] == 100.0
assert redirect["p95Ms"] <= 20 and redirect["p99Ms"] <= 50
assert phases["create POST /api/links"]["requests"] == 1200

raw = gzip.open(release / "jar-installed-log-50ad9c3.jsonl.gz", "rb").read()
rows = [json.loads(line) for line in raw.splitlines()]
assert {r["process"]["pid"] for r in rows} == {46435}
assert any("/private/tmp/urlshort-mission03-50ad9c3.jar" in r["message"] for r in rows)
assert any("jdbc:h2:file:/private/tmp/urlshort-m03-50ad9c3-db/urlshort" in r["message"] for r in rows)
assert any("Tomcat started on port 18230" in r["message"] for r in rows)
requests = [r for r in rows if r["message"] == "request completed"]
assert len(requests) == 19216
assert len({r["requestId"] for r in requests}) == len(requests)
instant = lambda r: datetime.datetime.fromisoformat(r["@timestamp"].replace("Z", "+00:00"))
groups = []
for request in requests:
    if not groups or (instant(request) - instant(groups[-1][-1])).total_seconds() > .5:
        groups.append([])
    groups[-1].append(request)
blocks = [g for g in groups if len(g) > 19000]
assert len(blocks) == 1
block = blocks[0]
assert len(block) == 19201  # One seed link, then 19,200 load requests.
assert collections.Counter(r["status"] for r in block) == {201: 1201, 302: 18000}
assert block[0]["status"] == 201
segments = [block[1:7201], block[7201:13201], block[13201:19201]]
assert collections.Counter(r["status"] for r in segments[0]) == {201: 1200, 302: 6000}
assert all(collections.Counter(r["status"] for r in g) == {302: 6000} for g in segments[1:])
windows = [dict(requests=len(g), start=g[0]["@timestamp"], end=g[-1]["@timestamp"],
    spanSeconds=(instant(g[-1]) - instant(g[0])).total_seconds()) for g in segments]
assert all(59 <= w["spanSeconds"] <= 61 for w in windows)

stats = json.loads((release / "bench-stats-50ad9c3.json").read_text())
assert stats["code"] == re.search(r"# bench link: (\w+)", bench)[1]
assert stats["totalClicks"] == 12000 and sum(d["clicks"] for d in stats["clicksPerDay"]) == 12000
assert stats["clicksPerDay"] == [dict(date="2026-10-04", clicks=12000, uniqueVisitors=1, botClicks=0)]
metrics = (release / "bench-prometheus-50ad9c3.txt").read_text().splitlines()
for method, status, expected in (("GET", "302", 12004), ("HEAD", "302", 6000), ("POST", "201", 1202)):
    matches = [line for line in metrics if line.startswith("http_server_requests_seconds_count{")
        and f'method="{method}"' in line and f'status="{status}"' in line]
    assert len(matches) == 1 and float(matches[0].rsplit(" ", 1)[1]) == expected
assert "urlshort_clicks_recorded_total 12004.0" in metrics
losses = [line for line in metrics if line.startswith("urlshort_clicks_lost_total{")]
assert len(losses) == 5 and all(float(line.rsplit(" ", 1)[1]) == 0 for line in losses)
rejections = [line for line in metrics if line.startswith("urlshort_ratelimit_rejections_total{")]
assert len(rejections) == 2 and all(float(line.rsplit(" ", 1)[1]) == 0 for line in rejections)
assert any(r["message"] == "Graceful shutdown complete" for r in rows)
assert any(r["message"] == "HikariPool-1 - Shutdown completed." for r in rows)

files = [release / name for name in (
    "bench-50ad9c3.txt", "bench-stats-50ad9c3.json", "bench-prometheus-50ad9c3.txt",
    "jar-installed-log-50ad9c3.jsonl.gz", "artifact-manifest-50ad9c3.json", "GAPS-snapshot.md")]
files += [Path("docs/REQUIREMENTS.md"), Path("docs/qa/GAPS.md")]
audit = dict(candidate=candidate, checkedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    verdict="PASS: item12 measurement/disclosure and item6 current gap record",
    jarSha256=manifest["jarSha256"], sourceHashes=source_hashes, phases=phases,
    logSha256=sha256(raw), totalRequestCompletions=len(requests), benchmarkBlockCompletions=len(block),
    serverCompletionWindows=windows, benchmarkClicks=stats["totalClicks"],
    recordedClicksIncludingFourPriorInstalledClicks=12004, lostClicks=0, quotaRejections=0,
    inputHashes={str(path): sha256(path.read_bytes()) for path in files},
    qualifications=["One preserved jar, one PID, file H2, trusted loopback, raised rate budgets; same host as load generator.",
        "Percentiles are the captured load-generator output. Per-request client latency samples were not retained; QA did not recompute them.",
        "Server logs carry status/requestId but no method/path; phase attribution uses script ordering and aggregate Prometheus method counts.",
        "Single-run NFR-L1 regression measurement; no capacity, container performance or isolated GET-minus-HEAD cost claim.",
        "No new app launch or full candidate QA gate in this proof follow-up. Earlier effects retain their existing attribution."])
output.write_text(json.dumps(audit, indent=2) + "\n")
print(json.dumps(audit, indent=2))
