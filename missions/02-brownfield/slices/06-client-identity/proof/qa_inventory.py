"""Read-only source/report custody and bidirectional test inventory."""
from pathlib import Path
import csv
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[5]
WT = ROOT / '.worktrees/06-client-identity'
OUT = ROOT / 'docs/qa/06-client-identity'
CANDIDATE = 'fb63a88a9b92c1fec97ba74686af1a2f30304160'
BASE = '50ad9c3'
CHAR = '1b4e0a7'


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def save(name, value):
    (OUT / (name + '.json')).write_text(json.dumps(value, indent=2) + '\n')


assert git('-C', str(WT), 'rev-parse', 'HEAD') == CANDIDATE
assert git('-C', str(WT), 'status', '--porcelain') == ''
files = git('ls-tree', '-r', '--name-only', BASE, '--', 'src/functionalTest').splitlines()
unchanged = [{ 'path': path, 'baselineBlob': git('rev-parse', BASE + ':' + path),
               'candidateBlob': git('rev-parse', CANDIDATE + ':' + path)} for path in files]
assert all(row['baselineBlob'] == row['candidateBlob'] for row in unchanged)
assert git('rev-parse', BASE + ':src/main') == git('rev-parse', CHAR + ':src/main') == '50387c729aa575a5eebc505f86c542ed22ff2995'
assert not git('diff', '--name-only', CHAR, '7e23259', '--', 'src/test', 'src/functionalTest')
assert not git('diff', '--name-only', BASE, CANDIDATE, '--', 'build.gradle.kts', 'settings.gradle.kts',
               'src/main/resources/db', 'docs/api/openapi.json')
adaptations = []
for path in ['src/test/java/dev/urlshort/audit/AuditControllerTest.java',
             'src/test/java/dev/urlshort/click/ClickRecorderTest.java',
             'src/test/java/dev/urlshort/web/RateLimitFilterTest.java']:
    old, new = [git('show', sha + ':' + path) for sha in [CHAR, CANDIDATE]]
    expected = old.replace('import dev.urlshort.web.RateLimitFilter;', 'import dev.urlshort.web.ClientIdentity;')
    expected = expected.replace('AuditController.fromLoopback', 'ClientIdentity.fromLoopback')
    expected = expected.replace('RateLimitFilter.clientOf', 'ClientIdentity.clientOf').replace('RateLimitFilter.CLIENT_ATTRIBUTE', 'ClientIdentity.CLIENT_ATTRIBUTE')
    if path.endswith('AuditControllerTest.java'):
        expected = expected.replace('import dev.urlshort.web.Problems.FieldError;', 'import dev.urlshort.web.ClientIdentity;\nimport dev.urlshort.web.Problems.FieldError;')
    assert expected == new, path
    adaptations.append({'path': path, 'referenceOnlyAdaptation': True})
(OUT / 'candidate-source.diff').write_text(subprocess.check_output(['git', 'diff', BASE, CANDIDATE, '--', 'src', 'build.gradle.kts', 'docs/api/openapi.json'], cwd=ROOT, text=True))
save('source-custody', {'candidate': CANDIDATE, 'candidateProductTree': git('rev-parse', CANDIDATE + ':src/main'),
                       'baseline': git('rev-parse', BASE), 'characterization': git('rev-parse', CHAR),
                       'baselineProductTree': git('rev-parse', BASE + ':src/main'),
                       'unchangedOriginalFunctionalFiles': unchanged, 'unitAdaptations': adaptations,
                       'separateProductionMoveHasNoTestChange': True,
                       'noMigrationBuildSettingsOrApiChange': True, 'worktreeClean': True,
                       'impactAnalysisCommit': git('show', '-s', '--format=%H %aI', '6772b68'),
                       'designAndAdrsCommit': git('show', '-s', '--format=%H %aI', '57cb9ae'),
                       'dependentMoveCommit': git('show', '-s', '--format=%H %aI', '7e23259')})

# Mask strings and comments, retaining offsets and braces of Java syntax.
mask = re.compile(r'""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|/\*.*?\*/|//[^\n]*', re.S)
token = re.compile(r'\bclass\s+(\w+)|(@(?:Test|ParameterizedTest|RepeatedTest)\b)|\bvoid\s+(\w+)\s*\(|[{}]')
methods, classes = [], {}
for suite, directory in [('unit', 'src/test/java'), ('functional', 'src/functionalTest/java')]:
    for path in sorted((WT / directory).rglob('*.java')):
        raw = path.read_text()
        clean = mask.sub(lambda match: ' ' * len(match.group()), raw)
        package = re.search(r'\bpackage\s+([\w.]+)', clean).group(1)
        depth, stack, pending_class, annotation = 0, [], None, None
        local = []
        for match in token.finditer(clean):
            if match.group(1):
                pending_class = match.group(1)
            elif match.group(2):
                annotation = match.group(2)[1:]
            elif match.group(3):
                if annotation:
                    clazz = package + '.' + '$'.join(name for name, _ in stack)
                    local.append({'class': clazz, 'method': match.group(3), 'suite': suite,
                                  'source': str(path.relative_to(WT)), 'annotation': annotation})
                    annotation = None
            elif match.group() == '{':
                depth += 1
                if pending_class:
                    stack.append((pending_class, depth))
                    pending_class = None
            else:
                if stack and stack[-1][1] == depth:
                    stack.pop()
                depth -= 1
        assert len(local) == len(re.findall(r'@(?:Test|ParameterizedTest|RepeatedTest)\b', clean)), path
        methods.extend(local)
        outer = re.search(r'\bclass\s+(\w+)(?:\s+extends\s+(\w+))?', clean)
        if outer:
            classes[package + '.' + outer.group(1)] = package + '.' + outer.group(2) if outer.group(2) else None
