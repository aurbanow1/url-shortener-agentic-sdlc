"""Build QA records from captured evidence; shared-file appends are separate."""
from pathlib import Path
import hashlib
import json
import uuid

ROOT = Path(__file__).resolve().parents[5]
OUT = ROOT / 'docs/qa/06-client-identity'
PROOF = Path(__file__).resolve().parent
SHA = 'fb63a88a9b92c1fec97ba74686af1a2f30304160'
RUN = ROOT / (PROOF / 'qa-last-run.txt').read_text().strip()
blank = ROOT / (OUT / 'blank-settings/last-run.txt').read_text().strip()
inventory = json.loads((OUT / 'test-inventory.json').read_text())
coverage = json.loads((OUT / 'coverage-totals.json').read_text())
hashes = json.loads((OUT / 'gate-final/coverage-hashes.json').read_text())
for path, digest in hashes.items():
    assert hashlib.sha256((ROOT / 'docs/qa/coverage/06-client-identity' / path).read_bytes()).hexdigest() == digest
assert len(hashes) == 378
requests = [json.loads(line) for line in (RUN / 'requests.jsonl').read_text().splitlines()]
assert len(requests) == 2041
for request in requests:
    assert str(uuid.UUID(request['headers']['x-request-id'])) == request['headers']['x-request-id']
    if request['name'] in ['default-create-061', 'default-redirect-601']:
        assert request['status'] == 429 and int(request['headers']['retry-after']) >= 1
checks = [json.loads(line) for line in (RUN / 'assertions.jsonl').read_text().splitlines()]
assert sum(item['pass'] for item in checks) == 10380
assert sum(item['pass'] and item['claim'].endswith('stopped/port closed') for item in checks) == 19

