# Acceptance-criteria ↔ test traceability

Appended by the QA Agent at every `qa_check`. One table per slice; every AC has
at least one functional test, every test maps to an AC or a business rule.

Rows carry the product requirement id (`FR-n`/`NFR-n` from `docs/REQUIREMENTS.md`)
next to the AC from the first mission-01 slice on; the 00-hello table predates the baseline.

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

## Re-check 01-ping — candidate f286a10863e4a8081235226f2d56e51ac121b319

Independent execution 2026-10-03 UTC: all 15 tests pass with no skips; all
eight ACs also pass live HTTP checks. QA-01 resolved. Names relative to
`dev.urlshort`; prior table is historical.

| AC / business rule | Test class#method | Suite | Result |
|---|---|---|---|
| AC-1; BR-4, BR-6 | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS, live PASS |
| AC-2; BR-5 | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS, live PASS |
| AC-3; BR-2, BR-3 | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS, live PASS |
| AC-4; BR-2 | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS, live PASS |
| AC-5; BR-3, BR-7 | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS, live PASS |
| AC-6; BR-8 | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS, live PASS |
| AC-7; BR-1, logging obligation | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS including thread-name exclusion, live PASS |
| AC-8; BR-1 | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS, live PASS |
| AC-3, AC-6; BR-2, BR-3, BR-8 (MDC lifecycle) | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS |
| AC-4, AC-8; BR-1, BR-2 | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS |
| BR-1, BR-8 (request context isolated after failure) | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS |
| AC-1, AC-2; BR-4, BR-5, BR-6 | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS |
| BR-4 (baseline health responsibility) | HealthJourneyTest#healthEndpointReportsUp | functional | PASS |
| BR-4 startup prerequisite; coverage gate (baseline) | UrlshortApplicationTests#contextLoads | unit | PASS; context smoke, not direct ping assertion |
| BR-4 startup prerequisite; coverage gate (baseline main entry point) | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS; bootstrap check, not HTTP assertion |

All AC-1–AC-8, BR-1–BR-8 and the 15 executed methods are mapped. Exact
inventory: `missions/00-hello/slices/01-ping/proof/qa-tests-f286a10.txt`.

## 01-create-redirect — candidate a922f49144049db0228c316c474ac6e890742fa5

Independent QA execution 2026-10-03 UTC: 72 unit and 87 functional invocations, no failures, errors or skips. The table maps all 42 unit and 45 functional methods, including the parameterized rows they own; lifecycle hooks and fixture factories are not tests. AC-1–AC-28 and BR-1–BR-12 are all represented. Exact JUnit invocation inventory: `missions/01-greenfield-core/slices/01-create-redirect/proof/qa-test-invocations-a922f49.txt`.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| AC-16; bootstrap prerequisite; NFR-M1 | dev.urlshort.UrlshortApplicationTests#contextLoads | unit | PASS |
| AC-16; bootstrap prerequisite; NFR-M1 | dev.urlshort.UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS |
| BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2 | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS |
| BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2 | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS |
| BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1 | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS |
| BR-6; FR-4 | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#validUrlsPass | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS |
| BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1 | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS |
| BR-1; FR-1 | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS |
| BR-1; FR-1 | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS |
| AC-16; 01-ping AC-1, AC-2 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS |
| BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6 | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS |
| BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9 | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS |
| BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9 | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS |
| BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9 | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS |
| AC-7; NFR-S3; body-limit stream mechanics | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS |
| AC-7; NFR-S3; body-limit stream mechanics | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS |
| AC-7; NFR-S3; body-limit stream mechanics | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS |
| AC-7; NFR-S3; body-limit stream mechanics | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS |
| AC-7; NFR-S3; body-limit stream mechanics | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS |
| BR-10; AC-26, AC-27; NFR-O1, NFR-O2 | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS |
| BR-10; AC-26, AC-27; NFR-O1, NFR-O2 | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS |
| BR-10; AC-26, AC-27; NFR-O1, NFR-O2 | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS |
| BR-10; AC-26, AC-27; NFR-O1, NFR-O2 | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS |
| AC-16; existing health surface regression | dev.urlshort.HealthJourneyTest#healthEndpointReportsUp | functional | PASS |
| AC-22; NFR-A1; BR-9, BR-12 | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS |
| AC-23; NFR-A1; BR-6, BR-9, BR-12 | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS |
| AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10 | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS |
| AC-25; NFR-A2; BR-9 | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS |
| AC-17; FR-9; BR-5, BR-9 | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS |
| AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9 | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS |
| AC-19; NFR-R5; BR-5 | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS; FunctionalClockConfig @Primary FunctionalClock.shift/reset controls 23h, 24h−1s, 24h+1s |
| AC-20; FR-9, FR-5; BR-5, BR-8 | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS |
| AC-20; FR-9, FR-5; BR-5, BR-8 | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS |
| AC-21; FR-9; BR-5 | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS |
| AC-1; FR-1; BR-1, BR-2, BR-11, BR-12 | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS |
| AC-2; FR-1; BR-1, BR-4 | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS |
| AC-3; FR-1, NFR-S4; BR-11 | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS |
| AC-4; FR-5, NFR-S1; BR-3, BR-8 | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS |
| AC-4; FR-5, NFR-S1; BR-3, BR-8 | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS |
| AC-5; FR-5; BR-8 | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS |
| AC-6; FR-5, NFR-S3; BR-8 | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS |
| AC-7; NFR-S3; BR-8 | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS |
| AC-16; FR-6 (surface regression); BR-1 | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS |
| AC-8; FR-3; BR-2, BR-6, BR-11, BR-12 | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS |
| AC-9; FR-3, FR-4; BR-6 | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS |
| AC-10; FR-4; BR-6, BR-9 | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS |
| AC-11; FR-4; BR-6, BR-9 | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS |
| AC-14; FR-6; BR-8 | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS |
| AC-15; FR-6; BR-8 | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS |
| AC-3; FR-1, NFR-S4; BR-11 | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS |
| AC-12; FR-2; BR-2, BR-7 | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS |
| AC-13; FR-4; BR-6, BR-8 | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS |
| BR-7; AC-12; FR-2 | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC1 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC2 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC3 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC4 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC5 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC6 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC7 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS |
| AC-16; BR-10, BR-12; 01-ping AC8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS |
| AC-26; NFR-O1, NFR-O2; BR-10 | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS |
| AC-26; NFR-O1, NFR-O2; BR-10 | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS |
| AC-27; NFR-O2; BR-10 | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS |
| AC-27; NFR-O2; BR-10 | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS |
| BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2 | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS |
| AC-28; NFR-M3; proof item 10 | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS |
| AC-28; NFR-M3 | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS |

## 02-analytics — candidate 862c52eea8294e438b1f98b832ae4f64f7a16923

