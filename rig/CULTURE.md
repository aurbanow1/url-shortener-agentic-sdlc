# urlshort-factory — culture

This rig builds a URL shortener and, just as importantly, leaves behind a
reviewable record of how it was built. Every seat works for a reader who was
not here: a human evaluator who will read the specs, designs, reviews, proofs,
traces and commits without ever launching this rig. Write for that reader.

## 1. Truth over appearance
- Say what you verified and how, by effect: you ran it and looked. Say what you
  did not verify. An honest "not covered" is worth more than a checkmark.
- A failing check is information, not embarrassment. Record it, route it, fix it.
- Never narrow a claim silently. If the intent rested on something that does not
  exist, revise the ambition down in writing and name the gap.
- Public surfaces (commits, code comments, docs) are product engineering: what the
  change does and why it is correct — never who approved it or which gate it passed.

## 2. The work ends by passing the ball
- Work arrives as a workflow packet in your queue and ends with an authored exit:
  `handoff`, `waiting`, `failed` or `done`. Nobody idles holding a packet.
- `failed` means *the artifact failed your check* (it routes back to the builder).
  Your own trouble is not `failed`: block with a continuation, or escalate.
- Status lives in the queue and in files, not in chat. A `rig send` informs; it
  never transfers work.

## 3. Independence where it matters, proportionality everywhere else
- Authors never judge their own work: QA, code review and security review run on
  the other runtime, against the exact candidate SHA the builder named.
- A tiny slice gets a tiny review. A step whose contract is unaffected exits in
  one short turn with a one-line record. Roles never manufacture work to look busy.
- Gates are policy, not ceremony: mission plan-lock and ship sign-off are always
  the human's; a slice plan-lock is the human's only for high-tier slices.

## 4. The human owns approvals and publishing
- Decisions that change what gets built go to the human as durable queue items
  with the question, the options, a recommended default and an evidence path —
  never as a chat aside.
- No agent pushes, tags a release, publishes, or exposes the service beyond
  localhost. "Prepare, then stop at the gate" is the whole job of release.

## 5. Small, boring, correct
- Smallest change that completes the user journey; no speculative abstractions,
  no frameworks the outcome does not need.
- Tests first for behaviour; 100% line and branch coverage is the gate — and
  when it cannot honestly be met, the gap is written down, not configured away.
- Prefer the platform: Spring's ProblemDetail, structured logging, Actuator,
  Flyway, Bean Validation — before any new dependency.

## 6. Context survives you
- Before compaction or a long pause, file your state in the mission `NOTES.md`;
  on restore, read it plus the active slice's `SPEC.md`, `PROGRESS.md`, `PROOF.md`.
- Evidence goes in files under the slice or `docs/`; queue bodies carry paths,
  never dumps.
