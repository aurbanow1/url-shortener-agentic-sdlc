"""Re-derive the historical preparation claims from raw files and pinned Git bytes."""
from pathlib import Path
import csv, hashlib, io, json, re, subprocess, tarfile, xml.etree.ElementTree as E
from collections import Counter
from datetime import datetime, timezone

OUT = Path(__file__).parent
OLD = OUT.parent / 'pre-run-e227acf'
PIN = 'e227acf04e1ea902406532fcebd741f8a86f7f65'
FINAL = '30f8de4e647b05ff54cde09f1019ae519b00069d'
METRIC_COMMIT = '8d3c536'
def git(*args):
    return subprocess.check_output(['git', *args])
def sha(data):
    return hashlib.sha256(data).hexdigest()
def sec(value):
    return int(value + .5) if value >= 0 else int(value - .5)
def dt(value):
    return datetime.fromisoformat(value.replace('Z', '+00:00')).timestamp()
def save(name, value):
    (OUT/name).write_text(json.dumps(value, indent=2) + '\n')
audit = {'historicalProduct': PIN, 'finalCandidate': FINAL, 'metricsCommit': METRIC_COMMIT,
         'auditedAt': datetime.now(timezone.utc).isoformat(), 'limits': []}
manifest = json.loads((OLD/'gate-artifact.json').read_text())
jars = [Path('/private/tmp/urlshort-mission02-e227acf.jar'),
        Path('/private/tmp/urlshort-mission02-image-e227acf.jar')]
audit['oldJars'] = [{'path': str(j), 'bytes': j.stat().st_size, 'sha256': sha(j.read_bytes())} for j in jars]
assert all(j['sha256'] == manifest['jarSha256'] and j['bytes'] == manifest['bytes'] for j in audit['oldJars'])
root = E.parse(OLD/'canonical-jacocoTestReport.xml').getroot()
audit['oldCanonicalCounters'] = {c.get('type'): {k: int(c.get(k)) for k in ('missed','covered')} for c in root.findall('counter')}
assert audit['oldCanonicalCounters']['LINE'] == {'missed': 0, 'covered': 581}
assert audit['oldCanonicalCounters']['BRANCH'] == {'missed': 0, 'covered': 206}
assert 'BUILD SUCCESSFUL' in (OLD/'check-bootjar.txt').read_text()
assert 'SMOKE OK' in (OLD/'container-smoke.txt').read_text()
assert 'SMOKE OK' in (OLD/'rollback-reapplied-smoke.txt').read_text()
audit['oldGateSuiteCounts'] = manifest['suites']
audit['limits'].append('Historical 226/250 suite counts are retained from the manifest; the fresh final XML independently proves 268/322. Historical per-suite XML was not archived by the pre-run.')
deps = json.loads((OLD/'advisories.json').read_text())
assert len(deps['dependencies']) == len(deps['raw']['results']) == 97
assert deps['findings'] == [] and all(not x.get('vulns') for x in deps['raw']['results'])
audit['oldAdvisories'] = {'queriedAt': deps['queriedAt'], 'dependencies': 97, 'findings': 0}
same_paths = ['build.gradle.kts','settings.gradle.kts','gradle','Dockerfile','compose.yaml',
              'src/main/resources/db/migration','scripts/smoke.sh','tools/bench.mjs','.github']
