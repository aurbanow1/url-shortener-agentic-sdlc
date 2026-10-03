# Independent QA instruments and limits

The fresh quality gate used the original source/tests/build configuration at
the packet's exact candidate. The manual controlled run launched its compiled
classes and resources on real loopback Tomcat with the candidate jar's own
runtime libraries. `qa-control-a7c533f.java` adds three disposable test controls:

- A primary UTC clock that starts frozen and advances on an acknowledged stdin
  command, to observe exact refill boundaries and the 24-hour key expiry.
- A request wrapper that overrides `getRemoteAddr()` from an explicit QA-only
  header, like MockMvc's controlled peers. It precedes the product filters;
  the candidate filter and its actual header-selection algorithm run unchanged.
- A real Hikari/H2 datasource whose connection acquisition can deliberately
  throw a fixed SQLException, then recover. Readiness follows it through the
  actual health contributor and real HTTP. This is controlled unavailability,
  not a natural disk/database failure.

These controls exist only in the disposable launcher, never the jar or repo
product paths. Every captured HTTP request uses `scripts/http`. Captures name
the control where used; exact SPEC addresses also passed the fresh functional
suite. No actual alternate-TCP-peer claim is made.

The first attempt passed AC-1–AC-5, then curl failed binding 127.0.0.2 with
exit 45 / errno 49 on macOS. Its capture is retained with an `attempt1` suffix.
The second attempt stopped on a QA assertion that assumed the root health body
contained only `status`. The actual body additionally lists `groups`, which
the SPEC permits; `attempt2` captures show the observation. After checking the
contract, the final run allowed only `status` and those two known group names.
Neither stop was classified as a candidate defect or hidden by a test retry.

The final run observed 2,303 exchanges, 30 rejected exchanges (all correlated
once), and exported 308 links / 309 audit rows / 1,806 reduced click rows.
The first 61 creates produced exactly 60 additional audit rows. Final storage
contained no searched client/forwarded values or rejected URL/UA/id canaries.
The separate unmodified `java -jar` runs used no clock/peer/datasource controls:
environment overrides, smoke, 60-second bench and shutdown drain all executed.

Reproduction from the root needs JDK 21, an existing candidate jar, and its
runtime libraries extracted into a temporary `libs/` directory. The runner
reads `docs/qa/03-operate/runtime-path.json` containing `{"temporary":"<temp>"}`;
the original extraction used Python's `zipfile` to copy `BOOT-INF/lib/*.jar`
from `build/libs/urlshort.jar`, without a download. Then run:

```sh
python3 missions/01-greenfield-core/slices/03-operate/proof/qa-journey-a7c533f.py
```

The launcher and Python source are preserved to make the controlled mechanism
reviewable. This is QA evidence, not a new product test suite or runtime API.

Text display captures use LF newlines and trim trailing spaces; original response strings, including trailing status/HELP spaces, remain in the JSON exchange records. Large JSON arrays use one exchange per line; their parsed content is unchanged.