prefix = 'dev.urlshort.web.ClientIdentityCharacterizationJourneyTest$'
primary = {
 1: [('dev.urlshort.link.RedirectJourneyTest', 'AC12_visitorIsRedirectedWithANonCacheable302')],
 2: [('dev.urlshort.link.RedirectJourneyTest', 'AC13_aRetiredLinkTellsTheVisitorItIsGone'),
     ('dev.urlshort.link.LinkReadRetireJourneyTest', 'AC14_unknownCodeIs404OnEveryLinkOperation'),
     ('dev.urlshort.link.LinkReadRetireJourneyTest', 'AC15_wrongMethodIs405'),
     ('dev.urlshort.link.LinkCreateJourneyTest', 'AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule')],
 3: [(prefix+'ShippedBudgets', 'untrustedForwardingHeadersCannotEvadeTheShippedCreateBudget'),
     (prefix+'ShippedBudgets', 'untrustedForwardingHeadersCannotEvadeTheShippedRedirectBudget')],
 4: [(prefix+'SharedBudgets','theRowSpendsTheReferencePeersCreateBudget'),
     (prefix+'SharedBudgets','theRowSpendsTheReferencePeersRedirectBudget'),
     (prefix+'UntrustedBudgets','forwardingHeadersSpendThePeersCreateBudget'),
     (prefix+'UntrustedBudgets','forwardingHeadersSpendThePeersRedirectBudget')],
 5: [(prefix+'ShippedBudgets','withoutATrustedProxyForwardingHeadersDoNotChangeTheClickClient'),
     (prefix+'TrustedProxies','theClickClientIsTheChargedClient')],
 6: [(prefix+'ShippedSettings','aHeaderlessLoopbackPeerReadsTheAuditTrail'),
     (prefix+'TrustedAuditPeers','aListedLoopbackPeerStillReadsTheAuditTrail')],
 7: [(prefix+'ShippedSettings','anyOtherPeerIsRefusedTheAuditTrail'),
     (prefix+'TrustedAuditPeers','aListedNonLoopbackPeerIsStillRefused')],
 8: [(prefix+'ShippedSettings','anyForwardingHeaderClosesTheAuditTrail'),
     (prefix+'TrustedAuditPeers','anyForwardingHeaderFromAListedPeerClosesTheAuditTrail')],
 9: [(prefix+'AddressRewritingSettings','anAddressRewritingSettingClosesTheAuditTrail'),
     (prefix+'AddressRewritingSettings','bothRemoteIpHeaderSettingsTogetherCloseTheAuditTrail'),
     (prefix+'AddressRewritingSettings','whitespaceOnlyRemoteIpHeaderSettingsKeepTheAuditTrailOpen'),
     (prefix+'AddressRewritingSettings','theShippedSettingKeepsTheAuditTrailOpenToADirectLoopbackClient')],
 10: [(prefix+'ShippedSettings','aRefusedAuditReadIsRefusedBeforeValidationAndNegotiation'),
      (prefix+'UntrustedBudgets','anEmptyBudgetIsAnsweredBeforeTheAuditGuardValidationAndNegotiation')],
 11: [('dev.urlshort.audit.AuditReadJourneyTest','AC09_invalidPagingParametersAreRefusedNamingTheField'),
      ('dev.urlshort.audit.AuditReadFailureJourneyTest','AC21_aFailedReadIsA500ProblemNeverAnEmptyOrPartialPage')],
 12: [('dev.urlshort.click.TrustedProxyClickJourneyTest','AC12_trustedProxyRedirectsLogNoForwardedValueOrAddress'),
      ('dev.urlshort.click.ClickRecordingJourneyTest','AC18_noClickDataReachesTheLogs'),
      ('dev.urlshort.audit.AuditReadJourneyTest','AC16_auditContentAndClientValuesStayOutOfTheLogs')],
 13: [('dev.urlshort.click.ClickResilienceJourneyTest','AC14_aSlowClickStoreDoesNotSlowTheRedirect'),
      ('dev.urlshort.click.ClickResilienceTrustedProxyJourneyTest','AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn'),
      ('dev.urlshort.click.ClickResilienceTrustedProxyJourneyTest','AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse'),
      ('dev.urlshort.click.ClickRecordingJourneyTest','AC02_onlyARedirectIsAClick')],
 14: [('dev.urlshort.web.ObservabilityJourneyTest','AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId'),
      ('dev.urlshort.audit.AuditReadJourneyTest','AC15_requestCorrelationOnTheNewPaths')],
 15: [('dev.urlshort.web.OpenApiDocumentTest','NFRM3_committedDocumentEqualsTheLiveOne')]
}
requirements = {1:'FR-13',2:'FR-13',3:'FR-10, NFR-R2',4:'FR-10, NFR-R2',5:'FR-7, FR-8, NFR-P1',
                6:'FR-17, NFR-S6',7:'FR-17, NFR-S6',8:'FR-17, NFR-S6',9:'FR-17, NFR-S6',
                10:'FR-17, FR-10, NFR-S6',11:'FR-17, NFR-R6',12:'NFR-P1, NFR-O2',
                13:'FR-7, FR-8, FR-13',14:'NFR-O1, NFR-O2',15:'NFR-M3'}
by_name = {(row['class'],row['method']):row for row in inventory['methods']}
trace = ['\n## 06-client-identity — candidate '+SHA+'\n',
         'Independent fresh gate268 unit/322 functional; 321 declared test methods plus five inherited subclass mappings =326 mappings across74 XML class reports. Every invocation is green. 56 parameterized source methods have only class-level XML attribution; this is labelled, not invented method evidence. Source/report inventory: `docs/qa/06-client-identity/test-inventory.json`.\n',
         '| AC | Requirement id | Test class#method | Suite | Result |', '|---|---|---|---|---|']
for ac, names in primary.items():
    for name in names:
        row = by_name[name]
        assert row['suite'] == 'functional'
        trace.append('| AC-'+str(ac)+' | '+requirements[ac]+' | '+name[0]+'#'+name[1]+' | functional | PASS; '+row['attribution']+'; own effect recorded in QA README |')
