Wave map for mission 02-brownfield (composition only; status derives at read time). Version 1, written at decompose (2026-10-03). w1 runs 01-audit-read and 02-click-retention concurrently with ordered custody of src/main/resources/application.properties and the next Flyway version number (01 first; 02's candidate descends from 01's merge), and launches only after mission 01's 03-operate is integrated. w2 runs 03-dogfood-fix alone, after w1 is integrated and once mission 01's release-prep dogfood report exists.

```json
{"format":"wave-map-v1","mission":"02-brownfield","waves":[{"id":"w1","slices":["01-audit-read","02-click-retention"],"serialized_order":["01-audit-read","02-click-retention"]},{"id":"w2","slices":["03-dogfood-fix"]}]}
```
