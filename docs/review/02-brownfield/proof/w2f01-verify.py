"""Reconcile the narrow W2F-01 return and retain the reviewer's fresh gate totals."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).parent
WT = ROOT / ".worktrees/review-w2f01-50ad9c3"
QA = ROOT / "missions/02-brownfield/slices/02-click-retention/proof/w2f-01/qa"
MERGE = "50ad9c3ab9e65baa4100ede1772b514322957fa5"
CANDIDATE = "0552b815e39b9899fa4fdaacf3ea3454e116720c"
FILE = "src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java"

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)

def count_xml(paths):
    totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
    for path in paths:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
    return totals

def coverage(path):
    rows = list(csv.DictReader(path.open()))
    keys = ("LINE_MISSED", "LINE_COVERED", "BRANCH_MISSED", "BRANCH_COVERED")
    return {key: sum(int(row[key]) for row in rows) for key in keys}

assert git("-C", str(WT), "rev-parse", "HEAD").decode().strip() == MERGE
assert not git("-C", str(WT), "status", "--porcelain")
assert git("diff", "--name-only", MERGE + "^1", MERGE).decode().splitlines() == [FILE]
assert git("show", MERGE + ":" + FILE) == git("show", CANDIDATE + ":" + FILE)
assert not git("diff", CANDIDATE, MERGE, "--", "src", "scripts", "gradle", "build.gradle.kts", "settings.gradle.kts", ".github")
manifest = json.loads((QA / "artifact-hashes.json").read_text())
for entry in manifest:
    assert hashlib.sha256((ROOT / entry["path"]).read_bytes()).hexdigest() == entry["sha256"], entry["path"]

targeted = []
for index in range(1, 11):
    path = QA / f"targeted-{index:02}.xml"
    counts = count_xml([path])
    assert counts == {"tests": 1, "failures": 0, "errors": 0, "skipped": 0}, path
    suite = ET.parse(path).getroot()
    assert suite.find("testcase").get("name").startswith("AC08_aPurgeRunsEveryUtcDayWithoutAnOperator")
    events = [json.loads(line) for line in (suite.findtext("system-out") or "").splitlines() if line.startswith("{")]
    purges = [event for event in events if event.get("message") == "clicks purged"]
    assert [(e["deleted"], e["cutoff"]) for e in purges] == [(0, "2026-07-06"), (2, "2026-07-07")], path
    targeted.append({"run": index, **counts, "purges": [{k: e[k] for k in ("deleted", "cutoff")} for e in purges]})

own = {suite: count_xml(sorted((WT / "build/test-results" / suite).glob("TEST-*.xml"))) for suite in ("test", "functionalTest")}
assert own["test"] == {"tests": 226, "failures": 0, "errors": 0, "skipped": 0}
assert own["functionalTest"] == {"tests": 250, "failures": 0, "errors": 0, "skipped": 0}
own_csv = WT / "build/reports/jacoco/all/jacocoTestReport.csv"
merged_coverage = coverage(own_csv)
assert merged_coverage == {"LINE_MISSED": 0, "LINE_COVERED": 582, "BRANCH_MISSED": 0, "BRANCH_COVERED": 206}
assert coverage(QA / "full-coverage/all/jacocoTestReport.csv") == merged_coverage
log = (OUT / "w2f01-recheck-50ad9c3.txt").read_text()
assert "BUILD SUCCESSFUL" in log and "14 actionable tasks: 14 executed" in log
schedule = WT / "build/test-results/functionalTest/TEST-dev.urlshort.click.ClickRetentionScheduleJourneyTest.xml"
shutil.copyfile(schedule, OUT / "w2f01-recheck-50ad9c3.xml")
shutil.copyfile(own_csv, OUT / "w2f01-recheck-50ad9c3.csv")
result = {"merge": MERGE, "candidate": CANDIDATE, "changed_files": [FILE], "qa_hashes_matched": len(manifest), "qa_targeted": targeted, "own_gate": own, "merged_coverage": merged_coverage, "jacoco_execution_files": sorted(p.name for p in (WT / "build/jacoco").glob("*.exec")), "worktree_clean": True}
(OUT / "w2f01-reconciliation-50ad9c3.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps({k: v for k, v in result.items() if k != "qa_targeted"}, indent=2))
