"""Independent jar drain with a supported host locale; no product changes."""
from pathlib import Path
import os,socket,subprocess,sys
root=Path.cwd();wt=root/'.worktrees/03-operate';proof=root/'missions/01-greenfield-core/slices/03-operate/proof'
with socket.socket() as s:
 s.bind(('127.0.0.1',0));port=s.getsockname()[1]
p=subprocess.run([str(wt/'scripts/smoke.sh'),'--drain',str(wt/'build/libs/urlshort.jar'),str(port)],cwd=wt,text=True,capture_output=True,timeout=90,env={**os.environ,'LC_ALL':'C','LC_CTYPE':'C','LANG':'C'})
(proof/'qa-drain-1c8b2cf.txt').write_text(p.stdout+p.stderr)
print(p.stdout+p.stderr)
sys.exit(p.returncode)
