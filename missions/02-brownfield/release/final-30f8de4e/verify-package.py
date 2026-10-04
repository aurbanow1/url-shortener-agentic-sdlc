#!/usr/bin/env python3
"""Check preparation records, local links, and committed proof evidence."""
import hashlib
import json
import pathlib
import re
import subprocess
import urllib.parse

ROOT = pathlib.Path(__file__).resolve().parents[4]
HERE = pathlib.Path(__file__).resolve().parent
EXPORT = ROOT / "docs/evidence/02-brownfield"
digest = lambda data: hashlib.sha256(data).hexdigest()
records = []
for path in sorted(EXPORT.rglob("*.json")):
    if path.name == "prep-validation.json":
        continue
    json.loads(path.read_text())
    records.append({"path": str(path.relative_to(EXPORT)), "sha256": digest(path.read_bytes())})
proof = json.loads((EXPORT / "proof-readiness.json").read_text())
assert proof["state"] == "ready" and not proof["issues"]
evidence = {}
counts = {}
for slice_record in proof["slices"]:
    readiness = slice_record["readiness"]
    assert readiness["state"] == "ready" and not readiness["issues"]
    counts[slice_record["id"]] = len(readiness["items"])
    for item in readiness["items"]:
        assert item["state"] == "accepted"
        for reference in item["judgment"]["evidence"]:
            ref = pathlib.Path(reference["ref"])
            candidates = [ROOT / ref, ROOT / "missions/02-brownfield/slices" / slice_record["id"] / ref]
            path = next((p for p in candidates if p.is_file()), None)
            assert path, reference
            actual = digest(path.read_bytes())
            assert actual == reference["sha256"], reference
            committed = subprocess.check_output(["git", "show", "HEAD:" + str(path.relative_to(ROOT))], cwd=ROOT)
            assert digest(committed) == actual, ("not committed", path)
            evidence[str(path.relative_to(ROOT))] = actual
assert sum(counts.values()) == 64
assert (HERE / "GAPS-snapshot.md").read_bytes() == (ROOT / "docs/qa/GAPS.md").read_bytes()
inputs = ["src", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "Dockerfile", "compose.yaml", "scripts", "tools/sdlc-metrics.mjs"]
assert not subprocess.check_output(["git", "diff", "30f8de4e", "HEAD", "--", *inputs], cwd=ROOT)

def anchors(path):
    used = {}
    result = set()
    for line in path.read_text().splitlines():
        if re.match(r"^#{1,6} ", line):
            value = re.sub(r"^#{1,6} ", "", line).lower().strip()
            value = re.sub(r"[^\w\- ]", "", value).replace(" ", "-")
            count = used.get(value, 0)
            used[value] = count + 1
            result.add(value + (f"-{count}" if count else ""))
    return result

links = []
documents = [ROOT / "missions/02-brownfield/RELEASE.md", ROOT / "docs/metrics/README.md", ROOT / "README.md", EXPORT / "INDEX.md", EXPORT / "INDEX-notes.md", HERE / "README.md", HERE / "rollback.md"]
for document in documents:
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", document.read_text()):
        if target.startswith(("https:", "http:", "mailto:")):
            continue
        target = urllib.parse.unquote(target.strip("<>"))
        name, _, anchor = target.partition("#")
        path = (document.parent / name).resolve() if name else document
        assert path.exists(), (document, target)
        if anchor:
            assert anchor in anchors(path), (document, target, "anchor")
        links.append({"from": str(document.relative_to(ROOT)), "target": target})
validation = {"phase": "release_prep after both QA closures; before independent release review and human decision", "state": "pass", "counts": {"rawJson": len(records), "instances": len(list((EXPORT / "instances").glob("*.trace.json"))), "packets": len(list((EXPORT / "packets").glob("*.show.json"))), "localLinks": len(links), "committedProofReferences": len(evidence)}, "proofState": proof["state"], "sliceAcceptedCounts": counts, "proofEvidenceHashes": evidence, "links": links, "records": records}
old = EXPORT / "prep-validation.json"
saved = HERE / "initial-export-validation.json"
if old.exists() and not saved.exists():
    saved.write_bytes(old.read_bytes())
old.write_text(json.dumps(validation, indent=2) + "\n")
print(json.dumps({k: v for k, v in validation.items() if k in ("state", "counts", "proofState", "sliceAcceptedCounts")}, indent=2))