# Inherited functional invocations are attributed to the actual subclass too.
for clazz, parent in classes.items():
    if parent and not any(row['class'] == clazz for row in methods):
        methods.extend([{**row, 'class': clazz, 'declaredIn': parent} for row in list(methods) if row['class'] == parent])

reports = {}
for suite, subdir in [('unit', 'test'), ('functional', 'functionalTest')]:
    for path in (OUT / 'gate-final/test-results' / subdir).glob('TEST-*.xml'):
        report = ET.parse(path).getroot()
        assert all(int(report.get(key, '0')) == 0 for key in ['failures', 'errors', 'skipped'])
        reports[suite, report.get('name')] = {'path': str(path.relative_to(ROOT)), 'invocations': int(report.get('tests')),
                                             'names': [case.get('name') for case in report.findall('testcase')]}
for row in methods:
    report = reports[row['suite'], row['class']]
    named = any(name.startswith(row['method'] + '(') for name in report['names'])
    assert named or row['annotation'] in ['ParameterizedTest', 'RepeatedTest'], row
    row['attribution'] = 'named XML invocation' if named else 'green class parameterized group; XML display omits method'
    row['report'] = report['path']
    row['rule'] = 'BR-1 / FR-13 inherited regression; prior slice AC numbers remain their originating scope'
    if 'ClientIdentityTest' in row['class']:
        row['rule'] = 'BR-2, BR-3 / NFR-P1, FR-10, NFR-S6; exact identity and audit predicates'
    if row['method'] in ['withoutTheLimitersAttributeThePeerIsHashed', 'anAttributeThatIsNotAStringIsIgnoredAndThePeerIsHashed']:
        row['rule'] = 'BR-5 / FR-7, NFR-P1; internal resolved-client fallback'
    if row['method'] in ['whitespaceOnlyRemoteIpSettingsAreUnsetAndKeepTheEndpointOpen', 'bothRemoteIpSettingsTogetherCloseTheEndpoint']:
        row['rule'] = 'BR-3 / FR-17, NFR-S6; untrimmed internal setting conditions'
    if 'ClientIdentityCharacterizationJourneyTest' in row['class']:
        row['rule'] = 'BR-1–BR-5 / FR-7, FR-8, FR-10, FR-17, NFR-P1, NFR-S6; matrix/settings preservation'

assert set(reports) == {(row['suite'], row['class']) for row in methods}
save('test-inventory', {'candidate': CANDIDATE, 'methods': methods, 'methodMappings': len(methods),
                        'declaredMethods': sum('declaredIn' not in row for row in methods),
                        'classReports': len(reports), 'invocations': {suite: sum(report['invocations'] for (s, _), report in reports.items() if s == suite)
                                                                    for suite in ['unit', 'functional']},
                        'classOnlyMethods': sum('group' in row['attribution'] for row in methods)})

coverage = {}
for suite in ['unit', 'functional', 'all']:
    path = ROOT / ('docs/qa/coverage/06-client-identity/' + suite + '/jacoco' + ('All' if suite == 'all' else 'FunctionalTest' if suite == 'functional' else 'Test') + 'Report.csv')
    if not path.exists():
        path = next((ROOT / ('docs/qa/coverage/06-client-identity/' + suite)).glob('*.csv'))
    rows = list(csv.DictReader(path.open()))
    counts = {key: sum(int(row[key]) for row in rows) for key in ['LINE_MISSED', 'LINE_COVERED', 'BRANCH_MISSED', 'BRANCH_COVERED']}
    coverage[suite] = {**counts, 'linePercent': round(100 * counts['LINE_COVERED'] / (counts['LINE_COVERED'] + counts['LINE_MISSED']), 2),
                       'branchPercent': round(100 * counts['BRANCH_COVERED'] / (counts['BRANCH_COVERED'] + counts['BRANCH_MISSED']), 2),
                       'csv': str(path.relative_to(ROOT))}
assert coverage['all']['LINE_MISSED'] == coverage['all']['BRANCH_MISSED'] == 0
save('coverage-totals', coverage)
print(json.dumps({'filesUnchanged': len(files), 'methodMappings': len(methods),
                  'classOnlyMethods': sum('group' in row['attribution'] for row in methods),
                  'invocations': {suite: sum(report['invocations'] for (s, _), report in reports.items() if s == suite) for suite in ['unit', 'functional']},
                  'coverage': coverage}))
