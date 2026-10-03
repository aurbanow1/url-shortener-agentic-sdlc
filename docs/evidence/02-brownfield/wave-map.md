Wave map for mission 02-brownfield (composition only; status derives at read time). Version 3, 2026-10-03T18:33Z, supersedes version 2 (18:00Z) and version 1 (decompose).

- Version 3: a fifth slice, `05-ci-cd`, added under the human's decision D14 (relayed by the operator on qitem-20261003182906-74ccb677: CI/CD in GitHub Actions on every repository, docs/guidance/ci-cd.md). It goes in w2, as the operator asked ("the next wave so it does not disturb wave 1"). Its territory is `.github/` only.
- Version 2: `04-audit-columns` added under the human's audit-column decision (qitem-20261003175330-fb054f2f).
- w1, unchanged: `01-audit-read` and `02-click-retention` concurrently, with ordered custody of `src/main/resources/application.properties` and `README.md`, 01 first. `01-audit-read` takes no Flyway number; `02-click-retention` takes V3.
- w2: `03-dogfood-fix` (W2-01, `web/` and `docs/api/openapi.json`), `04-audit-columns` (`link/`, `audit/`, the next migration after V3) and `05-ci-cd` (`.github/`). Their territories are pairwise disjoint, so they run concurrently once both w1 slices are integrated.

```json
{"format":"wave-map-v1","mission":"02-brownfield","version":3,"waves":[{"id":"w1","slices":["01-audit-read","02-click-retention"],"serialized_order":["01-audit-read","02-click-retention"]},{"id":"w2","slices":["03-dogfood-fix","04-audit-columns","05-ci-cd"]}]}
```
