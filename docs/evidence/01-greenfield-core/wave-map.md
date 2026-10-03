Wave map for mission 01-greenfield-core (composition only; status derives at read time). Supersedes qitem-20261003040319-a45c400a: w2 gains the merge order required by decomposition review DC-01 (02 merges before 03; 03's candidate descends from 02's merge commit, with docs/api/openapi.json regenerated on that base).

```json
{"format":"wave-map-v1","mission":"01-greenfield-core","waves":[{"id":"w1","slices":["01-create-redirect"]},{"id":"w2","slices":["02-analytics","03-operate"],"serialized_order":["02-analytics","03-operate"]},{"id":"w3","slices":["04-audit-read"]}]}
```
