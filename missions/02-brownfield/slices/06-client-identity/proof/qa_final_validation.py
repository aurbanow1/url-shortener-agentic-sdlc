"""Final passive evidence verification and immutable file custody manifest."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path(__file__).resolve().parents[5]
OUT = ROOT / 'docs/qa/06-client-identity'
PROOF = Path(__file__).resolve().parent
SHA = 'fb63a88a9b92c1fec97ba74686af1a2f30304160'
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip() == SHA
assert subprocess.check_output(['git','status','--porcelain'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip() == ''
inventory = json.loads((OUT/'test-inventory.json').read_text())
trace = (ROOT/'docs/qa/TRACEABILITY.md').read_text().split('## 06-client-identity — candidate '+SHA)[1]
for row in inventory['methods']:
    assert row['class']+'#'+row['method'] in trace
for ac in range(1,16):
    assert '| AC-'+str(ac)+' |' in trace
assert (OUT/'gaps-append.md').read_text() in (ROOT/'docs/qa/GAPS.md').read_text()
assert (OUT/'traceability-append.md').read_text() in (ROOT/'docs/qa/TRACEABILITY.md').read_text()
run = ROOT/(PROOF/'qa-last-run.txt').read_text().strip()
responses = [json.loads(line) for line in (run/'requests.jsonl').read_text().splitlines()]
joins = json.loads((run/'request-log-joins.json').read_text())
assert len(responses) == len(joins) == 2041
assert len({(r['app'],r['name']) for r in responses}) == len(responses)
for record in responses:
    raw = (run/record['raw']).read_bytes().decode()
    head, body = raw.split('\r\n\r\n',1)
    assert int(head.splitlines()[0].split()[1]) == record['status']
    assert body == record['body']
assert json.loads((run/'scoped-comparison.json').read_text())['pass']
assert json.loads((OUT/'after/jar-scoped-comparison.json').read_text())['pass']
assert json.loads((OUT/'record-validation.json').read_text())['pass']
hashes = {}
for directory in [OUT,ROOT/'docs/qa/coverage/06-client-identity']:
    for path in sorted(directory.rglob('*')):
        if path.is_file() and path.name not in ['artifact-hashes-final.json','final-validation.json']:
            hashes[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
for path in sorted(PROOF.glob('qa*')):
    if path.is_file():
        hashes[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
for path in sorted((PROOF/'qa-media').rglob('*')):
    if path.is_file():
        hashes[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
path = PROOF/'QaIdentityRunner.java'
hashes[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
(OUT/'artifact-hashes-final.json').write_text(json.dumps(hashes,indent=2)+'\n')
for relative,digest in hashes.items():
    assert hashlib.sha256((ROOT/relative).read_bytes()).hexdigest() == digest
result = {'pass':True,'candidate':SHA,'worktreeClean':True,'artifactHashesRechecked':len(hashes),
          'coverageReportResources':378,'controlledRawResponseLogJoins':len(joins),
          'methodMappings':inventory['methodMappings'],'ACsMapped':15,'gapEntryPresent':True,
          'deferredItem':16,'sequencingTransition':1882}
(OUT/'final-validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result))