Independent QA, 2026-10-03 UTC: 121 unit and 126 functional invocations, zero failures/errors/skips. All 138 source test methods are mapped below; parameterized invocations are in the slice proof/qa-test-invocations-862c52e.txt. AC-11 is explicitly asserted by AC-08/09/10. Inherited rows retain the prior slice's numbered AC/rules and product ids; every inherited test ran again on this candidate and guards AC-20/BR-8.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#contextLoads | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aHeadRequestIsNotAClick | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aRedirectIsStoredAsItsReducedFactsOnly | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeReducedIsOneWarn | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFastStoreIsDrainedOnCloseAndNothingIsReported | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 862c52e |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 862c52e |
| AC-1, AC-4; BR-2, BR-4; NFR-P1; migration constraints after DDL connection retirement | click.ClickSchemaTest#theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerIsReducedToItsOrigin | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#aReferrerThatIsNotAnHttpOriginIsNone | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerLengthCapIs2048 | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theUserAgentIsReducedToAClass | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#anAbsentOrEmptyUserAgentIsUnknown | unit | PASS on 862c52e |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#rule2_aClickHoldsOnlyTheReducedFacts | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#anotherAddressOrAnotherDayHashesDifferently | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#theHashIsNeverTheUnsaltedDigest | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#closeDropsTheSalt | unit | PASS on 862c52e |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick | unit | PASS on 862c52e |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#noRowsIsZeroAndTwoEmptyLists | unit | PASS on 862c52e |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals | unit | PASS on 862c52e |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#referrersAreRankedByClicksThenByCodePointAndCappedAtTen | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-6; FR-4; 02 AC-20/BR-8 regression | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#validUrlsPass | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; 01-ping AC-1, AC-2 regression; 02 AC-20/BR-8 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS on 862c52e |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; existing health surface regression; 02 AC-20/BR-8 regression | HealthJourneyTest#healthEndpointReportsUp | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-22; NFR-A1; BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-23; NFR-A1; BR-6, BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-25; NFR-A2; BR-9; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS on 862c52e |
| AC-1; BR-1, BR-2, BR-6; FR-7 | click.ClickRecordingJourneyTest#AC01_aRedirectRecordsExactlyOneClickWithItsTime | functional | PASS on 862c52e |
| AC-2; BR-1; FR-7 | click.ClickRecordingJourneyTest#AC02_onlyARedirectIsAClick | functional | PASS on 862c52e |
| AC-3; BR-2, BR-3; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC03_theReferrerIsStoredAsItsOriginOnly | functional | PASS on 862c52e |
| AC-4; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC04_theUserAgentIsStoredAsAClassOnly | functional | PASS on 862c52e |
| AC-5; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay | functional | PASS on 862c52e; controlled FunctionalClock; no real midnight wait |
| AC-6; BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC06_forwardingHeadersDoNotChangeTheRecordedClient | functional | PASS on 862c52e |
| AC-17; BR-2, BR-7; NFR-P1 | click.ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly | functional | PASS on 862c52e |
| AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs | functional | PASS on 862c52e |
| AC-14; BR-5; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect | functional | PASS on 862c52e; real Tomcat, 20 replies each <250 ms with 2 s write delay |
| AC-15; BR-5, BR-9; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn | functional | PASS on 862c52e; real Tomcat, spy failure, one safe correlated WARN |
| AC-16; BR-1, BR-2, BR-6; FR-7 | click.ClickResilienceJourneyTest#AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse | functional | PASS on 862c52e |
| AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated | functional | PASS on 862c52e |
| AC-22; BR-1, BR-7; FR-8 | click.ClickResilienceJourneyTest#AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing | functional | PASS on 862c52e; HEAD body asserted on Tomcat, OPTIONS on MockMvc and live curl |
| AC-1, AC-4; BR-2, BR-4; FR-7, NFR-P1; pooled-connection retirement smoke | click.ClickSchemaJourneyTest#everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections | functional | PASS on 862c52e |
| AC-7; BR-7; FR-8 | click.StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics | functional | PASS on 862c52e |
| AC-8, AC-11; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC08_totalClicksCountsEveryRedirect | functional | PASS on 862c52e |
| AC-9, AC-11; BR-2, BR-7; FR-8 | click.StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay | functional | PASS on 862c52e; controlled FunctionalClock; no real midnight wait |
| AC-10, AC-11; BR-3, BR-7; FR-8 | click.StatsJourneyTest#AC10_topReferrersAreRankedAndCapped | functional | PASS on 862c52e |
| AC-12; BR-1, BR-7, BR-8; FR-8 | click.StatsJourneyTest#AC12_aRetiredLinksStatisticsAreStillReadable | functional | PASS on 862c52e |
| AC-13; BR-7, BR-9; FR-8 | click.StatsJourneyTest#AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails | functional | PASS on 862c52e |
| AC-19; BR-9; NFR-O1, NFR-O2 | click.StatsJourneyTest#AC19_theStatisticsPathAndASettledRedirectAreCorrelated | functional | PASS on 862c52e |
| AC-20; BR-8; FR-2, FR-4, FR-6, NFR-A1 regression | click.StatsJourneyTest#AC20_redirectAndAuditBehaviourAreUnchanged | functional | PASS on 862c52e |
| AC-21; BR-7; NFR-M3 | click.StatsJourneyTest#AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint | functional | PASS on 862c52e |
| AC-22; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing | functional | PASS on 862c52e; HEAD body asserted on Tomcat, OPTIONS on MockMvc and live curl |
| Inherited 01-create-redirect: AC-17; FR-9; BR-5, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-19; NFR-R5; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-21; FR-9; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-1; FR-1; BR-1, BR-2, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-2; FR-1; BR-1, BR-4; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-5; FR-5; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-6; FR-5, NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-7; NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; FR-6 (surface regression); BR-1; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-8; FR-3; BR-2, BR-6, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-9; FR-3, FR-4; BR-6; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-10; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-11; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-14; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-15; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-12; FR-2; BR-2, BR-7; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-13; FR-4; BR-6, BR-8; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS on 862c52e |
| Inherited 01-create-redirect: BR-7; AC-12; FR-2; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC1 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC2 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC3 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC4 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC5 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC6 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC7 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC8 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS on 862c52e |
| Inherited 01-create-redirect: BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-28; NFR-M3; proof item 10; 02 AC-20/BR-8 regression | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS on 862c52e |
| Inherited 01-create-redirect: AC-28; NFR-M3; 02 AC-20/BR-8 regression | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS on 862c52e |

## 02-analytics — candidate 5b3490c65915cf42594a4720350950bcefd2d7d0

Re-check after code CR-01: only DailySaltTest setup changed; all original assertions and every functional method unchanged. Fresh 121 unit / 126 functional invocations, zero failures/errors/skips; isolated midnight method also passes. All 138 methods mapped below (69 per suite), all 22 ACs/nine rules retained. Prior full live effects on 862c52e are adopted by exact product/config/functional-tree equality; a new 15-exchange live check and actual failed insert are recorded on this SHA.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#contextLoads | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aHeadRequestIsNotAClick | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aRedirectIsStoredAsItsReducedFactsOnly | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeReducedIsOneWarn | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFastStoreIsDrainedOnCloseAndNothingIsReported | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 5b3490c |
| AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 5b3490c |
| AC-1, AC-4; BR-2, BR-4; NFR-P1; migration constraints after DDL connection retirement | click.ClickSchemaTest#theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerIsReducedToItsOrigin | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#aReferrerThatIsNotAnHttpOriginIsNone | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerLengthCapIs2048 | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theUserAgentIsReducedToAClass | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#anAbsentOrEmptyUserAgentIsUnknown | unit | PASS on 5b3490c |
| AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#rule2_aClickHoldsOnlyTheReducedFacts | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#anotherAddressOrAnotherDayHashesDifferently | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#theHashIsNeverTheUnsaltedDigest | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt | unit | PASS on 5b3490c ; isolated cold test also PASS |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#closeDropsTheSalt | unit | PASS on 5b3490c |
| AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick | unit | PASS on 5b3490c |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#noRowsIsZeroAndTwoEmptyLists | unit | PASS on 5b3490c |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals | unit | PASS on 5b3490c |
| AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#referrersAreRankedByClicksThenByCodePointAndCappedAtTen | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-6; FR-4; 02 AC-20/BR-8 regression | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#validUrlsPass | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; 01-ping AC-1, AC-2 regression; 02 AC-20/BR-8 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; existing health surface regression; 02 AC-20/BR-8 regression | HealthJourneyTest#healthEndpointReportsUp | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-22; NFR-A1; BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-23; NFR-A1; BR-6, BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-25; NFR-A2; BR-9; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS on 5b3490c |
| AC-1; BR-1, BR-2, BR-6; FR-7 | click.ClickRecordingJourneyTest#AC01_aRedirectRecordsExactlyOneClickWithItsTime | functional | PASS on 5b3490c |
| AC-2; BR-1; FR-7 | click.ClickRecordingJourneyTest#AC02_onlyARedirectIsAClick | functional | PASS on 5b3490c |
| AC-3; BR-2, BR-3; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC03_theReferrerIsStoredAsItsOriginOnly | functional | PASS on 5b3490c |
| AC-4; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC04_theUserAgentIsStoredAsAClassOnly | functional | PASS on 5b3490c |
| AC-5; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay | functional | PASS on 5b3490c; controlled FunctionalClock; no real midnight wait |
| AC-6; BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC06_forwardingHeadersDoNotChangeTheRecordedClient | functional | PASS on 5b3490c |
| AC-17; BR-2, BR-7; NFR-P1 | click.ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly | functional | PASS on 5b3490c |
| AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs | functional | PASS on 5b3490c |
| AC-14; BR-5; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect | functional | PASS on 5b3490c; real Tomcat, 20 replies each <250 ms with 2 s write delay |
| AC-15; BR-5, BR-9; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn | functional | PASS on 5b3490c; real Tomcat, spy failure, one safe correlated WARN |
| AC-16; BR-1, BR-2, BR-6; FR-7 | click.ClickResilienceJourneyTest#AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse | functional | PASS on 5b3490c |
| AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated | functional | PASS on 5b3490c |
| AC-22; BR-1, BR-7; FR-8 | click.ClickResilienceJourneyTest#AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing | functional | PASS on 5b3490c; HEAD body asserted on Tomcat, OPTIONS on MockMvc and live curl |
| AC-1, AC-4; BR-2, BR-4; FR-7, NFR-P1; pooled-connection retirement smoke | click.ClickSchemaJourneyTest#everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections | functional | PASS on 5b3490c |
| AC-7; BR-7; FR-8 | click.StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics | functional | PASS on 5b3490c |
| AC-8, AC-11; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC08_totalClicksCountsEveryRedirect | functional | PASS on 5b3490c |
| AC-9, AC-11; BR-2, BR-7; FR-8 | click.StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay | functional | PASS on 5b3490c; controlled FunctionalClock; no real midnight wait |
| AC-10, AC-11; BR-3, BR-7; FR-8 | click.StatsJourneyTest#AC10_topReferrersAreRankedAndCapped | functional | PASS on 5b3490c |
| AC-12; BR-1, BR-7, BR-8; FR-8 | click.StatsJourneyTest#AC12_aRetiredLinksStatisticsAreStillReadable | functional | PASS on 5b3490c |
| AC-13; BR-7, BR-9; FR-8 | click.StatsJourneyTest#AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails | functional | PASS on 5b3490c |
| AC-19; BR-9; NFR-O1, NFR-O2 | click.StatsJourneyTest#AC19_theStatisticsPathAndASettledRedirectAreCorrelated | functional | PASS on 5b3490c |
| AC-20; BR-8; FR-2, FR-4, FR-6, NFR-A1 regression | click.StatsJourneyTest#AC20_redirectAndAuditBehaviourAreUnchanged | functional | PASS on 5b3490c |
| AC-21; BR-7; NFR-M3 | click.StatsJourneyTest#AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint | functional | PASS on 5b3490c |
| AC-22; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing | functional | PASS on 5b3490c; HEAD body asserted on Tomcat, OPTIONS on MockMvc and live curl |
| Inherited 01-create-redirect: AC-17; FR-9; BR-5, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-19; NFR-R5; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-21; FR-9; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-1; FR-1; BR-1, BR-2, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-2; FR-1; BR-1, BR-4; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-5; FR-5; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-6; FR-5, NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-7; NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; FR-6 (surface regression); BR-1; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-8; FR-3; BR-2, BR-6, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-9; FR-3, FR-4; BR-6; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-10; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-11; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-14; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-15; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-12; FR-2; BR-2, BR-7; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-13; FR-4; BR-6, BR-8; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-7; AC-12; FR-2; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC1 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC2 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC3 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC4 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC5 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC6 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC7 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC8 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-28; NFR-M3; proof item 10; 02 AC-20/BR-8 regression | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS on 5b3490c |
| Inherited 01-create-redirect: AC-28; NFR-M3; 02 AC-20/BR-8 regression | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS on 5b3490c |

