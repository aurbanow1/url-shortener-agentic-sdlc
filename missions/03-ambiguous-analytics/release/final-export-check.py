import datetime
import hashlib
import json
from pathlib import Path
import re
import subprocess

root = Path.cwd()
out = root / "docs/evidence/03-ambiguous-analytics"
release = root / "missions/03-ambiguous-analytics/release"
sha = lambda data: hashlib.sha256(data).hexdigest()
records = {str(path.relative_to(out)): json.loads(path.read_text())
    for path in sorted(out.rglob("*.json")) if path.name != "final-validation.json"}
lifecycle = "01M40RVNDQ0KT7FPWN1KJW0DC3"
slice_id = "01M416Z3CM54YQTX93V4KG0CPS"
gate = "qitem-20261004023723-f58044d0"
qa = "qitem-20261004024232-5aea9029"
packet = "qitem-20261004024238-651b51b1"
metrics_packet = "qitem-20261004030758-9d6dc648"
required = ["compiled-graph.json", "proof-readiness.json", "scope-audit.json",
    "workflow-list.json", "workflow-status.json", "queue-active.json", "usage-top.json"]
required += ["instances/" + name + suffix for name in (lifecycle, slice_id)
    for suffix in (".trace.json", ".show.json")]
required += ["packets/" + name + suffix for name in (gate, qa, packet, metrics_packet)
    for suffix in (".transitions.json", ".show.json")]
assert all(name in records for name in required), [name for name in required if name not in records]
instance_rows = records["workflow-list.json"]
if not isinstance(instance_rows, list):
    instance_rows = instance_rows.get("instances", instance_rows.get("rows", instance_rows.get("items", [])))
for row in instance_rows:
    name = row.get("instanceId", row.get("instance_id", row.get("id")))
    assert name and "instances/" + name + ".trace.json" in records, row
    assert "instances/" + name + ".show.json" in records, row
for name, value in records.items():
    if name.endswith(".trace.json"):
        for qitem in set(re.findall(r"qitem-[A-Za-z0-9_-]+", json.dumps(value))):
            assert "packets/" + qitem + ".transitions.json" in records, qitem
            assert "packets/" + qitem + ".show.json" in records, qitem
    if name.endswith(".transitions.json"):
        assert name.replace(".transitions.json", ".show.json") in records, name
    if name.endswith(".show.json") and name.startswith("packets/"):
        assert name.replace(".show.json", ".transitions.json") in records, name
proof = records["proof-readiness.json"]
assert proof["state"] == "ready" and not proof["issues"]
items = proof["slices"][0]["readiness"]["items"]
assert len(items) == 12 and all(item["state"] == "accepted" for item in items)
evidence_checks = 0
references = set()
receipts = {}
for item in items:
    judgment = item["judgment"]
    assert judgment["actor"] == "qa-agent@urlshort-factory"
    for entry in judgment["evidence"]:
        path = root / entry["ref"]
        assert sha(path.read_bytes()) == entry["sha256"], str(path)
        pinned = subprocess.check_output(["git", "show", "0e125ca7:" + entry["ref"]])
        assert sha(pinned) == entry["sha256"], entry["ref"] + " not committed"
        evidence_checks += 1
        references.add(entry["ref"])
    if item["index"] in (1, 2, 5, 6):
        assert judgment["subject"]["ref"] == "50ad9c3ab9e65baa4100ede1772b514322957fa5"
        receipts[item["index"]] = judgment["sequence"]
assert receipts == {1: 18, 2: 15, 5: 16, 6: 17}
assert records["packets/" + qa + ".show.json"]["state"] == "done"
assert any("0e125ca7" in t["transitionNote"] for t in records["packets/" + qa + ".transitions.json"])
decision = next(t for t in records["packets/" + gate + ".transitions.json"] if t["transitionId"] == 1916)
assert decision["actorSession"] == "human@kernel"
assert "> " + decision["transitionNote"] in (root / "missions/03-ambiguous-analytics/RELEASE.md").read_text()
trace = records["instances/" + lifecycle + ".trace.json"]
assert trace["instance"]["instanceId"] == lifecycle
assert any(t["stepId"] == "ship_signoff" and t["closureReason"] == "handoff" for t in trace["trail"])
assert records["instances/" + slice_id + ".trace.json"]["instance"]["status"] == "completed"
compiled = records["compiled-graph.json"]
assert compiled["identity"]["mission"] == "03-ambiguous-analytics"
assert compiled["dependencies"] == trace["instance"]["lifecycleBinding"]["dependencies"]
gaps = (root / "docs/qa/GAPS.md").read_bytes()
assert (release / "GAPS-final-export-50ad9c3.md").read_bytes() == gaps
item6 = next(item for item in items if item["index"] == 6)
assert next(e["sha256"] for e in item6["judgment"]["evidence"] if e["ref"] == "docs/qa/GAPS.md") == sha(gaps)
historical = {}
for name in ("GAPS-snapshot.md", "proof-readiness-50ad9c3.json", "artifact-manifest-50ad9c3.json",
             "package-verification.json", "GAPS-after-qa-50ad9c3.md",
             "proof-readiness-after-qa-50ad9c3.json", "post-qa-verification-50ad9c3.json"):
    path = release / name
    before = subprocess.check_output(["git", "show", "14815f9:" + str(path.relative_to(root))])
    assert path.read_bytes() == before, name
    historical[name] = sha(before)