assert not git('diff', '--name-only', PIN, FINAL, '--', *same_paths).strip()
audit['unchangedReuseInputs'] = same_paths
changed = git('diff', '--name-only', PIN, FINAL, '--', 'src').decode().splitlines()
audit['reviewedSourceDelta'] = changed
assert len(changed) == 10
assert not git('diff','--name-only','e40b09541feb0b7555c475baa82587fdd09e4890',FINAL,'--','src').strip()
audit['finalSourceEqualsReviewedE40'] = True
bench = (OLD/'bench.txt').read_text()
audit['benchmarkRawTextSha256'] = sha(bench.encode())
audit['limits'].append('Benchmark counts and printed quantiles re-read from bench.txt. No per-request latency sample archive exists, so quantiles are not independently recomputed; no final-SHA load rerun is claimed.')
base = Path('/private/tmp/urlshort-m02-rollback-e227acf')
stages = {}
with tarfile.open(OUT/'rollback-raw-csv.tar.gz', 'w:gz') as archive:
    for stage in ('before','after-v4','after-v3','reapplied'):
        data = {}
        for f in sorted((base/stage).glob('*.csv')):
            raw = f.read_bytes()
            rows = list(csv.reader(io.StringIO(raw.decode())))
            data[f.name] = {'sha256': sha(raw), 'dataRows': len(rows)-1, 'header': rows[0]}
            archive.add(f, arcname=stage+'/'+f.name)
        versions = list(csv.DictReader((base/stage/'flyway.csv').open()))
        data['versions'] = [r.get('version', r.get('VERSION')) for r in versions if r.get('version',r.get('VERSION'))]
        stages[stage] = data
assert stages['before']['versions'] == ['1','2','3','4']
assert stages['after-v4']['versions'] == ['1','2','3']
assert stages['after-v3']['versions'] == ['1','2']
assert stages['reapplied']['versions'] == ['1','2','3','4']
for f in ('link.csv','audit_log.csv','click.csv','user_agent_class.csv'):
    assert len({stages[s][f]['sha256'] for s in ('before','after-v4','after-v3')}) == 1
for f, added in [('link.csv',1),('audit_log.csv',2),('click.csv',1)]:
    assert stages['reapplied'][f]['dataRows'] == stages['before'][f]['dataRows'] + added
audit['rollbackStages'] = stages

# Re-scan exact old tracked blobs, batching Git reads. Never output matched values.
scan = json.loads((OLD/'secret-scan.json').read_text())
entries = git('ls-tree','-r','-z',PIN).split(b'\0')
items = [e.split(b'\t',1) for e in entries if e]
request = b''.join(meta.split()[2]+b'\n' for meta, name in items)
raw = subprocess.run(['git','cat-file','--batch'],input=request,capture_output=True,check=True).stdout
stream = io.BytesIO(raw)
text_count, binary_count, findings = 0, 0, []
patterns = {k: re.compile(v) for k,v in scan['patterns'].items()}
for meta, name in items:
    size = int(stream.readline().split()[2])
    content = stream.read(size); assert stream.read(1) == b'\n'
    try:
        value = content.decode('utf-8')
        if '\0' in value: raise UnicodeDecodeError('utf-8',b'\0',0,1,'binary')
    except UnicodeDecodeError:
        binary_count += 1; continue
    text_count += 1
    for label, pattern in patterns.items():
        if pattern.search(value): findings.append({'path': name.decode(), 'pattern': label})
audit['oldSecretRescan'] = {'textFiles': text_count, 'binaryFiles': binary_count, 'findings': findings}
assert text_count == scan['textFilesChecked'] and binary_count == len(scan['binaryFilesNotScanned']) and not findings

# Independent historical metric calculation against the committed raw snapshot.
paths = git('ls-tree','-r','--name-only',METRIC_COMMIT,'--','docs/evidence').decode().splitlines()
paths = [p for p in paths if re.search(r'/(instances/[^/]+\.trace|packets/[^/]+\.transitions)\.json$',p)]
paths.append('docs/metrics/metrics.json')
archive_bytes = git('archive',METRIC_COMMIT,'--',*paths)
(OUT/'historical-metrics-8d3c536-inputs.tar.gz').write_bytes(__import__('gzip').compress(archive_bytes,mtime=0))
records = {}
with tarfile.open(fileobj=io.BytesIO(archive_bytes)) as archive:
    for member in archive.getmembers():
        if member.isfile(): records[member.name] = json.load(archive.extractfile(member))
expected = records['docs/metrics/metrics.json']
now = dt(expected['totals']['generatedAt'])
traces, transitions = {}, {}
for p in sorted(records):
    if p.endswith('.trace.json'):
        v=records[p]; traces[v['instance']['instanceId']] = v
    if p.endswith('.transitions.json'): transitions[Path(p).name.removesuffix('.transitions.json')] = records[p]
