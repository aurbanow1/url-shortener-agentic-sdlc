# QA observations — 03-operate

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

## Re-check 1c8b2cf — 2026-10-03 UTC

No failing AC-1–AC-20, red build or merged coverage gap. Independent reviewer
limiter probe and stricter R0 controls verify the CR-01/SEC-01, CR-02 and
CR-03 repairs. Review2 still owns their review judgment.

- QA-OPR-01 remains MEDIUM and release-owned: the new 60-second bench has
  4,925 redirects (82.1/s), 987 creates (16.4/s), zero bad responses;
  redirect p95 2.3 ms / p99 3.8 ms and create p95 2.6 ms. These rates still
  fall below 100/20. Reproduction is unchanged; output is
  `proof/qa-bench-1c8b2cf.txt`. The numeric release verdict remains pending.
- QA-OPR-02 remains LOW: the identical metrics configuration still exposes
  the working-directory `path` in disk gauges; `proof/qa-prometheus-1c8b2cf.txt`
  contains it. All prohibited client/code/URL canaries remain absent.

## QA-OPR-03 — LOW — R0 host locale requirement

- AC-25/28 release instrument; this does not fail an in-suite product AC.
- Reproduce on this macOS host with the Codex session's inherited
  `LC_ALL=LC_CTYPE=LANG=C.UTF-8`: run
  `perl -MTime::HiRes=time -e 'printf "%d\n", time * 1000'`.
- Expected: a millisecond timestamp. Observed: exit 9, unsupported-locale
  warning, then `panic: locale.c: 4486: Could not change LC_CTYPE locale to
  C.UTF-8, errno=9`. The jar journey and first R0 controls stopped at this
  host dependency. Their outputs are retained as `*-locale-failed.*`.
- With the supported `C` locale the same Perl command exits 0; the unchanged
  candidate R0 functions classify all eight response cases correctly, and
  the unchanged jar drain succeeds. The QA control requires the harness to
  complete and emit the R0 verdict, so setup failure cannot prove a negative
  response case. No candidate/toolchain change was made.
- Release continuation: verify Perl/Time::HiRes and a supported locale on
  its actual host; record the environment. Linux and container restart
  execution remain unverified here. This is a recorded host limitation,
  without a release-target waiver.