trace += ['\nAC-9 also retains unchanged `AuditAccessSettingsJourneyTest` cloud/CR-01 coverage; the real original/candidate jars independently exercise remote-IP, protocol, both, native, framework, cloud and all individual/combined blank settings. AC-11 wrong method is independently observed405 in core captures and unchanged audit/OpenAPI journeys. The full reverse inventory below covers all methods, including inherited regressions whose originating AC numbers must not be read as this slice\'s numbers. BR-1 is this SPEC\'s preservation responsibility; prior exact mappings remain in their originating tables above.\n',
          '| Test class#method | Suite | This SPEC business rule / requirement | Result |', '|---|---|---|---|']
for row in inventory['methods']:
    trace.append('| '+row['class']+'#'+row['method']+' | '+row['suite']+' | '+row['rule']+' | PASS; '+row['attribution']+' |')
(OUT/'traceability-append.md').write_text('\n'.join(trace)+'\n')

summary = ['# Coverage and QA — 06-client-identity\n',
           'QA PASS on exact `'+SHA+'` (QA2, independent Codex). Fresh `../../scripts/gw --log <capture> --offline check --rerun-tasks`:268 unit +322 functional =590 passing invocations; zero failures/errors/skips; Javadoc and coverage verification green.\n',
           '| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |',
           '|---|---:|---:|---:|---:|---:|']
for suite, row in coverage.items():
    summary.append('| '+suite+' | '+str(row['LINE_COVERED'])+' / '+str(row['LINE_COVERED']+row['LINE_MISSED'])+' | '+str(row['linePercent'])+' | '+str(row['BRANCH_COVERED'])+' / '+str(row['BRANCH_COVERED']+row['BRANCH_MISSED'])+' | '+str(row['branchPercent'])+' |')
summary += ['\nTotals are sums of each copied CSV, not averages of suite percentages. Overall report merges only fresh `test.exec` and `functionalTest.exec`; no shipped-replay execution file. All378 HTML/XML/CSV/report-resource hashes rechecked against the saved fresh outputs. No new exclusion or threshold change.\n',
            'Logs: `docs/qa/06-client-identity/check-fb63a88-diagnostic-02.txt`; XML/custody: `gate-final/`; first red retained in `gate-attempt-01/` and `localhost-collision.md`. Independent original-production characterization gate also268/322 green at1b4e0a7.\n',
            'Every AC observed by effect:2041 controlled HTTP responses across original/candidate,10380 passing outcome checks,1017 exact response/log pairs plus two separately scoped metric/privacy comparisons per lane; 59+59 unmodified-jar responses/336 assertions per run;72 actual blank-setting jar responses/372 assertions. All51 apps stopped, counting the ten pre-work baseline apps; shutdown is bounded process/port observation, not an additional in-flight shutdown guarantee.\n',
            'See `docs/qa/06-client-identity/README.md`, TRACEABILITY and GAPS for fixtures, comparisons and qualifications. Rule6 substitution dictionaries are retained. The original broad metric comparator failure remains visible; runtime telemetry and the existing springdoc duration measurement are explicitly reported outside AC-14. No promised HTTP/log/access/grouping difference remains.\n',
            '**Proof16 remains pending:** lead confirmed qitem-20261004015644-b1493e3f/transition1882. Independent review, merge and design-owner guidance updates return as a durable QA item. This PASS covers qa_check, not future merge/lifecycle completion.\n']
(ROOT/'docs/qa/coverage/06-client-identity/SUMMARY.md').write_text('\n'.join(summary))

