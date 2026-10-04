# Architecture consistency verdict — 06-client-identity (register row 1, D20)

- Requested by `design2-agent`, the slice's designer: `qitem-20261004010101-68e1848a`.
- Input: `missions/02-brownfield/slices/06-client-identity/design.draft.md` (`84ba733`), against
  `docs/guidance/architecture.md` §11 at `41eff65`.
- Author: `design-agent@urlshort-factory`, the register's owner, 2026-10-04.
- Disclosure: the two code paths this design moves come from my designs. The audit guard is from
  `01-audit-read`, the `CLIENT_ATTRIBUTE` hand-off from `01-analytics-v2`. This is a check against
  the register, not a design review; `design_review` is independent of me.

**Verdict: CONSISTENT, no condition.**

The design does what row 1's principle asks: one code path for the concern. `web.ClientIdentity`
becomes the single home of both halves of the row. The resolved client (`clientOf`, `resolve`, `of`)
stays under ADR-0015, and the direct-peer audit guard (`peerIsConnection`, `fromLoopback`) stays
under ADR-0019. The two questions keep separate methods, and the guard never receives the
trusted-proxy list. So the row's central separation holds by construction: the guard reads the
connection's own peer, and trusting a proxy for rate limits cannot open the audit read. The row's
rules are kept:
- the request is not wrapped and `getRemoteAddr()` is never rewritten;
- the click recorder still takes the limiter's resolved client and falls back to the peer only when
  it is absent;
- `peerIsConnection` carries all three Boot 4.1.1 valve triggers verbatim, with the `ponytail:`
  ceiling;
- each decision amends the ADR it is settled in (ADR-0015, ADR-0019), with no rival ADR.

The new dependency direction matches the layering rule and the register: `click/` and `audit/`
depend on a static `web/` utility, as they already do on `web.Problems`, and `web/` names no
feature package. The register is not redefined: the setting stays `urlshort.rate-limit.trusted-proxies`
in `RateLimitProperties`, and no other row's code path moves.

**Row edits I make after the merge** (`slice.yaml` assigns the row to me), checked then against the
merged code:
- **Row 1, code path:** `web.ClientIdentity`. `resolve` is called by the limiter after the exempt
  check and before charging. `of` is called by the click recorder. `peerIsConnection` and
  `fromLoopback` are called by `AuditController`. `CLIENT_ATTRIBUTE` is
  `ClientIdentity.CLIENT_ATTRIBUTE`.
- **Row 1, rule:**
  - "Inside `web/`, reuse `ClientIdentity`; elsewhere, call `ClientIdentity.of(request)`."
  - The M3S-03 sentence becomes "must still call `ClientIdentity.resolve`" in place of "must still
    set `CLIENT_ATTRIBUTE`", with the same meaning.
- **Client hashing row:** "its input is `ClientIdentity.of(request)`".
- **Operator settings row:** M3S-02 (the `trusted-proxies` comment) is not this slice's to fix
  (D21: no settings change), so it stays as drift.

**Re-checked on the final design** (2026-10-04, after `design2-agent`'s 01:10Z note). Read: the
final `design.md` at `9f6508a`, and the committed ADR-0015 and ADR-0019 amendments in `57cb9ae`.
- §1, §2 and §6 carry the draft's substance: the same five members, the same call sites, the guard
  never given the trusted-proxy list or the resolved client, and amendments to the settling ADRs
  only.
- ADR-0019's amendment adds one fact: Boot 4.1.1's valve trigger and the guard both use
  `StringUtils.hasText`, so a whitespace-only `remoteip` setting installs no valve and leaves the
  read open. That is the merged guard's behaviour, and a consistent one.
- The other changes are §5's characterization matrix, which is test placement and not a register
  matter, and the post-merge M3S-03 wording, now in §6.

**The verdict stands: CONSISTENT, no condition.**
