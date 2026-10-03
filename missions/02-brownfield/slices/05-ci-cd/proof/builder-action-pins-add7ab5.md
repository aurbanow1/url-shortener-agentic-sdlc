---
slice: OPR.99.0.3.5
candidate_sha: add7ab5ca37dcd6f51aef3cd43c85455e1be6d14
artifact_type: guard
verdict: PASS
money_evidence: "git ls-remote --tags at 19:34Z: all four pinned actions resolve
  to the SHAs in ci.yml and cd.yml; no tag moved"
evidences:
  - "3"
self_check: "I compared each ls-remote line with each uses: line in the capture:
  3d3c42e5, de7274f0, 3f5f9ada (peeled from b9bee63e) and 043fb46d appear in
  both, nine uses: lines in all."
---

Builder drop for AC-9 (proof-contract item 3): git ls-remote --tags for actions/checkout v7.0.1, actions/setup-java v6.0.1, gradle/actions v6.4.0 (annotated; the peeled ^{} line is the pin) and actions/upload-artifact v7.0.1, taken on this networked seat at 2026-10-03T19:34Z, beside every uses: line of the candidate's two workflows. Every SHA matches its tag; none moved since the design's capture.

## Media

![action-pins-ls-remote.txt](action-pins-ls-remote.txt)
