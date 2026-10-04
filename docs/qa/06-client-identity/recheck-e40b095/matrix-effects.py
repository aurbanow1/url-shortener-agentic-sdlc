"""Affected AC4/5/10 replay on current production; historical capture untouched."""
from pathlib import Path
import datetime as dt
import hashlib
import json
import subprocess

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent
source = ROOT / 'missions/02-brownfield/slices/06-client-identity/proof/qa_by_effect.py'
prefix = source.read_text().split('\ntry:\n')[0]
prefix = prefix.replace("OUT = ROOT / 'docs/qa/06-client-identity/effects' / RUN",
                        "OUT = ROOT / 'docs/qa/06-client-identity/recheck-e40b095/matrix-effects' / RUN")
prefix = prefix.replace("(PROOF / 'qa-last-run.txt').write_text(str(OUT.relative_to(ROOT)) + '\\n')", '')
namespace = {'__file__': str(source)}
exec(compile(prefix, str(source), 'exec'), namespace)
OUT = namespace['OUT']
save = namespace['save']
try:
    sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip()
    assert sha == 'e40b09541feb0b7555c475baa82587fdd09e4890'
    save('custody', {'candidate': sha, 'productionTree': subprocess.check_output(['git','rev-parse','HEAD:src/main'],cwd=ROOT/'.worktrees/06-client-identity',text=True).strip(),
                    'reusedDriverSourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                    'fixture': 'Same declared external Clock/Servlet peer/header controls as original QA; candidate production classpath.'})
    namespace['matrix']('candidate')
    namespace['correlated_logs']()
    passed = [item for item in namespace['CHECKS'] if not item['pass']]
    assert not passed
    save('summary', {'pass': True, 'candidate': sha, 'rows': 12,
                     'requests': len(namespace['REQUESTS']), 'assertions': len(namespace['CHECKS']),
                     'appsStopped': len(namespace['APPS']), 'output': str(OUT.relative_to(ROOT))})
    (HERE/'matrix-last-run.txt').write_text(str(OUT.relative_to(ROOT))+'\n')
    print((OUT/'summary.json').read_text())
finally:
    for app in namespace['APPS']:
        if app.process.poll() is None:
            app.stop()
