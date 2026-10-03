Wave map for mission 01-greenfield-core (composition only; status derives at read time). Version 3: supersedes qitem-20261003041633-b5881594 after the human's fast-plan decision of 2026-10-03 (operator packet qitem-20261003052736-7830d02a): 04-audit-read moved to mission 02, so wave w3 disappears and w2 is the last wave. The w2 merge order from decomposition review DC-01/DC-02 stands (02 merges before 03; 03's candidate descends from 02's merge commit with docs/api/openapi.json regenerated on that base).

```json
{"format":"wave-map-v1","mission":"01-greenfield-core","waves":[{"id":"w1","slices":["01-create-redirect"]},{"id":"w2","slices":["02-analytics","03-operate"],"serialized_order":["02-analytics","03-operate"]}]}
```
