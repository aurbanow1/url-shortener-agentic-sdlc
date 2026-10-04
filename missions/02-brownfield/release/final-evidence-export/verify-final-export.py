#!/usr/bin/env python3
"""Verify final audit custody without rebuilding or modifying preparation data."""
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
import tarfile
import urllib.parse

ROOT = pathlib.Path(__file__).resolve().parents[4]
HERE = pathlib.Path(__file__).resolve().parent
OUT = ROOT / "docs/evidence/02-brownfield"
PREP = ROOT / "missions/02-brownfield/release/final-30f8de4e"
sha = lambda data: hashlib.sha256(data).hexdigest()
records = []
for path in sorted(OUT.rglob("*.json")):
    if path.name == "final-validation.json":
        continue
    json.loads(path.read_text())
    records.append({"path": str(path.relative_to(OUT)), "sha256": sha(path.read_bytes())})
proof = json.loads((OUT / "proof-readiness.json").read_text())
assert proof["state"] == "ready" and not proof["issues"]
references = {}
counts = {}
for slice_record in proof["slices"]:
    readiness = slice_record["readiness"]
    assert readiness["state"] == "ready" and not readiness["issues"]
    counts[slice_record["id"]] = len(readiness["items"])
    spec = ROOT / "missions/02-brownfield/slices" / slice_record["id"] / "SPEC.md"
    text = spec.read_text()
    assert "approved-by:" in text and "approved-at:" in text, spec
    assert subprocess.check_output(["git", "show", "2020d53b:" + str(spec.relative_to(ROOT))], cwd=ROOT) == spec.read_bytes()
    for item in readiness["items"]:
        assert item["state"] == "accepted"
        for reference in item["judgment"]["evidence"]:
            name = pathlib.Path(reference["ref"])
            candidates = [ROOT / name, spec.parent / name]
            path = next((p for p in candidates if p.is_file()), None)
            assert path, reference
            actual = sha(path.read_bytes())
            assert actual == reference["sha256"], reference
            committed = subprocess.check_output(["git", "show", "HEAD:" + str(path.relative_to(ROOT))], cwd=ROOT)
            assert sha(committed) == actual, ("not committed", path)
            references[str(path.relative_to(ROOT))] = actual
assert sum(counts.values()) == 64
assert not subprocess.check_output(["git", "diff", "30f8de4e", "HEAD", "--", "src", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "Dockerfile", "compose.yaml", "scripts", "tools/sdlc-metrics.mjs"], cwd=ROOT)
gate = json.loads((OUT / "packets/qitem-20261004052127-b89c249b.transitions.json").read_text())
decision = next(r for r in gate if r["transitionId"] == 2078)
assert decision["actorSession"] == "human@kernel" and decision["transitionNote"].startswith("approve:")
assert "> " + decision["transitionNote"] in (ROOT / "missions/02-brownfield/NOTES.md").read_text()
mission_spec = ROOT / "missions/02-brownfield/SPEC.md"
assert "approved-at: 2026-10-04T05:24:24.131Z" in mission_spec.read_text()
assert subprocess.check_output(["git", "show", "10df955a:missions/02-brownfield/SPEC.md"], cwd=ROOT) == mission_spec.read_bytes()
trace = json.loads((OUT / "instances/01M40SN34E37K96B38JPG9K41X.trace.json").read_text())
assert trace["instance"]["currentStepId"] == "evidence_export"
assert any(row.get("stepId") == "release_review" and row.get("closureReason") == "handoff" for row in trace["trail"])
assert any(row.get("stepId") == "ship_signoff" and row.get("closureReason") == "handoff" for row in trace["trail"])
downstream = json.loads((OUT / "packets/qitem-20261004052725-7bd25702.show.json").read_text())
assert downstream["blockedOn"] == "qitem-20261004052612-42705c30"
frozen = {}
for path in sorted(PREP.rglob("*")):
    if path.is_file():
        name = str(path.relative_to(ROOT))
        original = subprocess.check_output(["git", "show", "ad0c01f6:" + name], cwd=ROOT)
        assert original == path.read_bytes(), ("reviewed preparation changed", name)
        frozen[name] = sha(original)
archive = HERE / "preparation-export-ad0c01f6.tar.gz"
with tarfile.open(archive) as snapshot:
    manifest = json.load(snapshot.extractfile("docs/evidence/02-brownfield/prep-validation.json"))
    for record in manifest["records"]:
        assert sha(snapshot.extractfile("docs/evidence/02-brownfield/" + record["path"]).read()) == record["sha256"]
    for name in ("docs/metrics/metrics.json", "docs/metrics/README.md"):
        assert snapshot.extractfile(name).read() == (ROOT / name).read_bytes(), ("shared dated metrics changed", name)

def anchors(path):
    used, result = {}, set()
    for line in path.read_text().splitlines():
        if re.match(r"^#{1,6} ", line):
            value = re.sub(r"^#{1,6} ", "", line).lower().strip()
            value = re.sub(r"[^\w\- ]", "", value).replace(" ", "-")
            count = used.get(value, 0)
            used[value] = count + 1
            result.add(value + (f"-{count}" if count else ""))
    return result

links = []
for document in [ROOT / "missions/02-brownfield/RELEASE.md", OUT / "INDEX.md", OUT / "INDEX-notes.md", ROOT / "docs/metrics/README.md", ROOT / "README.md", HERE / "README.md"]:
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", document.read_text()):
        if target.startswith(("https:", "http:", "mailto:")):
            continue
        name, _, anchor = urllib.parse.unquote(target.strip("<>")).partition("#")
        path = (document.parent / name).resolve() if name else document
        assert path.exists(), (document, target)
        if anchor:
            assert anchor in anchors(path), (document, target)
        links.append({"from": str(document.relative_to(ROOT)), "target": target})
result = {"state": "pass", "validatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(), "phase": "after human approval and all delivery stamps; before evidence_export closure/mission_close", "counts": {"rawJson": len(records), "instances": len(list((OUT / "instances").glob("*.trace.json"))), "packets": len(list((OUT / "packets").glob("*.show.json"))), "committedProofReferences": len(references), "localLinks": len(links), "frozenPreparationFiles": len(frozen)}, "proofState": proof["state"], "sliceAcceptedCounts": counts, "humanDecision": decision, "preparationArchiveSha256": sha(archive.read_bytes()), "frozenPreparationHashes": frozen, "proofEvidenceHashes": references, "links": links, "records": records}
(OUT / "final-validation.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps({k: v for k, v in result.items() if k in ("state", "counts", "proofState", "sliceAcceptedCounts")}, indent=2))
