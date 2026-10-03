# Mission 01 wave review — forward fixes and retained limitations

Owner: orchestration-lead@urlshort-factory. Review packet `qitem-20261003135957-0f6e0c8b`, product range `7636264..8e9c065`.

The two-vantage wave verdict is PASS with no blocking finding. Full findings and fresh evidence are in `docs/review/01-greenfield-core/wave-2-review-review-agent.md`; structure is in `wave-2-review-design-agent.md` at `dad0a5a`. Please own these as forward-fix slices or explicitly parked backlog with the stated triggers; this item does not block release preparation.

1. **W2-01 / CR-01, MEDIUM, JUDGMENT-GAP:** correct the OpenAPI ProblemDetail component to describe top-level errors and remove the fictitious nested properties member. Existing web customizer + schema assertions + regenerated JSON is enough. Scope: web/OpenApiConfig, OpenApiDocumentTest, docs/api/openapi.json; next compatible shared-file grant. Both vantages and the fresh HTTP capture reproduce it.
2. **W2-02 / A-9, LOW, CONTEXT-GAP:** before configured-proxy use or any unique-client/hash consumer, align click identity with the explicit trusted-proxy rule. Own jar shows independent client budgets but one hash for both clients. Current raw totals remain correct; historical proxy hashes cannot be separated retroactively. Keep the limitation visible at release and pass its trigger into mission 03.
3. **W2-03, LOW, JUDGMENT-GAP:** disk-meter path on the anonymous loopback operator scrape. Deny that meter through a supported property when config is next touched, or retain the disclosed limitation.
4. **W2-04, LOW, JUDGMENT-GAP:** smoke R0 modes need Perl/Time::HiRes and a supported locale (C tested). Release must verify/document prerequisites; schedule any portability repair without weakening completion/deadline checks.
5. **W2-05, LOW, CONTEXT-GAP:** click reduction failures use reason rejected, which design documentation defines too narrowly as queue rejection. Widen that definition or use a distinct static reason in passing.

**Already resolved:** W2D-02 (MEDIUM design-document drift) fixed by design-agent at `dad0a5a`; independently checked. Preserve settled limiter/R0 repairs and lead shutdown/clock decisions.

Release obligations already listed in `docs/evidence/01-greenfield-core/wave-integration.md` remain release-owned (container evidence, actual 100/20 rate, p95/L3, scans and final proof judgments); they are not new wave findings. No product edits or external publication were performed by this review.
