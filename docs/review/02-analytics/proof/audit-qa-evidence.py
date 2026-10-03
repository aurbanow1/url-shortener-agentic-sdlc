"""Reconcile committed QA captures and fresh review reports; run from repository root."""
import csv
import json
import re
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

root = Path(__file__).resolve().parents[4]
proof = root / "missions/01-greenfield-core/slices/02-analytics/proof"
candidate = "862c52eea8294e438b1f98b832ae4f64f7a16923"
http = json.loads((proof / "qa-http-862c52e.json").read_text())
assert http["candidate"] == candidate
exchanges = http["exchanges"]
events = [json.loads(line) for line in (proof / "qa-correlated-events-862c52e.jsonl").read_text().splitlines()]
ids = [e["headers"]["x-request-id"] for e in exchanges]
assert len(exchanges) == len(set(ids)) == 312
for exchange, request_id in zip(exchanges, ids):
    matching = [e for e in events if e.get("requestId") == request_id and e["message"] == "request completed"]
    assert len(matching) == 1 and matching[0]["status"] == exchange["status"]
    if exchange["status"] == 302:
        assert exchange["headers"]["cache-control"] == "no-store"
        assert exchange["headers"]["location"] in {v["target"] for v in http["codes"].values()}
    if exchange["status"] >= 400:
        problem = json.loads(exchange["body"])
        assert exchange["headers"]["content-type"].startswith("application/problem+json")
        assert problem["status"] == exchange["status"] and problem["instance"] == "urn:uuid:" + request_id
        assert not {"detail", "trace", "exception"} & problem.keys()
    if exchange["path"].endswith("/stats") and exchange["status"] == 200 and exchange["request"]["method"] == "GET":
        stats = json.loads(exchange["body"])
        assert set(stats) == {"code", "totalClicks", "clicksPerDay", "topReferrers"}
        assert sum(d["clicks"] for d in stats["clicksPerDay"]) == stats["totalClicks"]
        assert stats["clicksPerDay"] == sorted(stats["clicksPerDay"], key=lambda d: d["date"])
        assert len(stats["topReferrers"]) <= 10
        assert stats["topReferrers"] == sorted(stats["topReferrers"], key=lambda r: (-r["clicks"], r["referrer"]))
lost = [e for e in events if e["message"] == "click lost"]
assert len(events) == 314 and len(lost) == 1 and lost[0]["log"]["level"] == "WARN"
assert not any("stack_trace" in json.dumps(e) for e in events)
print("PASS: 312 unique HTTP ids/completion statuses; redirects, problems and every captured stats body agree; 314 events, one loss WARN")

with (proof / "qa-clicks-862c52e.csv").open() as f:
    clicks = list(csv.DictReader(f))
counts = Counter(row["CODE"] for row in clicks)
assert len(clicks) == 256
for label, count in {"representative": 3, "seven": 7, "ranking": 26, "retiredFour": 4, "failed": 0, "concurrent": 200}.items():
    assert counts[http["codes"][label]["code"]] == count
for row in clicks:
    assert row["USER_AGENT_CLASS"] in {"unknown", "browser", "bot", "other"}
    assert re.fullmatch("[0-9a-f]{64}", row["CLIENT_HASH"])
    assert row["CLICKED_AT"][:10] == row["CLICKED_ON"] and row["CLICKED_AT"].endswith("+00")
forward = [r for r in clicks if r["CODE"] == http["codes"]["ua6"]["code"]]
assert len(forward) == 3 and len({r["CLIENT_HASH"] for r in forward}) == 1
for kind, count in [("audit", 24), ("link", 22)]:
    before = (proof / f"qa-{kind}-before-862c52e.csv").read_bytes()
    after = (proof / f"qa-{kind}-after-862c52e.csv").read_bytes()
    assert before == after
    assert len(list(csv.DictReader(before.decode().splitlines()))) == count
print("PASS: 256 reduced clicks, exact 200 concurrent; spoofed forwarding identity unchanged; 24 audit/22 link rows byte-identical")

worktree = root / ".worktrees/02-analytics"
live = json.loads((proof / "qa-live-openapi-862c52e.json").read_text())
committed = json.loads((worktree / "docs/api/openapi.json").read_text())
assert live == committed
for suite, expected in [("test", 121), ("functionalTest", 126)]:
    reports = [ET.parse(f).getroot() for f in (worktree / f"build/test-results/{suite}").glob("TEST-*.xml")]
    assert sum(int(r.attrib["tests"]) for r in reports) == expected
    assert all(int(r.attrib[k]) == 0 for r in reports for k in ("failures", "errors", "skipped"))
    print(f"PASS: fresh {suite} reports {expected} invocations, no failures/errors/skips")
for name, expected in [("unit", (34, 325, 0, 118)), ("functional", (35, 324, 26, 92)), ("all", (0, 359, 0, 118))]:
    with (root / f"docs/qa/coverage/02-analytics/{name}/jacocoTestReport.csv").open() as f:
        rows = list(csv.DictReader(f))
    counts = tuple(sum(int(r[k]) for r in rows) for k in ("LINE_MISSED", "LINE_COVERED", "BRANCH_MISSED", "BRANCH_COVERED"))
    assert counts == expected
    print(f"PASS: QA {name} CSV counters (line missed/covered, branch missed/covered) = {counts}")
fresh = ET.parse(worktree / "build/reports/jacoco/all/jacocoTestReport.xml").getroot()
qa_xml = ET.parse(root / "docs/qa/coverage/02-analytics/all/jacocoTestReport.xml").getroot()
for counter in fresh.findall("counter"):
    if counter.attrib["type"] in {"LINE", "BRANCH"}:
        assert int(counter.attrib["missed"]) == 0
        assert counter.attrib == next(c.attrib for c in qa_xml.findall("counter") if c.get("type") == counter.get("type"))
        print("PASS: fresh merged", counter.attrib)
print("NOTE: both XML roots count 358 distinct lines; both per-class CSV sums count 359 (shared line in web). Both are 100%.")
print("PASS: candidate live OpenAPI equals committed JSON")