## 03-operate — candidate a7c533ffef55650e5b422377ffe0c4e38d41400c

Independent QA, 2026-10-03 UTC: 163 unit / 155 functional invocations, zero failures/errors/skips. All 184 source methods are mapped below; parameterized rows are included in `missions/01-greenfield-core/slices/03-operate/proof/qa-test-invocations-a7c533f.txt`. Inherited tests ran freshly on this candidate, with the current functional budgets and clock reset; inherited AC/rule numbers retain their originating slice. Every AC-1–AC-20 has a functional method. Real HTTP effects additionally cover these criteria; controlled peers/time and DB failure are disclosed in PROOF.md.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#contextLoads | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; 01-ping AC-1, AC-2 regression; 02 AC-20/BR-8 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS on a7c533f |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03a_anExactlyEmptyBucketRefillsOneTokenAfterExactlyOneSecond | unit | PASS on a7c533f |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03b_retryAfterIsRoundedUpAndHonoured | unit | PASS on a7c533f |
| AC-4; BR-2; NFR-R2 | web.RateLimiterTest#AC04_aQuietMinuteRefillsTheWholeBucket | unit | PASS on a7c533f |
| AC-3, AC-9; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimiterTest#refusalsTakeNothing | unit | PASS on a7c533f |
| AC-5, AC-6; BR-1, BR-2; NFR-R2 | web.RateLimiterTest#budgetsAndClientsAreIndependent | unit | PASS on a7c533f |
| AC-3; BR-4; FR-10, NFR-R2 | web.RateLimiterTest#retryAfterIsTheWaitForOneTokenInWholeSeconds | unit | PASS on a7c533f |
| BR-2, BR-8; clock-step resilience; NFR-R2 | web.RateLimiterTest#aBackwardClockStepStartsTheClientFreshInsteadOfLockingItOut | unit | PASS on a7c533f |
| BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#fullBucketsAreReleasedByTheNextRequestButNotWhileIdle | unit | PASS on a7c533f |
| BR-2, BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#aBucketThatIsNotYetFullSurvivesTheRelease | unit | PASS on a7c533f |
| BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#theReleaseRunsAtMostOncePerSecond | unit | PASS on a7c533f |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_limitedRequestsAreChargedToTheirBudget | unit | PASS on a7c533f |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_operatorSurfacesAreNeitherChargedNorLimited | unit | PASS on a7c533f |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop | unit | PASS on a7c533f |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy | unit | PASS on a7c533f |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#theFilterReadsOnlyXForwardedForAndOnlyFromATrustedPeer | unit | PASS on a7c533f |
| AC-1, AC-2, AC-11; BR-3, BR-4, BR-6; FR-10, NFR-R2, NFR-P1 | web.RateLimitFilterTest#anEmptyBucketIsA429ProblemWithRetryAfterAndNoChain | unit | PASS on a7c533f |
| AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitFilterTest#eachRejectionIsCountedOnceUnderItsBudgetOnly | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#anotherAddressOrAnotherDayHashesDifferently | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#theHashIsNeverTheUnsaltedDigest | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#closeDropsTheSalt | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#noRowsIsZeroAndTwoEmptyLists | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#referrersAreRankedByClicksThenByCodePointAndCappedAtTen | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; NFR-P1; migration constraints after DDL connection retirement | click.ClickSchemaTest#theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aHeadRequestIsNotAClick | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aRedirectIsStoredAsItsReducedFactsOnly | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeReducedIsOneWarn | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFastStoreIsDrainedOnCloseAndNothingIsReported | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerIsReducedToItsOrigin | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#aReferrerThatIsNotAnHttpOriginIsNone | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerLengthCapIs2048 | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theUserAgentIsReducedToAClass | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#anAbsentOrEmptyUserAgentIsUnknown | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#rule2_aClickHoldsOnlyTheReducedFacts | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#validUrlsPass | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-6; FR-4; 02 AC-20/BR-8 regression | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; existing health surface regression; 02 AC-20/BR-8 regression | HealthJourneyTest#healthEndpointReportsUp | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC1 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC2 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC3 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC4 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC5 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC6 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC7 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC8 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS on a7c533f |
| AC-25, AC-28; BR-13; NFR-R3 (configuration prerequisite only) | web.ShutdownPhaseDefaultTest#theShippedGracefulShutdownPhaseIsTenSeconds | functional | PASS on a7c533f; config assertion, release outcome pending |
| AC-8; BR-5; NFR-R2 | web.TrustedProxyJourneyTest#AC08_aTrustedProxysForwardedAddressIdentifiesTheClient | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS on a7c533f |
| AC-14, AC-15; BR-9; NFR-R1 | web.DatabaseDownJourneyTest#AC14_AC15_readinessFollowsTheDatabaseAndLivenessDoesNot | functional | PASS on a7c533f |
| AC-13; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC13_livenessAndReadinessAreUpWithAWorkingDatabase | functional | PASS on a7c533f |
| AC-15; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC15_healthBodiesDiscloseNothingAboutTheInstallation | functional | PASS on a7c533f |
| AC-18; BR-10; NFR-O3 | web.HealthMetricsJourneyTest#AC18_redirectsAreCountedByRouteTemplate | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS on a7c533f |
| AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theShippedConfigurationCarriesTheDecidedBudgets | functional | PASS on a7c533f |
| AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theSettingsRecordDefaultsToTheSameBudgets | functional | PASS on a7c533f |
| AC-10; BR-11; NFR-R2 | web.RateLimitSettingsJourneyTest#AC10_theBudgetsAreOperatorSettings | functional | PASS on a7c533f |
| AC-1; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC01_theCreateBudgetAdmits60AndRefusesThe61st | functional | PASS on a7c533f |
| AC-2; BR-1, BR-2; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC02_theRedirectBudgetAdmits600AndRefusesThe601st | functional | PASS on a7c533f |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03a_retryAfterIsTruthfulFromAnExactlyEmptyBucket | functional | PASS on a7c533f |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03b_retryAfterIsAnUpperBoundFromAPartlyRefilledBucket | functional | PASS on a7c533f |
| AC-4; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC04_aFullBudgetReturnsAfterAQuietMinute | functional | PASS on a7c533f |
| AC-5; BR-1, BR-2; NFR-R2 | web.RateLimitJourneyTest#AC05_theTwoBudgetsAreIndependent | functional | PASS on a7c533f |
| AC-6; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC06_clientsAreIndependent | functional | PASS on a7c533f |
| AC-7; BR-5; NFR-R2 | web.RateLimitJourneyTest#AC07_aForgedForwardedAddressDoesNotChangeTheClient | functional | PASS on a7c533f |
| AC-9; BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC09_everyRequestInABudgetCountsAndTheLimitIsCheckedFirst | functional | PASS on a7c533f |
| AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_theTooManyRequestsProblemNamesNoClient | functional | PASS on a7c533f |
| AC-12; BR-6, BR-7; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#AC12_eachRejectionIsLoggedOnceCorrelatedAndWithoutClientValues | functional | PASS on a7c533f |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_percentEncodingDoesNotMoveARequestToTheLargerBudget | functional | PASS on a7c533f |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_operatorSurfacesAreNeverLimited | functional | PASS on a7c533f |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_aDotDotSegmentUnderAnExemptPrefixReachesNoLimitedOperation | functional | PASS on a7c533f |
| AC-16; BR-10; NFR-O3 | web.RateLimitJourneyTest#AC16_theMetricsSurfaceListsTheFourKindsOfMetric | functional | PASS on a7c533f |
| AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitJourneyTest#AC17_everyRejectionIsCountedOnceByBudget | functional | PASS on a7c533f |
| AC-19; BR-6, BR-10; NFR-O3, NFR-P1 | web.RateLimitJourneyTest#AC19_metricsAreExposedForScrapingWithoutClientOrLinkValues | functional | PASS on a7c533f |
| BR-1, BR-6; design DR-01; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#designDR01_anInvalidPathUnderAnExemptPrefixLogsNoSubmittedValueOnTomcat | functional | PASS on a7c533f |
| AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_onTomcatThe429IsTheSameProblemMediaTypeAsEveryOtherError | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; proof item 10; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3 | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3 | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS on a7c533f |
| AC-20; BR-1; FR-10, NFR-M3 | web.OpenApiDocumentTest#AC20_everyOperationDocumentsTheTooManyRequestsProblem | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; FR-7, NFR-P1; pooled-connection retirement smoke | click.ClickSchemaJourneyTest#everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-7; BR-7; FR-8 | click.StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-8, AC-11; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC08_totalClicksCountsEveryRedirect | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-9, AC-11; BR-2, BR-7; FR-8 | click.StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-10, AC-11; BR-3, BR-7; FR-8 | click.StatsJourneyTest#AC10_topReferrersAreRankedAndCapped | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-12; BR-1, BR-7, BR-8; FR-8 | click.StatsJourneyTest#AC12_aRetiredLinksStatisticsAreStillReadable | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-13; BR-7, BR-9; FR-8 | click.StatsJourneyTest#AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-19; BR-9; NFR-O1, NFR-O2 | click.StatsJourneyTest#AC19_theStatisticsPathAndASettledRedirectAreCorrelated | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-20; BR-8; FR-2, FR-4, FR-6, NFR-A1 regression | click.StatsJourneyTest#AC20_redirectAndAuditBehaviourAreUnchanged | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-21; BR-7; NFR-M3 | click.StatsJourneyTest#AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-1; BR-1, BR-2, BR-6; FR-7 | click.ClickRecordingJourneyTest#AC01_aRedirectRecordsExactlyOneClickWithItsTime | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-2; BR-1; FR-7 | click.ClickRecordingJourneyTest#AC02_onlyARedirectIsAClick | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-3; BR-2, BR-3; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC03_theReferrerIsStoredAsItsOriginOnly | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-4; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC04_theUserAgentIsStoredAsAClassOnly | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-6; BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC06_forwardingHeadersDoNotChangeTheRecordedClient | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-17; BR-2, BR-7; NFR-P1 | click.ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-14; BR-5; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-15; BR-5, BR-9; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-16; BR-1, BR-2, BR-6; FR-7 | click.ClickResilienceJourneyTest#AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.ClickResilienceJourneyTest#AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-22; NFR-A1; BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-23; NFR-A1; BR-6, BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-25; NFR-A2; BR-9; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-1; FR-1; BR-1, BR-2, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-2; FR-1; BR-1, BR-4; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-5; FR-5; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-6; FR-5, NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; FR-6 (surface regression); BR-1; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-8; FR-3; BR-2, BR-6, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-9; FR-3, FR-4; BR-6; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-10; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-11; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-14; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-15; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-12; FR-2; BR-2, BR-7; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-13; FR-4; BR-6, BR-8; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-7; AC-12; FR-2; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-17; FR-9; BR-5, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-19; NFR-R5; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS on a7c533f |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-21; FR-9; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS on a7c533f |