gaps = '''
## 06-client-identity — QA2, candidate fb63a88a9b92c1fec97ba74686af1a2f30304160

| Gap / qualification | Why | Compensating check / disposition |
|---|---|---|
| None for merged coverage or required AC effects |584/584 lines206/206 branches; all15 ACs observed| No new exclusion, lower threshold or unobserved acceptance outcome |
| Unit87.33% lines96.12% branches; functional94.18% lines83.50% branches | Gate requires merged100/100; suites complement | CSV sums and378 copied report hashes verified |
| Controlled servlet peers and exact whitespace request-header values | Host cannot bind127.0.0.2; curl omits all-space headers; simulated peers do not prove container rewrite safety | External BEFORE-limiter filter supplies explicit Servlet inputs; all other headers and responses use real HTTP. Separate unmodified jars cover actual connection CR-01/cloud and blank settings; empty forwarding header wire trace is retained. No physical nonlocal client or whitespace wire-header claim |
|56 parameterized methods have class-only XML attribution | JUnit display names omit their method |326 mappings across74 reports;321 declarations plus five inherited methods. All590 invocations green; exact source/report inventory retained |
| First candidate gate had two401s from another listener | Pre-existing pgAdmin IPv4 listener occupied50898; failed Tomcat wildcard requests reached another service. No simultaneous listener snapshot while failed test alive | Original failures retained, same candidate isolated19-test class and fresh full590 gate passed. Owner process untouched. Lead records LOW localhost collision backlog outside slice; no QA/toolchain fix or waiver |
| Broad final metric comparator rejected two scrapes | Runtime values differ, baseline has extra health503 series and candidate an existing JVM concurrent-GC phase meter; neither is an AC-12 equality promise | Preserve original false comparator and all differing names/tags/values; scoped saved analysis checks business values and metric privacy. Health503 startup-poll attribution is an inference (raw readiness probe responses were not captured). All1017 non-metric HTTP/log pairs are exactly equal under Rule6 |
| Springdoc duration message293ms versus162ms | Existing API initialization measurement varies | Retain both INFO messages/one extra event; all other fields/events and live API body match. It is outside AC-14's named audit200 comparison, whose events match exactly; no log message normalization |
| External mutable Clock, JDBC and held writer | Frozen budgets/day, repeatable expiry and storage failures | Same external fixture with original and candidate production classes; physical H2 table rename causes failures, latch delays real JDBC writer; normal callbacks/data paths unchanged. Controls are not product endpoints or edited tests |
| Proof16 downstream clauses pending | Independent review and post-merge register/system description happen after qa_check | Lead transition1882 explicitly returns16 after review/merge/design-owner edits; QA judges only1–15/17 now. No premature acceptance or scope waiver |
| No Docker/PostgreSQL, NAT network, broad latency or fresh in-flight shutdown experiment | Outside this refactor's required outcomes | Actual H2/original databases, jar contexts and unchanged functional timing assertions verified. All own apps stopped; no new portability/capacity/shutdown guarantee |
'''
(OUT/'gaps-append.md').write_text(gaps)

