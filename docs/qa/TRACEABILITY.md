# Acceptance-criteria ↔ test traceability

Appended by the QA Agent at every `qa_check`. One table per slice; every AC has
at least one functional test, every test maps to an AC or a business rule.

<!-- slice tables below -->

## 01-ping — candidate 3886a04a4afac6117038b2884cf72749f57d28aa

Independent execution 2026-10-02/03 UTC. All 15 tests pass with no skips.
Live AC-7 differs from MockMvc: see [QA-01](01-ping/findings.md).
Class names below are relative to `dev.urlshort`.

| AC / business rule | Test class#method | Suite | Result |
|---|---|---|---|
| AC-1; BR-4, BR-6 | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS, live PASS |
| AC-2; BR-5 | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS, live PASS |
| AC-3; BR-2, BR-3 | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS, live PASS |
| AC-4; BR-2 | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS, live PASS |
| AC-5; BR-3, BR-7 | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS, live PASS |
| AC-6; BR-8 | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS, live PASS |
| AC-7; BR-1, logging obligation | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS; live address-absence FAIL, QA-01 |
| AC-8; BR-1 | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS, live PASS |
| AC-3, AC-6; BR-2, BR-3, BR-8 (MDC lifecycle) | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS |
| AC-4, AC-8; BR-1, BR-2 | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS |
| BR-1, BR-8 (request context isolated after failure) | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS |
| AC-1, AC-2; BR-4, BR-5, BR-6 | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS |
| BR-4 (baseline health responsibility) | HealthJourneyTest#healthEndpointReportsUp | functional | PASS |
| BR-4 startup prerequisite; coverage gate (baseline) | UrlshortApplicationTests#contextLoads | unit | PASS; context smoke, not a direct ping assertion |
| BR-4 startup prerequisite; coverage gate (baseline main entry point) | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS; bootstrap check, not an HTTP assertion |

Each AC has its named functional test; BR-1 through BR-8 appear above; all
15 executed methods are mapped. Exact JUnit method inventory is
`missions/00-hello/slices/01-ping/proof/qa-tests.txt`.