old_metrics_ref = "missions/03-ambiguous-analytics/release/metrics-relevant-8d3c536.json"
assert (root / old_metrics_ref).read_bytes() == subprocess.check_output(["git", "show", "14815f9:" + old_metrics_ref])
frozen_metrics = json.loads((release / "metrics-final-export.json").read_text())
metrics_data = subprocess.check_output(["git", "show", frozen_metrics["commit"] + ":docs/metrics/metrics.json"])
metrics = json.loads(metrics_data)
assert sha(metrics_data) == frozen_metrics["sourceSha256"]
assert frozen_metrics["factoryTotals"] == metrics["totals"]
assert frozen_metrics["generatedAt"] == metrics["totals"]["generatedAt"]
assert frozen_metrics["evidenceCommit"] == "3c48d0a9e744162179177145e3fac9471e8ccf25"
assert records["packets/" + metrics_packet + ".show.json"]["state"] == "done"
for row, name in zip(frozen_metrics["missionInstances"], (lifecycle, slice_id)):
    assert row["instanceId"] == name
    assert row == next(r for r in metrics["instances"] if r["instanceId"] == name)
    assert row["failedClosures"] == 0 and row["retries"] == 0 and row["mttrSec"] is None
    exported = records["instances/" + name + ".trace.json"]
    assert row["stepClosures"] == len(exported["trail"])
assert frozen_metrics["missionInstances"][0]["rollbacks"] == 1
assert frozen_metrics["missionInstances"][1]["rollbacks"] == 0
assert len(frozen_metrics["rollbackHeuristicNotes"]) == 1
assert frozen_metrics["rollbackHeuristicNotes"][0]["transitionId"] == 1849
assert frozen_metrics["rollbackHeuristicNotes"][0] in records["packets/qitem-20261004004828-05d2aab9.transitions.json"]
stamps = {}
for name in ("missions/03-ambiguous-analytics/SPEC.md", "missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md"):
    body = (root / name).read_text().split("---", 2)[1]
    actor = re.search(r"^approved-by:\s*(.+)$", body, re.M)
    at = re.search(r"^approved-at:\s*(.+)$", body, re.M)
    assert actor and at, name
    stamps[name] = {"actor": actor[1], "at": at[1]}

def anchors(path):
    counts, result = {}, set()
    for heading in re.findall(r"^#{1,6}\s+(.+?)\s*#*\s*$", path.read_text(), re.M):
        slug = re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        result.add(slug + ("-" + str(count) if count else ""))
    return result

links = 0
for name in ("missions/03-ambiguous-analytics/RELEASE.md",
             "docs/evidence/03-ambiguous-analytics/INDEX.md",
             "docs/evidence/03-ambiguous-analytics/INDEX-notes.md"):
    doc = root / name
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", doc.read_text()):
        if target.startswith(("http:", "https:", "app:")):
            continue
        local, _, fragment = target.strip("<>").partition("#")
        path = (doc.parent / local).resolve() if local else doc
        assert path.exists(), target
        if fragment and path.suffix == ".md":
            assert fragment in anchors(path), target
        links += 1
result = {"verifiedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "candidate": "50ad9c3ab9e65baa4100ede1772b514322957fa5", "qaCommit": "0e125ca7",
    "exportJsonFilesParsed": len(records), "instances": len(list((out / "instances").glob("*.trace.json"))),
    "packets": len(list((out / "packets").glob("*.transitions.json"))), "requiredArtifactsPresent": required,
    "proofState": proof["state"], "accepted": 12, "newReceipts": receipts,
    "committedEvidenceHashesChecked": evidence_checks, "distinctEvidenceFiles": len(references),
    "localLinksAndAnchorsVerified": links,
    "humanDecision": decision, "deliveryStamps": stamps, "historicalRecordsUnchanged": historical,
    "finalGapsSha256": sha(gaps), "scopeAuditFindings": records["scope-audit.json"]["totalFindings"],
    "metricsCommit": frozen_metrics["commit"], "metricsSourceSha256": frozen_metrics["sourceSha256"],
    "frozenMissionRowsAndTotalsMatchCommittedMetrics": True, "preparationMetricsUnchanged": True,
    "compiledSourceDigest": compiled["compiledInputDigest"],
    "runningSourceDigest": trace["instance"]["compiledInputDigest"], "dependencyEdgesEqual": True,
    "lifecycleStatusAtExport": trace["instance"]["status"], "lifecycleStepAtExport": trace["instance"]["currentStepId"]}
(out / "final-validation.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
