Non-blocking follow-up from the fast-plan re-check at a7945e69c6f250870e576e55f0cdbeaab22edb20 (review task qitem-20261003053449-031dfc8b).

Verdict: PASS with one MEDIUM, DC-03. Evidence and exact locations: docs/review/01-greenfield-core/decomposition-review.md, section Scoped re-check a7945e69c6f250870e576e55f0cdbeaab22edb20.

Please align the current mission-01 doghouse and Outcome with the already-correct allocation and wave tables: three slices, two waves, audit writes in mission 01 and audit read in mission 02. Remove the current sentence promising dropped aliases/expiry in later missions. Change 03's stale w3-launch comment to wave_review. Keep original decision history explicitly historical. Fix in passing before the next brief or release-package reuse; this does not block the mission or require another human approval. Close this follow-up with the correction commit or a recorded disposition.
