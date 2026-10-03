"""Reconcile source annotations, executed JUnit invocations and requirement mappings."""
from pathlib import Path
import re, json, xml.etree.ElementTree as E

root = Path.cwd()
wt = root / '.worktrees/03-operate'
proof = root / 'missions/01-greenfield-core/slices/03-operate/proof'
sha = '1c8b2cff20ad8b73a060bc817c8d0011782f876f'
sources = []
parameterized_sources = set()
for suite in ['test', 'functionalTest']:
    for f in sorted((wt / 'src' / suite / 'java').rglob('*.java')):
        source = f.read_text()
        package = re.search(r'package\s+(\S+);', source).group(1)
        methods = re.findall(r'@(?:Test|ParameterizedTest)\b(?:(?!\bvoid\b)[\s\S])*?\bvoid\s+(\w+)\s*\(', source)
        sources.extend((suite, package + '.' + f.stem + '#' + m) for m in methods)
        parameterized_sources.update((suite, package + '.' + f.stem + '#' + m) for m in
                                     re.findall(r'@ParameterizedTest\b(?:(?!\bvoid\b)[\s\S])*?\bvoid\s+(\w+)\s*\(', source))

old = json.loads((proof / 'qa-traceability-a7c533f.json').read_text())
rows = [[x.replace('a7c533f', '1c8b2cf') for x in r] for r in old['methods']
        if not r[1].endswith('#aBackwardClockStepStartsTheClientFreshInsteadOfLockingItOut')]
new = [
    ['AC-1, AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2; CR-01/SEC-01 ordering regression',
     'web.RateLimiterTest#aRequestOvertakenByNewerOnesDecidesOnTheTimeItReachesTheBucket',
     'unit', 'PASS on 1c8b2cf; unchanged reviewer probe confirms 60 admissions'],
    ['BR-2; NFR-R2; disclosed fail-closed backward-clock limit, lead transition 726',
     'web.RateLimiterTest#afterABackwardClockStepTheBucketRefillsFromItsStoredTat',
     'unit', 'PASS on 1c8b2cf; Retry-After 61 after 60s rollback'],
    ['BR-8; NFR-R2; CR-03 rollback cleanup regression',
     'web.RateLimiterTest#theReleaseResumesAfterABackwardClockStep',
     'unit', 'PASS on 1c8b2cf; reviewer probe confirms 2 clients after 61s'],
]
idx = next(i for i, r in enumerate(rows) if r[1].endswith('#fullBucketsAreReleasedByTheNextRequestButNotWhileIdle'))
rows[idx:idx] = new
mapped = {(('test' if r[2] == 'unit' else 'functionalTest'), 'dev.urlshort.' + r[1]) for r in rows}
assert len(mapped) == len(rows) == len(sources) == 186, (len(mapped), len(rows), len(sources))
assert mapped == set(sources), (mapped - set(sources), set(sources) - mapped)

invocations = []
totals = {}
# Gradle XML retains parameterized display labels but drops the owning method.
# These groups were matched to the source providers, argument names and sizes.
parameterized_groups = {
    'dev.urlshort.click.ClickTest': [
        ('anAbsentOrEmptyUserAgentIsUnknown', 2), ('aReferrerThatIsNotAnHttpOriginIsNone', 7),
        ('theReferrerIsReducedToItsOrigin', 9), ('theUserAgentIsReducedToAClass', 7), ('theReferrerLengthCapIs2048', 2)],
    'dev.urlshort.link.LinkValidationTest': [
        ('malformedKeysFailFormat', 4), ('eachRejectedUrlFailsExactlyItsRule', 20), ('validUrlsPass', 5),
        ('keysLongerThan255FailFormat', 2), ('absentOrVisibleAsciiKeysPass', 4), ('exactly2048CharactersIsAccepted', 1)],
    'dev.urlshort.web.RateLimitFilterTest': [
        ('rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy', 2),
        ('rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop', 8),
        ('rule1_operatorSurfacesAreNeitherChargedNorLimited', 7), ('rule1_limitedRequestsAreChargedToTheirBudget', 12)],
    'dev.urlshort.click.ClickRecordingJourneyTest': [
        ('AC04_theUserAgentIsStoredAsAClassOnly', 7), ('AC03_theReferrerIsStoredAsItsOriginOnly', 7)],
    'dev.urlshort.click.StatsJourneyTest': [('AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails', 4)],
    'dev.urlshort.link.IdempotencyJourneyTest': [('AC20_aMalformedKeyIsRefused', 4)],
    'dev.urlshort.link.LinkCreateJourneyTest': [
        ('AC05_bodyThatIsNotAJsonObjectIsRefused', 4), ('AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule', 17)],
    'dev.urlshort.link.LinkReadRetireJourneyTest': [('AC14_unknownCodeIs404OnEveryLinkOperation', 5)],
    'dev.urlshort.web.ObservabilityJourneyTest': [
        ('AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId', 12),
        ('rule8_problemBodiesAndLogsNeverEchoASubmittedValue', 6)],
}
executed_sources = set()
executed_parameterized = set()
for suite in ['test', 'functionalTest']:
    count = 0
    for f in sorted((wt / 'build/test-results' / suite).glob('TEST-*.xml')):
        tests = E.parse(f).getroot()
        for key in ['failures', 'errors', 'skipped']:
            assert int(tests.attrib.get(key, 0)) == 0
        groups = []
        for tc in tests.findall('testcase'):
            if tc.attrib['name'].startswith('[1]'):
                groups.append([])
            if tc.attrib['name'].startswith('['):
                groups[-1].append(tc)
        owners = {}
        if groups:
            expected = parameterized_groups[tests.attrib['name']]
            assert len(groups) == len(expected)
            for group, (method, size) in zip(groups, expected):
                assert len(group) == size, (tests.attrib['name'], method, len(group), size)
                executed_parameterized.add((suite, tests.attrib['name'] + '#' + method))
                owners.update((id(tc), method) for tc in group)
        for tc in tests.findall('testcase'):
            name = tc.attrib['classname'] + '#' + tc.attrib['name']
            method = owners.get(id(tc), tc.attrib['name'].split('(', 1)[0])
            assert (suite, tc.attrib['classname'] + '#' + method) in mapped, name
            executed_sources.add((suite, tc.attrib['classname'] + '#' + method))
            invocations.append(suite + ' ' + name + ' -> ' + tc.attrib['classname'] + '#' + method)
            count += 1
    totals[suite] = count
