#!/usr/bin/env python3
"""Replay the frozen input set with the original metric formulas and clock."""
import datetime
import hashlib
import json
import pathlib
import subprocess
import tarfile
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
archive = HERE / "final-metrics-inputs.tar.gz"
recorded = json.loads((HERE / "final-metrics-replay.json").read_text())
assert hashlib.sha256(archive.read_bytes()).hexdigest() == recorded["archiveSha256"]
with tempfile.TemporaryDirectory(prefix="urlshort-metrics-replay-") as directory:
    root = pathlib.Path(directory)
    with tarfile.open(archive) as source:
        for member in source.getmembers():
            assert member.isfile() and not pathlib.PurePosixPath(member.name).is_absolute()
            assert ".." not in pathlib.PurePosixPath(member.name).parts
            target = root / member.name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.extractfile(member).read())
    expected = json.loads((root / "expected-metrics.json").read_text())
    script = root / "tools/sdlc-metrics.mjs"
    provenance = json.loads((HERE / "final-metrics-provenance.json").read_text())
    assert hashlib.sha256(script.read_bytes()).hexdigest() == provenance["generatorSha256"]
    epoch = round(datetime.datetime.fromisoformat(
        expected["totals"]["generatedAt"].replace("Z", "+00:00")).timestamp() * 1000)
    original = script.read_text()
    assert original.count("const now = Date.now();") == 1
    replay = root / "tools/replay.mjs"
    replay.write_text(original.replace("const now = Date.now();", f"const now = {epoch};"))
    subprocess.run(["git", "init", "--quiet"], cwd=root, check=True)
    subprocess.run(["node", str(replay), "--out", "replayed"], cwd=root,
                   check=True, stdout=subprocess.PIPE, text=True)
    actual = json.loads((root / "replayed/metrics.json").read_text())
    for data in (expected, actual):
        for instance in data["instances"]:
            instance["mission"] = "normalized-container-label"
    assert actual == expected, "Frozen metrics differ from the raw-input replay"
    print(json.dumps({"state": "pass", "instances": len(actual["instances"]),
                      "totals": actual["totals"]}, indent=2))
