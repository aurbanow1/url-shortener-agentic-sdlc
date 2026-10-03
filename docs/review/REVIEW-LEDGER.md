# Review ledger

One row per review pass, appended by the Review Agent. "Files reviewed" must
equal "files changed" for a pass to count as complete.

| Date (UTC) | Slice | Review | Candidate SHA | Files changed | Files reviewed | MUST-FIX | HIGH | MEDIUM | LOW | Verdict | Reviewer |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-10-02 | 01-ping | design_review | 3931c8b51e4b3e486cae7218e43c206bd9188ba8 | 9 | 9 | 0 | 1 | 0 | 0 | FAIL — DR-01: functional config shadows shipped logging/problem-detail settings | review-agent@urlshort-factory (Codex) |
| 2026-10-02 | 01-ping | design_review (re-review) | d0521deaa453d9e425b3ca9d06f2d10b65a16255 | 8 | 8 | 0 | 0 | 0 | 0 | PASS — DR-01 fixed by verified functional profile overlay | review-agent@urlshort-factory (Codex) |
| 2026-10-03 | 01-ping | code_review | f286a10863e4a8081235226f2d56e51ac121b319 | 9 | 9 | 0 | 0 | 0 | 0 | PASS — fresh full gate and QA evidence audit; Ponytail: lean already | review-agent@urlshort-factory (Codex) |
| 2026-10-03 | 01-ping | security_review | f286a10863e4a8081235226f2d56e51ac121b319 | 9 | 9 | 0 | 0 | 0 | 0 | PASS — live exposure/privacy/error checks; offline dependency inventory, network advisory check at release | review-agent@urlshort-factory (Codex) |
