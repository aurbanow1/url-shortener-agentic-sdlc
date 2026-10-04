"""Focused review of the attributed QA return and final release-document update."""
from pathlib import Path
from datetime import datetime, timezone
from collections import Counter
import hashlib
import json
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).parent
REL = ROOT / 'missions/03-ambiguous-analytics/release'
QA = ROOT / 'missions/03-ambiguous-analytics/slices/01-analytics-v2/proof'
SHA = '50ad9c3ab9e65baa4100ede1772b514322957fa5'
package = sys.argv[1]
git = lambda *a: subprocess.check_output(['git', *a], cwd=ROOT)
read = lambda p: json.loads(p.read_text())
hashof = lambda b: hashlib.sha256(b).hexdigest()
proof = read(OUT / 'release-readiness-ready.json')
items = proof['slices'][0]['readiness']['items']
assert proof['state'] == 'ready' and not proof['issues'] and len(items) == 12
references = []
for item in items:
    j = item['judgment']
    assert item['state'] == 'accepted' and j['actor'] == 'qa-agent@urlshort-factory'
    for evidence in j['evidence']:
        p = ROOT / evidence['ref']
        assert hashof(p.read_bytes()) == evidence['sha256'], evidence['ref']
        assert p.read_bytes() == git('show', package + ':' + evidence['ref']), 'uncommitted evidence ' + evidence['ref']
        references.append(evidence)
for index, sequence in ((12, 13), (6, 14)):
    j = next(i['judgment'] for i in items if i['index'] == index)
    assert j['subject'] == {'kind': 'commit', 'ref': SHA}
    p = QA / 'judgments' / f'{sequence:08}.md'
    assert p.read_bytes() == git('show', package + ':' + str(p.relative_to(ROOT)))
    assert 'id: ' + j['id'] in p.read_text()
assert len(references) == 41
qa_audit = read(QA / 'qa-release-50ad9c3/audit.json')
for name, digest in (qa_audit['inputHashes'] | qa_audit['sourceHashes']).items():
    assert hashof((ROOT / name).read_bytes()) == digest, name
assert qa_audit['candidate'] == SHA

# Readiness and gap refresh must preserve the previous pending snapshots.
for name in ('proof-readiness-50ad9c3.json', 'GAPS-snapshot.md', 'artifact-manifest-50ad9c3.json'):
    assert (REL / name).read_bytes() == git('show', '26cd845:missions/03-ambiguous-analytics/release/' + name)
after = read(REL / 'proof-readiness-after-qa-50ad9c3.json')
assert after['state'] == 'ready'
assert [i['judgment']['id'] for i in after['slices'][0]['readiness']['items']] == [i['judgment']['id'] for i in items]
gap = (ROOT / 'docs/qa/GAPS.md').read_bytes()
assert (REL / 'GAPS-after-qa-50ad9c3.md').read_bytes() == gap
assert hashof(gap) == '004509e6d7d92230fa3273e1c92e3b6b67c7bc42d44f90b6009dd135487fef43'

assert not git('diff', SHA, package, '--', 'src', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradle', 'gradlew', 'gradlew.bat', 'scripts', 'tools', 'Dockerfile', 'compose.yaml', '.github', 'docs/api/openapi.json')
release = ROOT / 'missions/03-ambiguous-analytics/RELEASE.md'
assert release.read_bytes() == git('show', package + ':' + str(release.relative_to(ROOT)))
links = re.findall(r'\[[^\]]+\]\(([^)]+)\)', release.read_text())
local = [l for l in links if not re.match(r'https?://', l)]
assert all((release.parent / l.split('#')[0]).exists() for l in local)
changed = git('diff-tree', '--no-commit-id', '--name-only', '-r', package).decode().splitlines()
inventory = []
for name in changed:
    b = git('show', package + ':' + name)
    if name.endswith('.json'): json.loads(b)
    else: b.decode()
    inventory.append({'path': name, 'sha256': hashof(b), 'verdict': 'PASS — focused post-QA release update; scoped claims and preserved history checked'})
out = dict(checkedAt=datetime.now(timezone.utc).isoformat(), candidate=SHA, package=package,
    proofState=proof['state'], acceptedItems=len(items), evidenceReferences=len(references),
    distinctEvidenceFiles=len({e['ref'] for e in references}), currentCommittedEvidence=True,
    receipts={str(i['index']):i['judgment']['id'] for i in items if i['index'] in (6,12)},
    qaInputHashesVerified=True, immutablePrepSnapshots=True, postQaGapHash=hashof(gap),
    productDiffEmpty=True, localReleaseLinks=len(local), deltaFileLedger=inventory,
    limitations=['No new product gate or installed run; original release audit retained',
        'Exact hosted CI/CD remains unverified; human ship decision remains separate'])
(OUT / 'release-final-audit-50ad9c3.json').write_text(json.dumps(out, indent=2)+'\n')
print(json.dumps(out, indent=2))
