# QA observations — 03-operate / a7c533f

No failing in-suite AC, red build or coverage shortfall. These observations do
not fail the assigned QA boundary; the locked SPEC assigns the first judgment
to release and does not prohibit the second value on this surface.

## QA-OPR-01 — MEDIUM — release bench input rate not established

- AC-27; NFR-L1/L2; proof items 12–13.
- Reproduce: start the candidate jar on loopback with both rate-limit budgets
  at 1,000,000/min, then run candidate `scripts/smoke.sh --bench <base-url>`.
  Exact independent output: slice `proof/qa-bench-a7c533f.txt`.
- Expected release workload: 100 redirect requests/s for 60 s and 20 creates/s.
- Observed: 4,948 redirects (82.5/s), 990 creates (16.5/s), zero bad responses;
  redirect p95 2.3 ms / p99 4.1 ms, create p95 2.4 ms. The script sleeps 100 ms
  after every request in ten redirect/two create loops, adding process/request
  time to every cycle. The numbers prove the mode executes and reports its
  limits; they do not prove the latency targets at the specified workload.
- Release continuation: establish the specified offered rate and judge the
  latency targets, or record the shortfall as an unresolved release gap.

## QA-OPR-02 — LOW — anonymous Prometheus exposes working-directory path

- Business rule 10 / installation metadata observation; AC-19 still passes.
- Reproduce: `scripts/http -sS <base-url>/actuator/prometheus`; inspect the
  `disk_free_bytes` and `disk_total_bytes` families in
  `proof/qa-prometheus-a7c533f.txt`.
- Observed: their `path` tag identifies the service working directory. Health
  bodies disclose no installation details; the scrape contains none of
  AC-19's prohibited client, code, URL or canary values.
- Review continuation: retain or remove these default gauges deliberately.
