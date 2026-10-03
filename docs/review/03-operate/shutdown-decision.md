# 03-operate: shutdown contract decision (DR-02)

Candidate `d7fa6029168f74ae8b67efa6607dcf7a08296931` uses native Tomcat graceful
shutdown. Review reproduced four resets per high-load stop after successful TCP
connection establishment; an already-dispatched held request completed, and all
ten paced stops had zero resets. SPEC `b53372f` AC-25/rule 13 currently count
every post-connection reset as failure, including the listen-backlog race.

Decision for the lead to route under the human's retained ambiguity authority:
keep that strict connection-level promise, or explicitly promise complete drain
of application-dispatched requests within 10 seconds while separately counting
the documented boundary connection losses? Recommended: the latter, preserving
the held-request completion check and failing any loss of that established
application request. It matches native service behavior without a new networking
layer. This is a proposed contract change, not an approval or a silent exception.

Evidence and exact commands: `docs/review/03-operate/design-review.md` DR-02 and
`docs/review/03-operate/proof/design-probe-rerun.txt` D1/D2/P1–P10. If approved,
the requirements owner updates SPEC and the designer aligns design/ADR/proof;
the review then checks those changes. Otherwise design must meet the existing
predicate. DR-01 logging mitigation can proceed independently.