Release checks required by the locked SPEC (not green functional tests):

| AC / rule; requirement | Release check | Level | Result |
|---|---|---|---|
| AC-21; BR-9, BR-10, BR-12; NFR-X1, NFR-R1, NFR-O3 | scripts/smoke.sh#smoke; docker compose up --build / inspect health | release | PENDING container; supplemental jar smoke PASS |
| AC-22; BR-12; NFR-X1 | docker compose port urlshort 8080; docker inspect port bindings | release | PENDING; compose configuration checked |
| AC-23; BR-12; NFR-S5 | docker inspect; container write probes; scripts/smoke.sh#smoke | release | PENDING; Dockerfile/compose configuration checked |
| AC-24; BR-8, BR-12; NFR-R4 | scripts/smoke.sh#restart_mode; compose restart and down/up persistence | release | PENDING |
| AC-25; BR-13; NFR-R3 | scripts/smoke.sh#drain_mode | release | Supplemental candidate jar PASS: R0 201 within 1 s, refused probe, 62 ok / 20 refused / 0 losses / 0 failures; release record pending |
| AC-26; BR-11, BR-12; NFR-X1 | java -jar; environment overrides; scripts/smoke.sh#smoke | release | Supplemental unmodified jar PASS; release record pending |
| AC-27; BR-11; NFR-L1, NFR-L2 | scripts/smoke.sh#bench_mode | release | Mode exercised for 60 s; achieved 82.5 / 16.5 req/s, below specified input rates; numeric verdict PENDING |
| AC-28; BR-13; NFR-R3, NFR-X1 | scripts/smoke.sh#restart_under_load; docker inspect StopTimeout | release | PENDING |

BR-1 through BR-13 are represented above; BR-12/13 retain their release checks. Exact source-method and JUnit-invocation inventories are committed in the slice proof/. No executed method is unmapped.

## Re-check 03-operate — candidate 1c8b2cff20ad8b73a060bc817c8d0011782f876f

