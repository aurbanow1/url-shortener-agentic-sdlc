# First gate: external localhost listener

The first exact-candidate gate on `fb63a88a9b92c1fec97ba74686af1a2f30304160`
completed 268 unit and 322 functional invocations, with two functional failures.
Both are unchanged `RateLimitJourneyTest` real-server methods: expected 404
and 429, observed 401. All reports/execution data were preserved immediately
under `gate-attempt-01/`; the raw gate log is `check-fb63a88.txt`.

The failed class's XML records Tomcat starting on **50898**. Its JSON request
events contain no status401, even though the two Java localhost calls received
401. The candidate source/dependencies have no authentication/401 handler.
The same class, isolated and unmodified, passed all19 invocations; its separate
report/log are preserved under `target-rate-limit/` and
`target-rate-limit-fb63a88.txt`. That isolated pass alone did not establish a
green gate.

QA inspected the recorded port after the failed test server stopped:

```
lsof -nP -iTCP:50898 -sTCP:LISTEN
COMMAND   PID    USER     TYPE   NODE NAME
Python  74360 andrzej    IPv4   TCP 127.0.0.1:50898 (LISTEN)
```

Both `scripts/http -sS --max-time 2 -i http://localhost:50898/api/links`
and the explicit `http://127.0.0.1:50898/api/links` returned:

```
HTTP/1.1 401 UNAUTHORIZED
Server: Werkzeug/3.1.5 Python/3.13.11
Content-Type: text/html; charset=utf-8
Content-Length: 317
Server: Python
```

The response had an HTML Unauthorized page and no urlshort X-Request-Id.
The other application set a session cookie; its value is deliberately omitted
from this evidence. No authentication was attempted, and no owner process,
listener or configuration was changed or stopped.

This directly establishes an external IPv4 listener answering401 at the failed
class's port. Together with the absent application401 log and isolated pass,
it explains the initial failure as traffic reaching that listener. The port
was not inspected while the failed Tomcat instance was still alive, so no
simultaneous socket snapshot is claimed. A fresh full diagnostic gate is
required and its outcome is retained separately; the first red run remains
visible. The by-effect apps bind explicitly to127.0.0.1 on reserved free ports.

`ps -p74360 -o pid,lstart,comm` identified pgAdmin4's Python runtime, started
Thursday October1 at16:43:19 local time, before this QA run. The process
metadata read required approved outside-sandbox execution. An explicit IPv6
probe after Tomcat stopped refused its connection, while the IPv4 listener
continued to answer401. The initial unquoted IPv6 URL was rejected by zsh
globbing before any request; the quoted rerun supplied that IPv6 observation.

The diagnostic full gate on the **same unchanged SHA** then completed all
14 tasks successfully: 268 unit and322 functional tests, zero failures/errors/
skips, merged584/584 lines and206/206 branches. Raw log:
`check-fb63a88-diagnostic-02.txt`. This is a retained, diagnosed environmental
incident, not an unexplained retry used to hide a flaky product assertion.
