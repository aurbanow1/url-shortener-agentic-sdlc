---
slice: OPR.99.0.3.5
candidate_sha: add7ab5ca37dcd6f51aef3cd43c85455e1be6d14
artifact_type: guard
verdict: PASS
money_evidence: "docker build --pull of add7ab5 exits 0: both eclipse-temurin
  base images pulled, 15/15 steps, in-container bootJar BUILD SUCCESSFUL"
evidences:
  - "2"
self_check: "I read the capture: both Pulling/Digest/Status lines are present,
  BUILD SUCCESSFUL in 55s inside step 7, Successfully built and tagged, and the
  harness reported exit code 0 for an unpiped docker run."
---

Builder drop for AC-5/AC-12 (proof-contract item 2): docker build --pull --tag urlshort:cd on the clean candidate worktree at add7ab5, layer cache allowed as qa2-agent requested (qitem-20261003200244-22f2ea52). Exit code 0. Both base images were pulled and confirmed current (21-jdk 3e3c176f, 21-jre cff19e62); steps 5-7 ran, including the Gradle download and an in-container bootJar; Successfully built 8f8a7521104b. The full command, SHA, output and exit code are in the attached capture.

## Media

![docker-build-pull-add7ab5.txt](docker-build-pull-add7ab5.txt)