Independent QA, 2026-10-03 UTC: 165 unit / 155 functional invocations, zero failures/errors/skips. All 186 source methods (88 unit, 98 functional) map below and every invocation resolves to a mapped method. All AC-1–AC-20 have a fresh functional test and independent HTTP observation on this SHA. Inventories: slice proof/qa-source-methods-1c8b2cf.txt and qa-test-invocations-1c8b2cf.txt. The granted shifted-clock requests retain all their former assertions and timing; only their dedicated peers change.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#contextLoads | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; 01-ping AC-1, AC-2 regression; 02 AC-20/BR-8 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS on 1c8b2cf |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03a_anExactlyEmptyBucketRefillsOneTokenAfterExactlyOneSecond | unit | PASS on 1c8b2cf |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03b_retryAfterIsRoundedUpAndHonoured | unit | PASS on 1c8b2cf |
| AC-4; BR-2; NFR-R2 | web.RateLimiterTest#AC04_aQuietMinuteRefillsTheWholeBucket | unit | PASS on 1c8b2cf |
| AC-3, AC-9; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimiterTest#refusalsTakeNothing | unit | PASS on 1c8b2cf |
| AC-5, AC-6; BR-1, BR-2; NFR-R2 | web.RateLimiterTest#budgetsAndClientsAreIndependent | unit | PASS on 1c8b2cf |
| AC-3; BR-4; FR-10, NFR-R2 | web.RateLimiterTest#retryAfterIsTheWaitForOneTokenInWholeSeconds | unit | PASS on 1c8b2cf |
| AC-1, AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2; CR-01/SEC-01 ordering regression | web.RateLimiterTest#aRequestOvertakenByNewerOnesDecidesOnTheTimeItReachesTheBucket | unit | PASS on 1c8b2cf; unchanged reviewer probe confirms 60 admissions |
| BR-2; NFR-R2; disclosed fail-closed backward-clock limit, lead transition 726 | web.RateLimiterTest#afterABackwardClockStepTheBucketRefillsFromItsStoredTat | unit | PASS on 1c8b2cf; Retry-After 61 after 60s rollback |
| BR-8; NFR-R2; CR-03 rollback cleanup regression | web.RateLimiterTest#theReleaseResumesAfterABackwardClockStep | unit | PASS on 1c8b2cf; reviewer probe confirms 2 clients after 61s |
| BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#fullBucketsAreReleasedByTheNextRequestButNotWhileIdle | unit | PASS on 1c8b2cf |
| BR-2, BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#aBucketThatIsNotYetFullSurvivesTheRelease | unit | PASS on 1c8b2cf |
| BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#theReleaseRunsAtMostOncePerSecond | unit | PASS on 1c8b2cf |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_limitedRequestsAreChargedToTheirBudget | unit | PASS on 1c8b2cf |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_operatorSurfacesAreNeitherChargedNorLimited | unit | PASS on 1c8b2cf |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop | unit | PASS on 1c8b2cf |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy | unit | PASS on 1c8b2cf |
| AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#theFilterReadsOnlyXForwardedForAndOnlyFromATrustedPeer | unit | PASS on 1c8b2cf |
| AC-1, AC-2, AC-11; BR-3, BR-4, BR-6; FR-10, NFR-R2, NFR-P1 | web.RateLimitFilterTest#anEmptyBucketIsA429ProblemWithRetryAfterAndNoChain | unit | PASS on 1c8b2cf |
| AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitFilterTest#eachRejectionIsCountedOnceUnderItsBudgetOnly | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#anotherAddressOrAnotherDayHashesDifferently | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#theHashIsNeverTheUnsaltedDigest | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#closeDropsTheSalt | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#noRowsIsZeroAndTwoEmptyLists | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#referrersAreRankedByClicksThenByCodePointAndCappedAtTen | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; NFR-P1; migration constraints after DDL connection retirement | click.ClickSchemaTest#theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aHeadRequestIsNotAClick | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aRedirectIsStoredAsItsReducedFactsOnly | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeReducedIsOneWarn | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFastStoreIsDrainedOnCloseAndNothingIsReported | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerIsReducedToItsOrigin | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#aReferrerThatIsNotAnHttpOriginIsNone | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerLengthCapIs2048 | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theUserAgentIsReducedToAClass | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#anAbsentOrEmptyUserAgentIsUnknown | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#rule2_aClickHoldsOnlyTheReducedFacts | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#validUrlsPass | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-6; FR-4; 02 AC-20/BR-8 regression | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; existing health surface regression; 02 AC-20/BR-8 regression | HealthJourneyTest#healthEndpointReportsUp | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC1 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC2 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC3 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC4 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC5 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC6 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC7 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC8 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS on 1c8b2cf |
| AC-25, AC-28; BR-13; NFR-R3 (configuration prerequisite only) | web.ShutdownPhaseDefaultTest#theShippedGracefulShutdownPhaseIsTenSeconds | functional | PASS on 1c8b2cf; config assertion, release outcome pending |
| AC-8; BR-5; NFR-R2 | web.TrustedProxyJourneyTest#AC08_aTrustedProxysForwardedAddressIdentifiesTheClient | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS on 1c8b2cf |
| AC-14, AC-15; BR-9; NFR-R1 | web.DatabaseDownJourneyTest#AC14_AC15_readinessFollowsTheDatabaseAndLivenessDoesNot | functional | PASS on 1c8b2cf |
| AC-13; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC13_livenessAndReadinessAreUpWithAWorkingDatabase | functional | PASS on 1c8b2cf |
| AC-15; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC15_healthBodiesDiscloseNothingAboutTheInstallation | functional | PASS on 1c8b2cf |
| AC-18; BR-10; NFR-O3 | web.HealthMetricsJourneyTest#AC18_redirectsAreCountedByRouteTemplate | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS on 1c8b2cf |
| AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theShippedConfigurationCarriesTheDecidedBudgets | functional | PASS on 1c8b2cf |
| AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theSettingsRecordDefaultsToTheSameBudgets | functional | PASS on 1c8b2cf |
| AC-10; BR-11; NFR-R2 | web.RateLimitSettingsJourneyTest#AC10_theBudgetsAreOperatorSettings | functional | PASS on 1c8b2cf |
| AC-1; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC01_theCreateBudgetAdmits60AndRefusesThe61st | functional | PASS on 1c8b2cf |
| AC-2; BR-1, BR-2; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC02_theRedirectBudgetAdmits600AndRefusesThe601st | functional | PASS on 1c8b2cf |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03a_retryAfterIsTruthfulFromAnExactlyEmptyBucket | functional | PASS on 1c8b2cf |
| AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03b_retryAfterIsAnUpperBoundFromAPartlyRefilledBucket | functional | PASS on 1c8b2cf |
| AC-4; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC04_aFullBudgetReturnsAfterAQuietMinute | functional | PASS on 1c8b2cf |
| AC-5; BR-1, BR-2; NFR-R2 | web.RateLimitJourneyTest#AC05_theTwoBudgetsAreIndependent | functional | PASS on 1c8b2cf |
| AC-6; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC06_clientsAreIndependent | functional | PASS on 1c8b2cf |
| AC-7; BR-5; NFR-R2 | web.RateLimitJourneyTest#AC07_aForgedForwardedAddressDoesNotChangeTheClient | functional | PASS on 1c8b2cf |
| AC-9; BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC09_everyRequestInABudgetCountsAndTheLimitIsCheckedFirst | functional | PASS on 1c8b2cf |
| AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_theTooManyRequestsProblemNamesNoClient | functional | PASS on 1c8b2cf |
| AC-12; BR-6, BR-7; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#AC12_eachRejectionIsLoggedOnceCorrelatedAndWithoutClientValues | functional | PASS on 1c8b2cf |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_percentEncodingDoesNotMoveARequestToTheLargerBudget | functional | PASS on 1c8b2cf |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_operatorSurfacesAreNeverLimited | functional | PASS on 1c8b2cf |
| BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_aDotDotSegmentUnderAnExemptPrefixReachesNoLimitedOperation | functional | PASS on 1c8b2cf |
| AC-16; BR-10; NFR-O3 | web.RateLimitJourneyTest#AC16_theMetricsSurfaceListsTheFourKindsOfMetric | functional | PASS on 1c8b2cf |
| AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitJourneyTest#AC17_everyRejectionIsCountedOnceByBudget | functional | PASS on 1c8b2cf |
| AC-19; BR-6, BR-10; NFR-O3, NFR-P1 | web.RateLimitJourneyTest#AC19_metricsAreExposedForScrapingWithoutClientOrLinkValues | functional | PASS on 1c8b2cf |
| BR-1, BR-6; design DR-01; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#designDR01_anInvalidPathUnderAnExemptPrefixLogsNoSubmittedValueOnTomcat | functional | PASS on 1c8b2cf |
| AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_onTomcatThe429IsTheSameProblemMediaTypeAsEveryOtherError | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; proof item 10; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3 | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3 | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS on 1c8b2cf |
| AC-20; BR-1; FR-10, NFR-M3 | web.OpenApiDocumentTest#AC20_everyOperationDocumentsTheTooManyRequestsProblem | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; FR-7, NFR-P1; pooled-connection retirement smoke | click.ClickSchemaJourneyTest#everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-7; BR-7; FR-8 | click.StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-8, AC-11; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC08_totalClicksCountsEveryRedirect | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-9, AC-11; BR-2, BR-7; FR-8 | click.StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-10, AC-11; BR-3, BR-7; FR-8 | click.StatsJourneyTest#AC10_topReferrersAreRankedAndCapped | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-12; BR-1, BR-7, BR-8; FR-8 | click.StatsJourneyTest#AC12_aRetiredLinksStatisticsAreStillReadable | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-13; BR-7, BR-9; FR-8 | click.StatsJourneyTest#AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-19; BR-9; NFR-O1, NFR-O2 | click.StatsJourneyTest#AC19_theStatisticsPathAndASettledRedirectAreCorrelated | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-20; BR-8; FR-2, FR-4, FR-6, NFR-A1 regression | click.StatsJourneyTest#AC20_redirectAndAuditBehaviourAreUnchanged | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-21; BR-7; NFR-M3 | click.StatsJourneyTest#AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-1; BR-1, BR-2, BR-6; FR-7 | click.ClickRecordingJourneyTest#AC01_aRedirectRecordsExactlyOneClickWithItsTime | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-2; BR-1; FR-7 | click.ClickRecordingJourneyTest#AC02_onlyARedirectIsAClick | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-3; BR-2, BR-3; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC03_theReferrerIsStoredAsItsOriginOnly | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-4; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC04_theUserAgentIsStoredAsAClassOnly | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-5; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-6; BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC06_forwardingHeadersDoNotChangeTheRecordedClient | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-17; BR-2, BR-7; NFR-P1 | click.ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-14; BR-5; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-15; BR-5, BR-9; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-16; BR-1, BR-2, BR-6; FR-7 | click.ClickResilienceJourneyTest#AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.ClickResilienceJourneyTest#AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-22; NFR-A1; BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-23; NFR-A1; BR-6, BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-25; NFR-A2; BR-9; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-1; FR-1; BR-1, BR-2, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-2; FR-1; BR-1, BR-4; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-5; FR-5; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-6; FR-5, NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; FR-6 (surface regression); BR-1; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-8; FR-3; BR-2, BR-6, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-9; FR-3, FR-4; BR-6; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-10; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-11; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-14; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-15; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-12; FR-2; BR-2, BR-7; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-13; FR-4; BR-6, BR-8; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-7; AC-12; FR-2; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-17; FR-9; BR-5, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-19; NFR-R5; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS on 1c8b2cf |
| Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-21; FR-9; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS on 1c8b2cf |

Locked SPEC release checks:

| AC / rule; requirement | Release check | Level | Result |
|---|---|---|---|
| AC-21; BR-9, BR-10, BR-12; NFR-X1, NFR-R1, NFR-O3 | scripts/smoke.sh#smoke; docker compose up --build / inspect health | release | PENDING container; supplemental jar smoke PASS |
| AC-22; BR-12; NFR-X1 | docker compose port urlshort 8080; docker inspect port bindings | release | PENDING runtime; configuration identical to previously inspected a7c533f |
| AC-23; BR-12; NFR-S5 | docker inspect; container write probes; scripts/smoke.sh#smoke | release | PENDING runtime; configuration identical to previously inspected a7c533f |
| AC-24; BR-8, BR-12; NFR-R4 | scripts/smoke.sh#restart_mode; compose restart and down/up persistence | release | PENDING |
| AC-25; BR-13; NFR-R3 | scripts/smoke.sh#drain_mode | release | Supplemental unmodified jar PASS: complete R0 201, curl exit 0 at 532 ms; refused probe; 62 ok / 16 refused / 0 losses / 0 failures. Strict R0 controls 8/8; supported C locale; release record pending |
| AC-26; BR-11, BR-12; NFR-X1 | java -jar; environment overrides; scripts/smoke.sh#smoke | release | Supplemental unmodified jar PASS; release record pending |
| AC-27; BR-11; NFR-L1, NFR-L2 | scripts/smoke.sh#bench_mode | release | 60s mode exercised: achieved 82.1 / 16.4 req/s, below specified 100/20; numeric judgment PENDING |
| AC-28; BR-13; NFR-R3, NFR-X1 | scripts/smoke.sh#restart_under_load; docker inspect StopTimeout | release | PENDING |

BR-1 through BR-13 remain represented; BR-12/13 require release evidence. No product source or test changed by QA. Backward Clock steps are outside the contract and fail closed under the lead decision; the sweep margin for arbitrary delayed concurrent clock reads was not stress-tested.


## 01-audit-read — candidate 35590f06c852543c29097a42c43b7802be90ba40

Independent QA 2026-10-03: fresh offline gate, 200 unit / 200 functional invocations, zero failures/errors/skips; merged CSV 492/492 lines and 190/190 branches. All 223 source methods (100 unit, 123 functional) map below. Parameterized JUnit display names may omit their owning method; the source inventory preserves that association rather than inventing names. Full green class XML and invocation inventories are in slice proof/qa-test-results-35590f0 and qa-test-invocations-35590f0.json; named-method inventory is qa-source-methods-35590f0.json.