matches, differences = [], []
for row in expected['instances']:
    trace = traces[row['instanceId']]
    inst, trail = trace['instance'], trace.get('trail',[])
    failed = [t for t in trail if t['closureReason'] == 'failed']
    counts = Counter(t['stepId'] for t in trail if t['closureReason'] != 'waiting')
    retries = len(failed)+sum(max(0,n-1) for n in counts.values())
    repairs = []
    for failure in failed:
        fixed = next((t for t in trail if t['stepId']==failure['stepId'] and dt(t['closedAt'])>dt(failure['closedAt']) and t['closureReason']!='failed'),None)
        if fixed: repairs.append(dt(fixed['closedAt'])-dt(failure['closedAt']))
    rollback = sum(bool(re.search(r'revert|rollback',(t.get('closureEvidence') or {}).get('evidence_ref') or '',re.I)) for t in trail)+inst.get('resumeCount',0)
    waits = 0
    for packet in row['packets']:
        seq = sorted(transitions[packet['qitemId']],key=lambda t:dt(t['ts']))
        note_count = sum(bool(re.search(r'revert|rollback|rolled back',t.get('transitionNote',''),re.I)) for t in seq)
        if note_count != packet['rollbackNotes']:
            differences.append({'instance':row['instanceId'],'packet':packet['qitemId'],'field':'rollbackNotes','committedRaw':note_count,'historicalMetric':packet['rollbackNotes']})
        rollback += note_count
        wait = 0
        for i,t in enumerate(seq):
            if t['state']=='blocked' and (t.get('blockedOn')=='human@kernel' or t.get('transitionNote','').startswith('workflow gate: parked on human@kernel')):
                end=next((u for u in seq[i+1:] if u.get('actorSession')=='human@kernel' or u['state']!='blocked'),None)
                wait += (dt(end['ts']) if end else now)-dt(t['ts'])
        if sec(wait) != packet['humanWaitSec']:
            differences.append({'instance':row['instanceId'],'packet':packet['qitemId'],'field':'humanWaitSec','committedRaw':sec(wait),'historicalMetric':packet['humanWaitSec']})
        waits += sec(wait)
    actual = {'stepClosures':len(trail),'failedClosures':len(failed),'reentries':retries-len(failed),'retries':retries,
              'rollbacks':rollback,'humanWaitSec':waits,'mttrSec':sec(sum(repairs)/len(repairs)) if repairs else None,
              'e2eLatencySec':sec((dt(inst['completedAt']) if inst.get('completedAt') else now)-dt(inst['createdAt']))}
    differences.extend({'instance':row['instanceId'],'field':k,'committedRaw':v,'historicalMetric':row[k]} for k,v in actual.items() if row[k]!=v)
    matches.append({'instanceId':row['instanceId'],**actual})
audit['historicalMetrics']={'generatedAt':expected['totals']['generatedAt'],'historicalTotals':expected['totals'],'rederivedCommittedRows':matches,'differences':differences,'inputCompleteness': 'The historical generator labels every row 03-ambiguous-analytics, but git ls-tree at 8d3c536 contains no committed docs/evidence/03-ambiguous-analytics raw export. Its then-present uncommitted inputs are not recoverable from that commit. Do not claim exact historical reproduction or reuse these totals as final release metrics.'}
audit['limits'].append('Historical metrics keep their export-directory labels, text-match rollback heuristic, omitted custom human waits and mean-of-means/non-failed MTTR limitations; no production incident rate inferred.')
save('prerun-revalidation.json',audit)
print(json.dumps({'oldJarMatched':True,'rollbackStagesMatched':True,'oldSecretRescan':audit['oldSecretRescan'],
                  'historicalMetricCommittedRowsRechecked':len(matches),'historicalDifferences':differences,'reviewedSourceDeltaPaths':len(changed)},indent=2))
