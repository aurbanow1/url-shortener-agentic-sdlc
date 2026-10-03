# Accumulated product file ledger — 7636264..8e9c065

82 product/build/test/API files. Every implementation and new operations test was read; unchanged inherited tests retain the earlier full review by exact Git blob identity and were executed again in the integrated gate. Modified inherited tests were reviewed against their prior full source and complete delta. This is a wave integration review, not a new claim to have independently judged this reviewer’s own historical review files.

| File | Verdict | Target blob |
|---|---|---|
| `Dockerfile` | PASS — integrated source/configuration read in full and checked against contracts | `ccd9225fc92056cda7263159209b8f12bb65dcab` |
| `build.gradle.kts` | PASS — integrated source/configuration read in full and checked against contracts | `69b237dcdfad53cdc2098ac3cf71e1cc706342c1` |
| `compose.yaml` | PASS — integrated source/configuration read in full and checked against contracts | `2cf1618a6729e64bceae6379269e25a9fbdbbdc5` |
| `docs/api/openapi.json` | MEDIUM carry-over CR-01/W2D-01 — live document matches, but ProblemDetail schema misdescribes extensions | `d0242544ae0668f86ed07fb67db31da9f5a7a665` |
| `scripts/smoke.sh` | PASS with LOW host prerequisite — strict R0 verdict and reconciliation inspected; installed/container/target-rate proof remains release-owned | `bc46d4ec79facc2962f91eade2c6d0ac125631c8` |
| `src/functionalTest/java/dev/urlshort/audit/AuditJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `5173ff1156f89136a23517b831687d1cf36a23b5` |
| `src/functionalTest/java/dev/urlshort/click/ClickRecordingJourneyTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `2d9fbd4d154c54e4dc6b14b4bae8ba0ea7075dd4` |
| `src/functionalTest/java/dev/urlshort/click/ClickResilienceJourneyTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `2ae14e221487cc11c7f9ea66b87e26d08bf7dae3` |
| `src/functionalTest/java/dev/urlshort/click/ClickSchemaJourneyTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `f4684355f5f4862e1cf8f235a38a6a48084a3a53` |
| `src/functionalTest/java/dev/urlshort/click/StatsJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `41767eef3089b79292208f521d453a81a1e1b668` |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `9cddd47d03697ddd9c474e76545b836844ca171e` |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClockConfig.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `9468a0f4356dda069286c0ec9a2dc6c53adbcc18` |
| `src/functionalTest/java/dev/urlshort/link/IdempotencyJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `1872ea5795705ae0b0c2751aa87460c383c1dbcc` |
| `src/functionalTest/java/dev/urlshort/link/LinkCreateJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `fc8d3155d857ca85277f3fb20365c4233a2c9933` |
| `src/functionalTest/java/dev/urlshort/link/LinkReadRetireJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `93862ffc21d50733f60876141039e966688bf224` |
| `src/functionalTest/java/dev/urlshort/link/PublicBaseUrlJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `9d43591e2dc1c7a4b352bf599a9dc7ee1115caf1` |
| `src/functionalTest/java/dev/urlshort/link/RedirectJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `8334d32ce0c7861c02f17c3921e26235eb1bd2e1` |
| `src/functionalTest/java/dev/urlshort/web/ColdStartJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `d952b27a793b19526f37931d1b092407f831db3b` |
| `src/functionalTest/java/dev/urlshort/web/DatabaseDownJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `b4dcedb96ed5ca51441917a57d88437568648315` |
| `src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `16000a4d0294ae5f2efdbeefcd0d5b5846945077` |
| `src/functionalTest/java/dev/urlshort/web/ObservabilityJourneyTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `6c78d164bf40fde1a464736f6aa7f563207ed6c9` |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `65f959e28e6d52f2b87410a59fb76dfe8f7d09d5` |
| `src/functionalTest/java/dev/urlshort/web/RateLimitDefaultsTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `e46a5c49fbf55b4b072f26c5de5798edf6e27ec5` |
| `src/functionalTest/java/dev/urlshort/web/RateLimitJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `f1439f7fbf43947c3b062b4b8e79ac964d87ce88` |
| `src/functionalTest/java/dev/urlshort/web/RateLimitSettingsJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `a2f8bfa42e31b96485a58aa458e6c3e33410727b` |
| `src/functionalTest/java/dev/urlshort/web/ShutdownPhaseDefaultTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `3274e564edfbd73d8e1e42134266592a29710a19` |
| `src/functionalTest/java/dev/urlshort/web/TrustedProxyJourneyTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `cc42a24cfc823d164d8d3a2dd893435873f22ac5` |
| `src/functionalTest/resources/application-functional.properties` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `181fbd67e84c6f1d112c9adf80d33398fd77c1db` |
| `src/main/java/dev/urlshort/UrlshortApplication.java` | PASS — integrated source/configuration read in full and checked against contracts | `e1e3beda29f2fc4773f0bd36a5cd04063f81e6ad` |
| `src/main/java/dev/urlshort/audit/AuditLog.java` | PASS — integrated source/configuration read in full and checked against contracts | `e2e0afbce0474cd3a516d995e8f99b87bcc5a92c` |
| `src/main/java/dev/urlshort/audit/package-info.java` | PASS — integrated source/configuration read in full and checked against contracts | `9b1830d5d9ee692c79ecc6794a4fc94d0dd3866d` |
| `src/main/java/dev/urlshort/click/Click.java` | PASS — integrated source/configuration read in full and checked against contracts | `ebb975c6831a4141863f1fafc54dd953125bf74a` |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS with LOW carry-overs — fail-open and shutdown ownership hold; proxy hash and reduction-reason wording recorded | `4078b1c9bc418f2cd420472e5b6220b3e261ae8b` |
| `src/main/java/dev/urlshort/click/ClickStore.java` | PASS — integrated source/configuration read in full and checked against contracts | `8b3e3d973013a8336c723e4f12ecd3b1f2effc40` |
| `src/main/java/dev/urlshort/click/DailySalt.java` | PASS — integrated source/configuration read in full and checked against contracts | `bd3f23efcfddcb8115853e989acbd140338dc116` |
| `src/main/java/dev/urlshort/click/LinkStats.java` | PASS — integrated source/configuration read in full and checked against contracts | `f77f4197db822ef0d4e06cd11a5fcfdffff5281e` |
| `src/main/java/dev/urlshort/click/StatsController.java` | PASS — integrated source/configuration read in full and checked against contracts | `58bb2a483e1dda2fcd034c40827c84a74a7fdceb` |
| `src/main/java/dev/urlshort/click/package-info.java` | PASS — integrated source/configuration read in full and checked against contracts | `0ee079737ef4a53a8035999f9c165d8482fc4c54` |
| `src/main/java/dev/urlshort/link/CreateLinkRequest.java` | PASS — integrated source/configuration read in full and checked against contracts | `b46f7cbc394268b0ffa904f0ea74fa0f19f29432` |
| `src/main/java/dev/urlshort/link/Link.java` | PASS — integrated source/configuration read in full and checked against contracts | `066e8d07d9c2f7852f8855553bf31db05a317aa3` |
| `src/main/java/dev/urlshort/link/LinkConfig.java` | PASS — integrated source/configuration read in full and checked against contracts | `44773c3456145354a85533590ee738606cb02ee9` |
| `src/main/java/dev/urlshort/link/LinkController.java` | PASS — integrated source/configuration read in full and checked against contracts | `3b5978f091895f6ed62b41b28a25b73c00734748` |
| `src/main/java/dev/urlshort/link/LinkProperties.java` | PASS — integrated source/configuration read in full and checked against contracts | `e6a2049aaf1d7875e5208153c976e4b1a777fe55` |
| `src/main/java/dev/urlshort/link/LinkRepository.java` | PASS — integrated source/configuration read in full and checked against contracts | `43106c469e04620650b52d38af0e072a327b1734` |
| `src/main/java/dev/urlshort/link/LinkResponse.java` | PASS — integrated source/configuration read in full and checked against contracts | `9c822acc63ab7a59e155bb1ae49450cb5b3b85d3` |
| `src/main/java/dev/urlshort/link/LinkService.java` | PASS — integrated source/configuration read in full and checked against contracts | `f339105992f09c6b7fbdad6666b7a40bc6080e84` |
| `src/main/java/dev/urlshort/link/LinkSnapshot.java` | PASS — integrated source/configuration read in full and checked against contracts | `4ef86e5f1b0494c56db46b30038f3cf133955ad2` |
| `src/main/java/dev/urlshort/link/LinkValidation.java` | PASS — integrated source/configuration read in full and checked against contracts | `79e0b395105a2f08d87e27dccd3a3c46c7d2cceb` |
| `src/main/java/dev/urlshort/link/RedirectController.java` | PASS — integrated source/configuration read in full and checked against contracts | `b0b8804b0bd32efdcbdc09118a794a086e905afc` |
| `src/main/java/dev/urlshort/link/ShortCodes.java` | PASS — integrated source/configuration read in full and checked against contracts | `2524f23de5c5e3ee68a17b0ee9460eb202ef2ce9` |
| `src/main/java/dev/urlshort/link/package-info.java` | PASS — integrated source/configuration read in full and checked against contracts | `7e46a48a9044450471712d916a987b3544009c79` |
| `src/main/java/dev/urlshort/ping/PingController.java` | PASS — integrated source/configuration read in full and checked against contracts | `f72fb06a37f46397fe08943a325eb45b150b3bc7` |
| `src/main/java/dev/urlshort/ping/PingResponse.java` | PASS — integrated source/configuration read in full and checked against contracts | `8b24612cb2202c598747a8fe591458725ce48404` |
| `src/main/java/dev/urlshort/web/OpenApiConfig.java` | PASS — integrated source/configuration read in full and checked against contracts | `9b996baa3f5949124f72444a2d5ab2069482e5b0` |
| `src/main/java/dev/urlshort/web/ProblemDetailsAdvice.java` | PASS — integrated source/configuration read in full and checked against contracts | `1b9c51aeb86076c67978004a68b077e061ad7c29` |
| `src/main/java/dev/urlshort/web/Problems.java` | PASS — integrated source/configuration read in full and checked against contracts | `c56a5b9ea7be14b849897fef2615553676b69821` |
| `src/main/java/dev/urlshort/web/RateLimitConfig.java` | PASS — integrated source/configuration read in full and checked against contracts | `a6e5e72e6c72dc9c1e6e79c78d5d2a693afdbd5a` |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java` | PASS — integrated source/configuration read in full and checked against contracts | `a0f74da4a7fae0d7c964338a566316269cfe5e3e` |
| `src/main/java/dev/urlshort/web/RateLimitProperties.java` | PASS — integrated source/configuration read in full and checked against contracts | `1015f3765b69f8fe565e746be63fce6bb87d9e50` |
| `src/main/java/dev/urlshort/web/RateLimiter.java` | PASS — integrated source/configuration read in full and checked against contracts | `8864ab40408d82506127f8a035b629173cd51fea` |
| `src/main/java/dev/urlshort/web/RequestBodyLimitFilter.java` | PASS — integrated source/configuration read in full and checked against contracts | `d83015a959badeaaf4e38b3808f714d084112156` |
| `src/main/java/dev/urlshort/web/RequestIdFilter.java` | PASS — integrated source/configuration read in full and checked against contracts | `130590ef03748d9af8b8b060dd01c592f146c8e0` |
| `src/main/java/dev/urlshort/web/package-info.java` | PASS — integrated source/configuration read in full and checked against contracts | `8597160af1f31b9ef9ee01bfc92e85d0a05205a6` |
| `src/main/resources/application.properties` | PASS — integrated source/configuration read in full and checked against contracts | `9c073ff53e62aed905a659f2b1f148f2bfd123d6` |
| `src/main/resources/db/migration/V1__create_link_and_audit_log.sql` | PASS — integrated source/configuration read in full and checked against contracts | `a73348c02de0ecd1f203a83e521e214849eef61c` |
| `src/main/resources/db/migration/V2__create_click.sql` | PASS — integrated source/configuration read in full and checked against contracts | `32897cb775c9be1a7cfee0d47b71f8f17d4b9bb5` |
| `src/test/java/dev/urlshort/audit/AuditLogTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `539058b812f4697d619ea31f2dc30a9411c330a3` |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `440de31db1dd691cf74a7a03f7834cf9e50c0e83` |
| `src/test/java/dev/urlshort/click/ClickSchemaTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `5d68a6016e371fdc94e3c120ef545a1a266cc94d` |
| `src/test/java/dev/urlshort/click/ClickTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `2a18269cfb5eaeb7c48f7cd0975e8046ad7ba7b4` |
| `src/test/java/dev/urlshort/click/DailySaltTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `9d31033071946e22ae9e90d0fad6bb08fa2cf91f` |
| `src/test/java/dev/urlshort/click/LinkStatsTest.java` | PASS — prior full source review at 5b3490c retained by exact blob identity; fresh gate and traceability audit | `fcdcb59bf746f4d9ed0e77d6217bb6dc6a2f345d` |
| `src/test/java/dev/urlshort/link/LinkServiceTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `de06330d3ce747e93f7489840872484bf5fe87af` |
| `src/test/java/dev/urlshort/link/LinkTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `aa46da04cc90f1d5367578f65d0d095cc81373f8` |
| `src/test/java/dev/urlshort/link/LinkValidationTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `4171784eb53f28742207329833f3f0c6d32338c9` |
| `src/test/java/dev/urlshort/link/ShortCodesTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `a0d87f57654d624b9e32146346c51f7a26c0a767` |
| `src/test/java/dev/urlshort/web/ProblemDetailsAdviceTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `011e9c7f7b179142688cdd30a7946467171caad9` |
| `src/test/java/dev/urlshort/web/ProblemsTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `157396176b387a283e1d2a86e1b6e72c87bc9c37` |
| `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `aee613b851928f7405d7a987622db98733c98272` |
| `src/test/java/dev/urlshort/web/RateLimiterTest.java` | PASS — integrated delta read against the prior source review, or new file read in full; fresh gate and traceability audit | `b06c4246ce9ba8e516308677eb72ac067ddccab7` |
| `src/test/java/dev/urlshort/web/RequestBodyLimitFilterTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `645b57bc4cd8f5caa9a1ed9e086d78ceab548707` |
| `src/test/java/dev/urlshort/web/RequestIdFilterTest.java` | PASS — prior full source review at a922f49 retained by exact blob identity; fresh gate and traceability audit | `6a13355d7666a74e0666318266e4910791f9d629` |