AC-17 additionally replays the unmodified f6dd29e functional sources: 155 invocations, 153 pass, exactly the two OpenApiDocumentTest enumeration assertions fail. Their candidate versions pass. Lead grant qitem-20261003182833-40a842ff transition1156, commit428e9e1, explicitly permits only those additions. This qualified result is not a claim that all 155 original assertions are green. Every inherited functional row below is also part of this replay (apart from the two named enumeration failures). AC-18 has AuditUpgradeJourneyTest and independent installed shipped/candidate jar captures, named qa-upgrade-before/after.json and qa-http/upgrade-*.

By-effect evidence is independent of the builder: 271 HTTP captures, 276 correlated request log events, read-only snapshots and exact stored-row comparisons; qa-verification-35590f0.json records 1,641 reconciliation assertions. AC-11..14 use controlled per-request Servlet peers as SPEC authorizes, plus real Tomcat for forwarded-header overrides; they do not establish an actual remote TCP-client boundary. See PROOF.md QA and GAPS.md for the scope.

| AC / business rule; product requirement | Test class#method | Suite | Result |
|---|---|---|---|
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#contextLoads | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; bootstrap prerequisite; NFR-M1; 02 AC-20/BR-8 regression | UrlshortApplicationTests#mainBootsWithoutAWebServer | unit | PASS on 35590f0 |
| AC-12; BR-2; NFR-S6 | audit.AuditControllerTest#loopbackPeersAreAdmitted | unit | PASS on 35590f0 |
| AC-11; BR-2, BR-6; NFR-S6 | audit.AuditControllerTest#otherPeersAreRefused | unit | PASS on 35590f0 |
| AC-11; BR-2, BR-6; NFR-S6 | audit.AuditControllerTest#aMissingPeerIsRefused | unit | PASS on 35590f0 |
| AC-13; BR-2, BR-6; NFR-S6 | audit.AuditControllerTest#aForwardingHeaderRefusesEvenFromLoopback | unit | PASS on 35590f0 |
| AC-12, AC-14; BR-2; NFR-S6 | audit.AuditControllerTest#theShippedStrategyAdmitsALoopbackRequest | unit | PASS on 35590f0 |
| AC-14; BR-2; NFR-S6 | audit.AuditControllerTest#anyOtherStrategyClosesTheEndpoint | unit | PASS on 35590f0 |
| AC-14; BR-2; NFR-S6 | audit.AuditControllerTest#anUnsetStrategyClosesTheEndpoint | unit | PASS on 35590f0 |
| AC-7, AC-9; BR-5, BR-6; FR-17 | audit.AuditControllerTest#limitDefaultsTo50AndAcceptsItsBounds | unit | PASS on 35590f0 |
| AC-9; BR-5, BR-6; FR-17 | audit.AuditControllerTest#limitOutsideItsRangeIsARangeProblem | unit | PASS on 35590f0 |
| AC-9; BR-5, BR-6; FR-17 | audit.AuditControllerTest#aLimitThatIsNotAWholeNumberIsAFormatProblem | unit | PASS on 35590f0 |
| AC-6, AC-9; BR-5, BR-6; FR-17 | audit.AuditControllerTest#aCursorIsTheBase64urlOfAPositiveId | unit | PASS on 35590f0 |
| AC-9; BR-5, BR-6; FR-17 | audit.AuditControllerTest#aMalformedCursorIsAFormatProblem | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-9; AC-22, AC-23, AC-25; NFR-A1, NFR-A2; 02 AC-20/BR-8 regression | audit.AuditLogTest#appendWritesOneRowWithServerOwnedValues | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aHeadRequestIsNotAClick | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aRedirectIsStoredAsItsReducedFactsOnly | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClickThatCannotBeReducedIsOneWarn | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aFastStoreIsDrainedOnCloseAndNothingIsReported | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-2, AC-14, AC-15, AC-19; BR-1, BR-2, BR-5, BR-9; FR-7, NFR-L3, NFR-O1/O2; bounded shutdown accounting | click.ClickRecorderTest#aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; NFR-P1; migration constraints after DDL connection retirement | click.ClickSchemaTest#theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerIsReducedToItsOrigin | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#aReferrerThatIsNotAnHttpOriginIsNone | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theReferrerLengthCapIs2048 | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#theUserAgentIsReducedToAClass | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#anAbsentOrEmptyUserAgentIsUnknown | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3, AC-4; BR-2, BR-3, BR-4; FR-7, NFR-P1 | click.ClickTest#rule2_aClickHoldsOnlyTheReducedFacts | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#anotherAddressOrAnotherDayHashesDifferently | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#theHashIsNeverTheUnsaltedDigest | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#closeDropsTheSalt | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-4; NFR-P1; salt lifetime and midnight ownership | click.DailySaltTest#aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#noRowsIsZeroAndTwoEmptyLists | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#daysAreSummedAscendingAndClicksWithoutAReferrerCountOnlyInTheTotals | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-7, AC-9, AC-10, AC-11; BR-3, BR-7; FR-8 | click.LinkStatsTest#referrersAreRankedByClicksThenByCodePointAndCappedAtTen | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithoutAKeyInsertsAndAuditsTheNewLink | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#createWithAnUnboundKeyBindsIt | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#replayWithinTheWindowReturnsTheBoundLinkAndWritesNothing | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#mismatchWithinTheWindowIs422AndKeepsTheBinding | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#anExpiredKeyIsReleasedAndBindsANewLink | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#readOfAnUnknownCodeIs404 | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#resolveSendsVisitorsToActiveLinksOnly | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireUpdatesConditionallyAndAuditsBeforeAndAfter | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-4, BR-5, BR-6, BR-9; FR-1, FR-4, FR-9, NFR-A1; 02 AC-20/BR-8 regression | link.LinkServiceTest#retireThatChangesNoRowIs410AndWritesNoAuditRow | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-6; FR-4; 02 AC-20/BR-8 regression | link.LinkTest#stateIsActiveUntilRetiredAtIsSet | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#eachRejectedUrlFailsExactlyItsRule | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#validUrlsPass | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#exactly2048CharactersIsAccepted | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#malformedKeysFailFormat | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#keysLongerThan255FailFormat | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-3, BR-5, BR-8; AC-4, AC-20; FR-5, NFR-S1; 02 AC-20/BR-8 regression | link.LinkValidationTest#absentOrVisibleAsciiKeysPass | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#codesAreEightCharactersFromTheAlphanumericAlphabet | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-1; FR-1; 02 AC-20/BR-8 regression | link.ShortCodesTest#aDrawThatSpellsAReservedSegmentIsDrawnAgain | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; 01-ping AC-1, AC-2 regression; 02 AC-20/BR-8 regression | ping.PingControllerTest#answersOkWithTheCurrentUtcInstant | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unwrapsABodyLimitErrorRaisedInsideTheJsonReaderTo413 | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unreadableBodyWithoutALimitErrorStays400WithoutTheFrameworkDetail | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#frameworkDetailThatEchoesClientInputIsCleared | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#domainProblemKeepsItsErrorsAndGetsTheRequestIdAsInstance | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#unhandledExceptionIsABare500AndOneMessageFreeEvent | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#errorOriginIsNoneWhenNoFrameIsOurs | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-5, AC-7, AC-24, AC-26, AC-27; NFR-R6; 02 AC-20/BR-8 regression | web.ProblemDetailsAdviceTest#aNonProblemBodyPassesThroughUntouched | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#validationIs400WithExactlyOneFieldError | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#notFoundAndGoneAreBareProblems | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8; AC-4, AC-14, AC-18, AC-20; FR-5, FR-6, FR-9; 02 AC-20/BR-8 regression | web.ProblemsTest#idempotencyMismatchIs422NamingTheHeaderAndRule | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_limitedRequestsAreChargedToTheirBudget | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1; request classification/exemptions; NFR-R2 | web.RateLimitFilterTest#rule1_operatorSurfacesAreNeitherChargedNorLimited | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_theClientIsThePeerOrTheRightMostUntrustedForwardedHop | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#rule5_anAbsentOrEmptyHeaderFromATrustedProxyChargesTheProxy | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-7, AC-8; BR-5; NFR-R2 | web.RateLimitFilterTest#theFilterReadsOnlyXForwardedForAndOnlyFromATrustedPeer | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-1, AC-2, AC-11; BR-3, BR-4, BR-6; FR-10, NFR-R2, NFR-P1 | web.RateLimitFilterTest#anEmptyBucketIsA429ProblemWithRetryAfterAndNoChain | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitFilterTest#eachRejectionIsCountedOnceUnderItsBudgetOnly | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03a_anExactlyEmptyBucketRefillsOneTokenAfterExactlyOneSecond | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimiterTest#AC03b_retryAfterIsRoundedUpAndHonoured | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-4; BR-2; NFR-R2 | web.RateLimiterTest#AC04_aQuietMinuteRefillsTheWholeBucket | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3, AC-9; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimiterTest#refusalsTakeNothing | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-5, AC-6; BR-1, BR-2; NFR-R2 | web.RateLimiterTest#budgetsAndClientsAreIndependent | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3; BR-4; FR-10, NFR-R2 | web.RateLimiterTest#retryAfterIsTheWaitForOneTokenInWholeSeconds | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-1, AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2; CR-01/SEC-01 ordering regression | web.RateLimiterTest#aRequestOvertakenByNewerOnesDecidesOnTheTimeItReachesTheBucket | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-2; NFR-R2; disclosed fail-closed backward-clock limit, lead transition 726 | web.RateLimiterTest#afterABackwardClockStepTheBucketRefillsFromItsStoredTat | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#fullBucketsAreReleasedByTheNextRequestButNotWhileIdle | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-2, BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#aBucketThatIsNotYetFullSurvivesTheRelease | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-8; non-functional limiter memory bound; NFR-R2 | web.RateLimiterTest#theReleaseRunsAtMostOncePerSecond | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-8; NFR-R2; CR-03 rollback cleanup regression | web.RateLimiterTest#theReleaseResumesAfterABackwardClockStep | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#aBodyOfExactlyTheLimitIsReadInFull | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theFirstByteOverTheLimitIs413OnTheBulkReadPath | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#singleByteReadsCountTooAndEndOfStreamIsPassedThrough | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#theWrappedStreamIsCreatedOnceSoTheCountCannotBeReset | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; body-limit stream mechanics; 02 AC-20/BR-8 regression | web.RequestBodyLimitFilterTest#servletStreamStateAndListenerDelegateToTheContainerStream | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#issuesOneIdOnHeaderAndMdcBeforeTheChainRunsAndClearsMdcAfter | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#ignoresInboundRequestIdAndIssuesADifferentIdPerRequest | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#writesOneRequestCompletedEventWithTheStatusAndTheIdButNotTheMethod | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-10; AC-26, AC-27; NFR-O1, NFR-O2; 02 AC-20/BR-8 regression | web.RequestIdFilterTest#clearsMdcWhenTheChainThrows | unit | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; existing health surface regression; 02 AC-20/BR-8 regression | HealthJourneyTest#healthEndpointReportsUp | functional | PASS on 35590f0 |
| AC-13, AC-14; BR-2, BR-6; NFR-S6 | audit.AuditAccessSettingsJourneyTest#AC13_AC14_noSettingOpensTheEndpointBeyondLoopback | functional | PASS on 35590f0 |
| AC-13, AC-14; BR-2, BR-6; NFR-S6 | audit.AuditAccessSettingsJourneyTest#aPlainLoopbackReadStillWorksUnderTheseSettings | functional | PASS on 35590f0 |
| AC-13, AC-14; BR-2, BR-6; NFR-S6 | audit.AuditForwardedHeadersJourneyTest#theShippedFilePinsTheForwardedHeaderStrategyOff | functional | PASS on 35590f0 |
| AC-13, AC-14; BR-2, BR-6; NFR-S6 | audit.AuditForwardedHeadersJourneyTest#onADetectedCloudPlatformThePinKeepsAForgedLoopbackHeaderOut | functional | PASS on 35590f0 |
| AC-13, AC-14; BR-2, BR-6; NFR-S6 | audit.AuditForwardedHeadersJourneyTest#anOverrideOfThePinClosesTheEndpoint | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-22; NFR-A1; BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC22_createWritesExactlyOneAuditRow | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-23; NFR-A1; BR-6, BR-9, BR-12; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC23_retireWritesExactlyOneAuditRow | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-24; NFR-A1, NFR-R6; BR-6, BR-8, BR-9, BR-10; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC24_aFailedAuditWriteRollsTheRetireBackAndFailsClosed | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-25; NFR-A2; BR-9; 02 AC-20/BR-8 regression | audit.AuditJourneyTest#AC25_auditRowsAreAppendOnlyUnderEveryOperation | functional | PASS on 35590f0 |
| AC-21; BR-6, BR-7; FR-17, NFR-R6; induction: functional AuditTrail spy throws DataAccessResourceFailureException; independent live JDBC prepareStatement throws SQLException | audit.AuditReadFailureJourneyTest#AC21_aFailedReadIsA500ProblemNeverAnEmptyOrPartialPage | functional | PASS on 35590f0 |
| AC-1; BR-1, BR-3; FR-17 | audit.AuditReadJourneyTest#AC01_anEmptyTrailIsAnEmptyPage | functional | PASS on 35590f0 |
| AC-2; BR-3; FR-17 | audit.AuditReadJourneyTest#AC02_aCreateRowIsReadableAsWritten | functional | PASS on 35590f0 |
| AC-3; BR-3, BR-8; FR-17 | audit.AuditReadJourneyTest#AC03_aRetireRowCarriesBeforeAndAfter | functional | PASS on 35590f0 |
| AC-4; BR-4; FR-17 | audit.AuditReadJourneyTest#AC04_rowsComeNewestFirstInTheOrderTheyWereWritten | functional | PASS on 35590f0 |
| AC-5; BR-3; FR-17 | audit.AuditReadJourneyTest#AC05_everyFieldMatchesTheStoredRow | functional | PASS on 35590f0 |
| AC-6; BR-5; FR-17 | audit.AuditReadJourneyTest#AC06_pagesFollowNextToTheEnd | functional | PASS on 35590f0 |
| AC-7; BR-5; FR-17 | audit.AuditReadJourneyTest#AC07_theDefaultAndMaximumPageSizes | functional | PASS on 35590f0 |
| AC-8; BR-4, BR-5; FR-17 | audit.AuditReadJourneyTest#AC08_pagingIsStableWhileRowsAreWritten | functional | PASS on 35590f0 |
| AC-9; BR-5, BR-6; FR-17 | audit.AuditReadJourneyTest#AC09_invalidPagingParametersAreRefusedNamingTheField | functional | PASS on 35590f0 |
| AC-10; BR-1, BR-8; FR-17, NFR-A2 | audit.AuditReadJourneyTest#AC10_readingChangesNothing | functional | PASS on 35590f0 |
| AC-11; BR-2, BR-6; NFR-S6 | audit.AuditReadJourneyTest#AC11_aNonLoopbackClientIsRefused | functional | PASS on 35590f0 |
| AC-12; BR-2; NFR-S6 | audit.AuditReadJourneyTest#AC12_everyLoopbackAddressIsAdmitted | functional | PASS on 35590f0 |
| AC-13; BR-2, BR-6; NFR-S6 | audit.AuditReadJourneyTest#AC13_forwardingHeadersNeverGrantAccess | functional | PASS on 35590f0 |
| AC-9, AC-11, AC-13; BR-2, BR-5, BR-6; FR-17, NFR-S6 | audit.AuditReadJourneyTest#theGuardAndTheValidationComeBeforeContentNegotiation | functional | PASS on 35590f0 |
| AC-15; BR-7; NFR-O1, NFR-O2 | audit.AuditReadJourneyTest#AC15_requestCorrelationOnTheNewPaths | functional | PASS on 35590f0 |
| AC-16; BR-7; NFR-O2 | audit.AuditReadJourneyTest#AC16_auditContentAndClientValuesStayOutOfTheLogs | functional | PASS on 35590f0 |
| AC-19; BR-1, BR-9; NFR-M3 | audit.AuditReadJourneyTest#AC19_theLiveApiDocumentDescribesTheAuditRead | functional | PASS on 35590f0 |
| AC-20; BR-4, BR-5; FR-17; induction: actual JDBC connection with autoCommit=false, held INSERT then commit after first page | audit.AuditReadJourneyTest#AC20_aTraversalAcrossAnInFlightWriteNeitherRepeatsNorSkipsCommittedRows | functional | PASS on 35590f0 |
| AC-18; BR-8; FR-13, NFR-X2 | audit.AuditUpgradeJourneyTest#AC18_anExistingDatabaseUpgradesInPlaceAndKeepsItsLinksAndRows | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1; BR-1, BR-2, BR-6; FR-7 | click.ClickRecordingJourneyTest#AC01_aRedirectRecordsExactlyOneClickWithItsTime | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-2; BR-1; FR-7 | click.ClickRecordingJourneyTest#AC02_onlyARedirectIsAClick | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-3; BR-2, BR-3; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC03_theReferrerIsStoredAsItsOriginOnly | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-4; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC04_theUserAgentIsStoredAsAClassOnly | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-5; BR-2, BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-6; BR-4; FR-7, NFR-P1 | click.ClickRecordingJourneyTest#AC06_forwardingHeadersDoNotChangeTheRecordedClient | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-17; BR-2, BR-7; NFR-P1 | click.ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-14; BR-5; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-15; BR-5, BR-9; FR-7, NFR-L3 | click.ClickResilienceJourneyTest#AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-16; BR-1, BR-2, BR-6; FR-7 | click.ClickResilienceJourneyTest#AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-18; BR-9; NFR-O2, NFR-P1 | click.ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.ClickResilienceJourneyTest#AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-1, AC-4; BR-2, BR-4; FR-7, NFR-P1; pooled-connection retirement smoke | click.ClickSchemaJourneyTest#everyUserAgentClassIsRecordedAfterThePoolRetiresItsConnections | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-7; BR-7; FR-8 | click.StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-8, AC-11; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC08_totalClicksCountsEveryRedirect | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-9, AC-11; BR-2, BR-7; FR-8 | click.StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-10, AC-11; BR-3, BR-7; FR-8 | click.StatsJourneyTest#AC10_topReferrersAreRankedAndCapped | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-12; BR-1, BR-7, BR-8; FR-8 | click.StatsJourneyTest#AC12_aRetiredLinksStatisticsAreStillReadable | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-13; BR-7, BR-9; FR-8 | click.StatsJourneyTest#AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-19; BR-9; NFR-O1, NFR-O2 | click.StatsJourneyTest#AC19_theStatisticsPathAndASettledRedirectAreCorrelated | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-20; BR-8; FR-2, FR-4, FR-6, NFR-A1 regression | click.StatsJourneyTest#AC20_redirectAndAuditBehaviourAreUnchanged | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-21; BR-7; NFR-M3 | click.StatsJourneyTest#AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: AC-22; BR-1, BR-7; FR-8 | click.StatsJourneyTest#AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-17; FR-9; BR-5, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC17_aReplayReturnsTheFirstLink | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-18; FR-9, NFR-R5; BR-5, BR-8, BR-9; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC18_sameKeyWithADifferentUrlIsRefusedAndTheBindingSurvives | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-19; NFR-R5; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aMalformedKeyIsRefused | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-20; FR-9, FR-5; BR-5, BR-8; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC20_aKeyOf255VisibleCharactersIsAccepted | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-21; FR-9; BR-5; 02 AC-20/BR-8 regression | link.IdempotencyJourneyTest#AC21_aRejectedCreateDoesNotConsumeTheKey | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-1; FR-1; BR-1, BR-2, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC01_validUrlBecomesAShortLink | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-2; FR-1; BR-1, BR-4; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC02_everyCreateWithoutAKeyIsANewLink | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-4; FR-5, NFR-S1; BR-3, BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC04_aUrlOfExactly2048CharactersIsAccepted | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-5; FR-5; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC05_bodyThatIsNotAJsonObjectIsRefused | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-6; FR-5, NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC06_nonJsonContentTypeIsRefused | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-7; NFR-S3; BR-8; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC07_bodyIsRefusedAtTheSixteenKibLimit | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; FR-6 (surface regression); BR-1; 02 AC-20/BR-8 regression | link.LinkCreateJourneyTest#AC16_redirectRouteDoesNotShadowTheExistingSurface | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-8; FR-3; BR-2, BR-6, BR-11, BR-12; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC08_readingALinkReturnsItsDetails | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-9; FR-3, FR-4; BR-6; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC09_aRetiredLinkIsStillReadableWithItsState | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-10; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC10_retiringALinkIs204WithAnEmptyBody | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-11; FR-4; BR-6, BR-9; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC11_retiringAnAlreadyRetiredLinkIs410AndNotASecondMutation | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-14; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC14_unknownCodeIs404OnEveryLinkOperation | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-15; FR-6; BR-8; 02 AC-20/BR-8 regression | link.LinkReadRetireJourneyTest#AC15_wrongMethodIs405 | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-3; FR-1, NFR-S4; BR-11; 02 AC-20/BR-8 regression | link.PublicBaseUrlJourneyTest#AC03_shortUrlUsesTheConfiguredBaseNeverTheHostHeader | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-12; FR-2; BR-2, BR-7; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC12_visitorIsRedirectedWithANonCacheable302 | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-13; FR-4; BR-6, BR-8; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#AC13_aRetiredLinkTellsTheVisitorItIsGone | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-7; AC-12; FR-2; 02 AC-20/BR-8 regression | link.RedirectJourneyTest#rule7_queryStringOnTheShortLinkIsNotForwarded | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC1 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC1_pingAnswersOkAsJson | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC2 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC2_timeIsCurrentUtcInstant | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC3 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC3_everyResponseCarriesRequestId | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC4 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC4_requestIdsAreUniquePerRequest | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC5 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC5_wrongMethodIsProblemDetailWithRequestId | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC6 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC6_pingIsLoggedAsJsonWithRequestId | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC7 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC7_logEventCarriesNoClientAddressOrUserAgent | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-16; BR-10, BR-12; 01-ping AC8 regression; 02 AC-20/BR-8 regression | ping.PingJourneyTest#AC8_clientSuppliedRequestIdIsIgnored | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ColdStartJourneyTest#AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-14, AC-15; BR-9; NFR-R1 | web.DatabaseDownJourneyTest#AC14_AC15_readinessFollowsTheDatabaseAndLivenessDoesNot | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-13; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC13_livenessAndReadinessAreUpWithAWorkingDatabase | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-15; BR-9; NFR-R1 | web.HealthMetricsJourneyTest#AC15_healthBodiesDiscloseNothingAboutTheInstallation | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-18; BR-10; NFR-O3 | web.HealthMetricsJourneyTest#AC18_redirectsAreCountedByRouteTemplate | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-26; NFR-O1, NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_noClientControlledValueReachesTheLogs | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-27; NFR-O2; BR-10; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: BR-8, BR-10; AC-27; FR-5, FR-6, NFR-O2; 02 AC-20/BR-8 regression | web.ObservabilityJourneyTest#rule8_problemBodiesAndLogsNeverEchoASubmittedValue | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; proof item 10; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3 | web.OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: Inherited 02-analytics / 01-create-redirect: Inherited 01-create-redirect: AC-28; NFR-M3; 02 AC-20/BR-8 regression; current AC-20 / NFR-M3; AC-19; NFR-M3; enumeration grant 428e9e1 | web.OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice | functional | PASS on 35590f0; original version fails only its authorized enumeration |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-20; BR-1; FR-10, NFR-M3; AC-19; NFR-M3; enumeration grant 428e9e1 | web.OpenApiDocumentTest#AC20_everyOperationDocumentsTheTooManyRequestsProblem | functional | PASS on 35590f0; original version fails only its authorized enumeration |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theShippedConfigurationCarriesTheDecidedBudgets | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-1, AC-2, AC-10; BR-11; NFR-R2 | web.RateLimitDefaultsTest#theSettingsRecordDefaultsToTheSameBudgets | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-1; BR-2, BR-3; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC01_theCreateBudgetAdmits60AndRefusesThe61st | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-2; BR-1, BR-2; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC02_theRedirectBudgetAdmits600AndRefusesThe601st | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03a_retryAfterIsTruthfulFromAnExactlyEmptyBucket | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-3; BR-2, BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC03b_retryAfterIsAnUpperBoundFromAPartlyRefilledBucket | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-4; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC04_aFullBudgetReturnsAfterAQuietMinute | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-5; BR-1, BR-2; NFR-R2 | web.RateLimitJourneyTest#AC05_theTwoBudgetsAreIndependent | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-6; BR-2; NFR-R2 | web.RateLimitJourneyTest#AC06_clientsAreIndependent | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-7; BR-5; NFR-R2 | web.RateLimitJourneyTest#AC07_aForgedForwardedAddressDoesNotChangeTheClient | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-9; BR-3, BR-4; FR-10, NFR-R2 | web.RateLimitJourneyTest#AC09_everyRequestInABudgetCountsAndTheLimitIsCheckedFirst | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_theTooManyRequestsProblemNamesNoClient | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-12; BR-6, BR-7; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#AC12_eachRejectionIsLoggedOnceCorrelatedAndWithoutClientValues | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_percentEncodingDoesNotMoveARequestToTheLargerBudget | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_operatorSurfacesAreNeverLimited | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1; request classification/exemptions; NFR-R2 | web.RateLimitJourneyTest#rule1_aDotDotSegmentUnderAnExemptPrefixReachesNoLimitedOperation | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-16; BR-10; NFR-O3 | web.RateLimitJourneyTest#AC16_theMetricsSurfaceListsTheFourKindsOfMetric | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-17; BR-6, BR-10; FR-10, NFR-O3 | web.RateLimitJourneyTest#AC17_everyRejectionIsCountedOnceByBudget | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-19; BR-6, BR-10; NFR-O3, NFR-P1 | web.RateLimitJourneyTest#AC19_metricsAreExposedForScrapingWithoutClientOrLinkValues | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: BR-1, BR-6; design DR-01; NFR-O1/O2, NFR-P1 | web.RateLimitJourneyTest#designDR01_anInvalidPathUnderAnExemptPrefixLogsNoSubmittedValueOnTomcat | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-11; BR-6; FR-10, NFR-O1, NFR-P1 | web.RateLimitJourneyTest#AC11_onTomcatThe429IsTheSameProblemMediaTypeAsEveryOtherError | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-10; BR-11; NFR-R2 | web.RateLimitSettingsJourneyTest#AC10_theBudgetsAreOperatorSettings | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-25, AC-28; BR-13; NFR-R3 (configuration prerequisite only) | web.ShutdownPhaseDefaultTest#theShippedGracefulShutdownPhaseIsTenSeconds | functional | PASS on 35590f0 |
| AC-17 / BR-8; FR-13 regression; baseline 03-operate and earlier slices: AC-8; BR-5; NFR-R2 | web.TrustedProxyJourneyTest#AC08_aTrustedProxysForwardedAddressIdentifiesTheClient | functional | PASS on 35590f0 |

All AC-1..AC-21 have functional evidence: AC-17 is the unchanged pre-change functional suite with its named grant; the other ACs have the named functional methods above. BR-1..BR-8 map directly above. BR-9 (inherited /api budget, safe 429 + Retry-After) additionally maps to web.RateLimitJourneyTest#AC01_theCreateBudgetAdmits60AndRefusesThe61st, web.OpenApiDocumentTest#AC20_everyOperationDocumentsTheTooManyRequestsProblem and the independent 90-request audit burst: first60=200, next30=429. No source test is unmapped; no merged coverage shortfall or exclusion. Security proof-contract item12 remains downstream, awaiting an independent record.
