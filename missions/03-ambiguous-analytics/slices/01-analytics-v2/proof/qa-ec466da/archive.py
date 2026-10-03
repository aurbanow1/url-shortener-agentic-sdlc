"""Retain original capture bytes, then canonicalize display whitespace only."""
import hashlib,json,pathlib,tarfile
P=pathlib.Path(__file__).resolve().parent;ROOT=P.parents[5]
archive=P/'raw-captures.tar.gz'
assert not archive.exists(),'Archive is immutable; do not replace original captures'
paths=[]
for directory in ['http','test-results','v1-test-results','v1-authorized-test-results']:
    paths += [f for f in (P/directory).rglob('*') if f.is_file()]
paths += list(P.glob('*.jsonl'))+list(P.glob('*rows.json'))+[P/'http-ledger.json',P/'head-wire.json',P/'controls.json']
paths += [P/'v1-replay.txt',P/'v1-authorized-replay.txt',P.parent/'qa-check-ec466da.txt',P.parent/'qa-bootjar-ec466da.txt']
manifest={str(f.relative_to(ROOT)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted(set(paths))}
with tarfile.open(archive,'w:gz') as out:
    for path in manifest:out.add(ROOT/path,arcname=path)
(P/'raw-capture-manifest.json').write_text(json.dumps({'archiveSha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'files':manifest,'displayNormalization':'Trailing line whitespace and terminal blank lines only; JSON bodies/logs unchanged in substance. Binary JUnit output is archived without duplicate display copies.'},indent=2)+'\n')
for path in manifest:
    f=ROOT/path
    if f.suffix in ['.headers','.body','.xml','.txt']:
        text=f.read_text();normalized='\n'.join(line.rstrip() for line in text.splitlines()).rstrip()
        f.write_text(normalized+'\n' if normalized else '')
    elif 'binary' in f.parts and f.suffix in ['.bin','.idx']:
        f.unlink()
print(json.dumps({'rawFiles':len(manifest),'archiveSha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'archiveBytes':archive.stat().st_size}))
