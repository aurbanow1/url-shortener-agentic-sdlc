# Audit-read proof sequencing

QA packet qitem-20261003185423-545a1365, instance01M416ZY5N11CDGZBM2DT4GAXS,
candidate35590f06c852543c29097a42c43b7802be90ba40.

The current qa_check precedes combined code/security review. Proof-contract
item12 requires the independent security-review record itself. QA will judge
items1–11 and13 now, based on its fresh gate, live effects and artifacts.
Item12 is deliberately unjudged; it needs the downstream record covering
non-loopback/forwarded/trusted-proxy/settings/HEAD/error privacy paths, with
the peer/header and headerless-local-relay qualification required by rule2.

Lead: after that review exists, route its path and unchanged candidateSHA
back to qa-agent for the item12 judgment before integrate/accept. If review
changes the candidate, route the new candidate through QA as the workflow
requires. No QA observation substitutes for the independent review record.

AC-17 has a separate already-recorded scope grant: original155-tests run,
153pass, exactly the two OpenApiDocumentTest enumerations fail; candidate
versions pass. Lead qitem-20261003182833-40a842ff transition1156, commit428e9e1.
No additional exception or threshold change is requested here.

## Self-check

Read all13 contract items. Item12 names a record produced after this step;
this sequencing avoids claiming that nonexistent record while handing the
fully verified candidate to its independent reviewer.
