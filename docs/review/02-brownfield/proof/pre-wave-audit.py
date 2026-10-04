"""Read-only reconciliation of the assigned, pinned wave pre-review."""
from pathlib import Path
import collections
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[4]
W = ROOT / '.worktrees/review-wave02-ed2b940'
OUT = ROOT / 'docs/review/02-brownfield/proof'

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)

def blob(ref, path):
    return git('show', f'{ref}:{path}')

def paths(base, end):
    return git('diff', '--name-only', base, end).decode().splitlines()

def sha(data):
    return hashlib.sha256(data).hexdigest()

changed = paths('8e9c065', 'ed2b940')
selected = [p for p in changed if p.startswith(('src/', '.github/', 'scripts/', 'tools/', 'gradle/'))
            or p in ['README.md', 'docs/api/openapi.json', 'gradlew.bat']]
assert len(selected) == 43
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=W).strip() == git('rev-parse', 'ed2b940').strip()
assert not subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=no'], cwd=W).strip()
result = {'base': git('rev-parse', '8e9c065').decode().strip(),
          'candidate': git('rev-parse', 'ed2b940').decode().strip(),
          'changed_path_count': len(changed),
          'changed_paths_by_root': dict(collections.Counter(p.split('/')[0] for p in changed)),
          'reviewed_product_tool_paths': [{'path': p, 'sha256': sha(blob('ed2b940', p))} for p in selected]}
groups = {
    'audit': ('7ac8af5', [p for p in selected if '/audit/' in p]),
    'dogfood': ('4fe7042', ['docs/api/openapi.json', 'src/main/java/dev/urlshort/web/OpenApiConfig.java',
        'src/main/java/dev/urlshort/web/MetricsConfig.java', 'src/test/java/dev/urlshort/web/MetricsConfigTest.java',
        'src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java',
        'src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java']),
    'ci': ('add7ab5', [p for p in selected if p.startswith('.github/')]),
    'retention': ('a8fc8b6', [p for p in paths('16312da', 'a8fc8b6') if p not in ['README.md','src/main/resources/application.properties']])
}
result['prior_candidate_blob_equality'] = {}
for name, (ref, files) in groups.items():
    assert all(blob(ref,p) == blob('ed2b940',p) for p in files), name
    result['prior_candidate_blob_equality'][name] = {'candidate': ref, 'equal_paths': files}
result['tests'] = {}
for suite in ['test','functionalTest']:
    reports = [ET.parse(p).getroot() for p in (W/'build/test-results'/suite).glob('TEST-*.xml')]
    sums = {k:sum(int(r.get(k,'0')) for r in reports) for k in ['tests','failures','errors','skipped']}
    assert reports and not any(sums[k] for k in ['failures','errors','skipped'])
    result['tests'][suite] = sums
result['jacoco_exec_inputs'] = sorted(p.name for p in (W/'build/jacoco').glob('*.exec'))
assert result['jacoco_exec_inputs'] == ['functionalTest.exec','test.exec']
result['coverage'] = {}
for suite in ['test','functionalTest','all']:
    report = ET.parse(W/f'build/reports/jacoco/{suite}/jacocoTestReport.xml').getroot()
    result['coverage'][suite] = {c.get('type'): {k:int(c.get(k)) for k in ['missed','covered']}
        for c in report.findall('counter') if c.get('type') in ['LINE','BRANCH']}
assert all(c['missed'] == 0 for c in result['coverage']['all'].values())
committed = json.loads(blob('ed2b940','docs/api/openapi.json'))
assert committed == json.loads((W/'build/openapi/openapi.json').read_text())
assert '/api/audit' in committed['paths']
assert 'errors' in committed['components']['schemas']['ProblemDetail']['properties']
assert 'properties' not in committed['components']['schemas']['ProblemDetail']['properties']
result['merged_openapi'] = 'live equals committed; audit path and corrected problem schema coexist'
props = blob('ed2b940','src/main/resources/application.properties').decode()
for line in ['server.forward-headers-strategy=none','urlshort.click.retention-days=90','urlshort.click.purge-enabled=true']:
    assert line in props, line
assert 'urlshort.click.purge-enabled=false' in blob('ed2b940','src/functionalTest/resources/application-functional.properties').decode()
result['merged_properties'] = 'NONE guard pin, 90-day/default-on purge and functional hold coexist'
assert not git('diff','8e9c065','ed2b940','--','build.gradle.kts','settings.gradle.kts','Dockerfile','compose.yaml','gradlew').strip()
result['unchanged_build_surfaces'] = ['build.gradle.kts','settings.gradle.kts','Dockerfile','compose.yaml','gradlew']
wrapper = blob('ed2b940','gradle/wrapper/gradle-wrapper.jar')
cache = list((ROOT/'.gradle-home/wrapper/dists').rglob('gradle-wrapper-main-9.8.0.jar'))
assert len(cache)==1
with zipfile.ZipFile(cache[0]) as z:
    assert wrapper == z.read('gradle-wrapper.jar')
result['wrapper_jar'] = {'sha256': sha(wrapper), 'matches_cached_9_8_distribution': True,
                         'online_provenance_verified_here': False}
assert not git('diff','--ignore-cr-at-eol','9bbf6e5^','9bbf6e5','--','gradlew.bat').strip()
result['bat_normalization'] = '9bbf6e5 has no diff ignoring CR at EOL; Windows execution not performed'
(OUT/'retention-range-diff.txt').write_bytes(git('range-diff','16312da..a8fc8b6','2ead709..a2c34c1'))
(OUT/'pre-wave-audit-ed2b940.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'candidate':result['candidate'],'selected_paths':len(selected),'tests':result['tests'],
                  'coverage':result['coverage']['all'],'blob_comparisons':{k:len(v['equal_paths']) for k,v in result['prior_candidate_blob_equality'].items()}}))