readme = '''# Independent QA — 06-client-identity

QA2 verifies exact `fb63a88a9b92c1fec97ba74686af1a2f30304160`; product tree4c945cf11bf38036bba900425dacda9bb8ca7a83. Product/build/tests were not edited. `source-custody.json` records the43 original functional files' equal Git blobs, production-only move, reference-only three unit adaptations, unchanged migrations/build/settings/API and impact→design/ADRs→dependent move chronology. The only properties delta is the granted comment, not a setting. Static source inspection confirms one ClientIdentity owns resolution and the direct-peer predicate, limiter resolves after exemption/before charge, recorder reads that attribute or peer fallback, and audit never consults proxy trust. DailySalt is unchanged: HMAC under an in-memory per-UTC-day key, expiry zeros/drops it, no response/log/meter carries hashes. Independent code/security review follows this QA; proof16 awaits its downstream clauses.

The final fresh gate has268 unit322 functional, Javadoc and merged584/584 lines206/206 branches. Read `check-fb63a88-diagnostic-02.txt`, `gate-final/summary.json`, copied reports and coverage SUMMARY. Earlier two external401 failures are retained with diagnosed listener collision; isolated class19 and full fresh gate passed unchanged. `characterization-baseline/` and `check-characterization-1b.txt` are QA's own fresh268/322 replay of the completed pre-move assertions on original production, not copied builder output.

| AC | Independently observed effect / capture |
|---|---|
|1| Original-production seed creates active and retired links; its stopped H2 directory is copied into each core app. Old active302 retains exact external Location and no-store; original link rows remain identical. `baseline-origin-fixtures.json`, core SQL and `status-302` |
|2| Unknown404, retired410, active POST405, javascript create400; same problem fields, no target or extra click; HEAD records none. Core captures and audit/link snapshots |
|3| Fixed-clock60 creates201/61st429 and600 redirects302/601st429, changing all three spoof headers. Retry-After positive integer; no rejected link/click and no429 Location. `*-defaults-*` |
|4| Every M01–M12 independently tests both2/min budgets, shared429Retry30 and unrelated201/302. Exact P-only/P,Q/empty trust settings, not merely a superset. `*-matrix-*`, `Mnn-*` |
|5| Every row gets three browser clicks, exactly one2026-10-01 day, clicks3/unique2/bot0; private rows use two hash groups with no raw identity. Matrix SQL and stats |
|6| All six loopback text forms200, whether listed as trusted or not, unchanged trail. `*-audit-*/loopback-*` |
|7| All four nonlocal forms GET and HEAD403, both trust settings, no trail. `nonlocal-*` |
|8| Both peer classes and both forwarding headers with remote, forged local, empty and whitespace values403 under both trust settings; no row change. `forwarded-*` |
|9| Real unmodified jars: strategy none with remote-IP/protocol/both, native and framework give GET/HEAD403 even headerless/forged; cloud plain200/fwd403. Six separate empty/space/both-empty/both-space contexts per original/candidate give plain GET/HEAD200 and forwarded GET/HEAD403. `before/`, `after/`, `blank-settings/` |
|10| Invalid limit0/AcceptHTML gets403 with available budget, then429Retry30 after spending it; audit rows identical. `matrix-0/precedence-*` |
|11| Local invalid limit400, POSTaudit405 and physical renamed audit table500; problem sanitized and no partial page, restored rows unchanged. `core/audit-*` |
|12| Raw address/forwarding/user-agent/referrer path/query/fragment canaries absent from logs, stored click values, stats and metric names/tags; private stored hash values also absent from public outputs/logs. Authorized audit may return canary target. `clicks-*/privacy-prometheus`, SQL, all logs |
|13| Both trust settings: held real writer leaves response302 under1s and no stored row until release; physical missing table keeps302 and yields exactly one correlated WARN/write-failed loss;20 concurrent redirects count20, HEAD/unsuccessful count0; final recorded21/lost1 and grouping1 or2. Inherited timing/assertion files unchanged and all pass. `clicks-*` |
|14| Equivalent201/302/audit200/400/403/404/405/410/429/injected500 carrying inbound canary all issue valid new UUIDs; header/problem/audit/event correlations preserved, every event counted.1017 exact controlled response/log pairs under documented substitutions, including loss WARN. `request-log-joins.json`, `substitutions.json`, normalized records, scoped comparison |
|15| Live original/candidate OpenAPI equals committed canonical document; unchanged generator passes and worktree API bytes stay equal. `before/live-openapi.json`, `after/live-openapi.json`, source custody |

Controlled capture: `effects/run-20261004T021042100465Z/` has2041 recorded requests (three seed,1019 per lane),10380 successful effect assertions and19 process/port stop checks. Exact-source external `proof/QaIdentityRunner.java`/`qa_by_effect.py` compiles via wrapper+external init file; it supplies the frozen Clock, explicit Servlet peer values and an actual JDBC writer latch. No product or shipped test edits. The peers and whitespace-only request headers are Servlet inputs supplied by this fixture, not claims of physical nonlocal sockets/whitespace HTTP transmission. All other traffic goes through scripts/http to real HTTP paths/repositories. Real jar checks independently cover container behavior. Baseline classes come from characterized1b4e0a7, whose production tree is byte-identical to the before-capture original b68ff80; candidate classes are exactfb63a88.

Before-capture59 responses/336 assertions and candidate replay59/336 each start/stop10 actual unmodified jars. Their jar hashes are in artifact.json; before evidence was committed03e0657 before implementation. Blank-setting follow-up72/372 starts/stops12 actual jars. All51 apps are stopped, including the pre-work ten. Full raw responses, curl wire traces (jar checks), argv, SQL, request-ID JSON log joins and startup/shutdown windows are retained. App stop is exit+port refusal; no separate in-flight shutdown claim.

Rule6 substitutions are explicit dictionaries: generated request UUID per named request, generated link code per create (replays keep the same code token), associated creation/audit timestamps, HTTP Date, JSON-log timestamp and process PID. Audit requestId/entityId relationships are validated and retained; audit event time is later than/equal to link creation time, not falsely equated to it. Controlled time/date and seed codes need no substitution. No status, target Location, header presence, problem field/stable value, access decision, grouping, event level/message/count or missing field is normalized. Private hashes are compared by within-process grouping/privacy, never across independently salted processes.

`proof/qa_by_effect.py` is the exact original driver: its final comparator was too broad and failed on two full Prometheus bodies. That failure and full differences remain in assertions.jsonl/comparison.json. `proof/qa_saved_comparisons.py` performs a passive scoped analysis:1017 non-metric pairs equal; two metric responses/events and business names/tags/values equal, privacy assertions passed. JVM/runtime values differ, baseline clicks-0 records health503 series (consistent with startup polling, an inference because raw readiness responses were not saved), and candidate clicks-1 records the existing concurrent-GC phase meter. Both additional name/tag sets are explicitly reported. This is not normalization of the named AC-14 cases or a metric identity promise: AC-12 requires private telemetry. Jar comparison retains the springdoc existing INFO initialization-duration message293ms versus162ms; API response and all other fields match. That API-docs event is outside AC-14's audit200 case, whose full events match. Neither measured message is erased.

`test-inventory.json` maps321 declarations plus five inherited subclass methods across74 reports; all590 invocations green,56 methods only class-attributed due to parameterized display names. TRACEABILITY maps each current AC/requirement to exact functional methods and every method back to a preservation/business responsibility. GAPS explicitly records per-suite misses, instruments, comparisons and deferred proof16. No new product finding or accepted coverage gap. No Docker/PostgreSQL/NAT/capacity/new performance guarantee is claimed.

## Self-check

Every AC and failure tried by effect; raw response/log/row joins inspected; matrix and settings enumerated; original production re-run; first red and comparator instrument limitations preserved; CSV totals and378 hashes read; reverse mappings326 with class-only limitations; GAPS append present; all owned apps stopped; worktree clean atfb63a88; no product/test mutation. QA proof drop and attributed receipts1–15/17 attach this evidence before qa_check handoff. Proof16 returns only after independent review/merge/design-owner updates, per lead qitem-20261004015644-b1493e3f/transition1882.
'''
(OUT/'README.md').write_text(readme)
(OUT/'record-validation.json').write_text(json.dumps({'pass':True,'coverageHashesVerified':len(hashes),
   'controlledResponses':len(requests),'effectAssertionsPassed':10380,'controlledAppsStopped':19,
   'methodMappings':inventory['methodMappings'],'declaredMethods':inventory['declaredMethods'],
   'classOnlyMethods':inventory['classOnlyMethods'],'blankSettingsSummary':json.loads((blank/'summary.json').read_text())},indent=2)+'\n')
print((OUT/'record-validation.json').read_text())