assert totals == {'test': 165, 'functionalTest': 155}, totals
assert executed_sources == mapped
assert executed_parameterized == parameterized_sources
functional_acs = set()
for r in rows:
    if r[2] == 'functional' and not r[0].startswith('Inherited'):
        functional_acs.update(int(n) for n in re.findall(r'AC-(\d+)', r[0]))
assert set(range(1, 21)).issubset(functional_acs), functional_acs

release = old['release']
for r in release:
    if r[0].startswith('AC-25;'):
        r[3] = 'Supplemental unmodified jar PASS: complete R0 201, curl exit 0 at 532 ms; refused probe; 62 ok / 16 refused / 0 losses / 0 failures. Strict R0 controls 8/8; supported C locale; release record pending'
    if r[0].startswith('AC-27;'):
        r[3] = '60s mode exercised: achieved 82.1 / 16.4 req/s, below specified 100/20; numeric judgment PENDING'
    if r[0].startswith('AC-22;') or r[0].startswith('AC-23;'):
        r[3] = 'PENDING runtime; configuration identical to previously inspected a7c533f'

(proof / 'qa-source-methods-1c8b2cf.txt').write_text(''.join(s + ' ' + m + '\n' for s, m in sources))
(proof / 'qa-test-invocations-1c8b2cf.txt').write_text('\n'.join(invocations) + '\n')
(proof / 'qa-traceability-1c8b2cf.json').write_text(json.dumps({
    'candidate': sha, 'methods': rows, 'release': release,
    'source_methods': len(sources), 'invocations': totals,
}, indent=2) + '\n')
section = '\n## Re-check 03-operate — candidate ' + sha + '\n\n'
section += ('Independent QA, 2026-10-03 UTC: 165 unit / 155 functional invocations, zero failures/errors/skips. '
            'All 186 source methods (88 unit, 98 functional) map below and every invocation resolves to a mapped method. '
            'All AC-1–AC-20 have a fresh functional test and independent HTTP observation on this SHA. '
            'Inventories: slice proof/qa-source-methods-1c8b2cf.txt and qa-test-invocations-1c8b2cf.txt. '
            'The granted shifted-clock requests retain all their former assertions and timing; only their dedicated peers change.\n\n')
section += '| AC / business rule; product requirement | Test class#method | Suite | Result |\n|---|---|---|---|\n'
section += ''.join('| ' + ' | '.join(r) + ' |\n' for r in rows)
section += '\nLocked SPEC release checks:\n\n| AC / rule; requirement | Release check | Level | Result |\n|---|---|---|---|\n'
section += ''.join('| ' + ' | '.join(r) + ' |\n' for r in release)
section += ('\nBR-1 through BR-13 remain represented; BR-12/13 require release evidence. '
            'No product source or test changed by QA. Backward Clock steps are outside the contract and fail closed under the lead decision; '
            'the sweep margin for arbitrary delayed concurrent clock reads was not stress-tested.\n')
trace = root / 'docs/qa/TRACEABILITY.md'
if '\n## Re-check 03-operate — candidate ' + sha in trace.read_text():
    assert section in trace.read_text(), 'Existing section differs from reconciled evidence'
else:
    with trace.open('a') as f:
        f.write(section)
print('Trace complete:', len(rows), 'methods;', totals, 'invocations;', len(functional_acs), 'current functional ACs')
