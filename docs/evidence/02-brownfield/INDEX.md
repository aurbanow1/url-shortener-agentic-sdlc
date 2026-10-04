# Evidence export — 02-brownfield

Exported 2026-10-04T05:30:09Z by tools/evidence-export.sh from OpenRig 0.6.3 (8b5e9488).

| Artifact | Governance clause (docs/GOVERNANCE.md) |
|---|---|
| compiled-graph.json | Explicit dependency graph with entry/exit gates: the mission DAG as compiled from the authored sources at export time (if a running instance was bound to an earlier version, the lead keeps that version here and the disk compile beside it as compiled-graph.authored-*.json; see Dynamic re-planning) |
| instances/*.trace.json | Cross-stage context and decision lineage; Bounded retries: the append-only step trail, one entry per closed packet with closureReason handoff / waiting / failed / done, actor and evidence_ref; failed → implement hops are the retries |
| instances/*.show.json | Dynamic re-planning; Fallback: bound sources and digests, revisionHistory (rig workflow revise receipts), the reconciliation block comparing the bound graph with the authored one, exception routing, resumeCount |
| packets/*.transitions.json | Human approval checkpoints; Safe-stop; Audit-grade observability: every state change of every packet with actor and timestamp, including the engine's gate park on human@kernel and the human's decision text on rig queue resolve |
| packets/*.show.json | Human approval checkpoints: the packet as last seen, with summary, evidence_ref, tier, tags (step, gate) and chain of record |
| proof-readiness.json | Controlled agent autonomy; Audit-grade observability: attributed proof judgments per slice (rig proof judge receipts, judge seat, subject commit, evidence hashes) and readiness |
| scope-audit.json | Policy guardrails: convention audit of mission and slice files (advisory) |
| workflow-status.json / workflow-list.json | Sequential and parallel paths: instance states and attention classes at export time (overlapping instance timestamps show pipeline parallelism) |
| queue-active.json | Safe-stop: live queue rows at export time (what was still held or parked) |
| usage-top.json | Reliability metrics: per-seat token burn over the window (24 h) |
| ../../metrics/metrics.json, ../../metrics/README.md | Reliability metrics: success rate, retries, rollbacks, MTTR, latency, human wait derived from these files by tools/sdlc-metrics.mjs |

Approval stamps are not in this export: they live in the stamped files' frontmatter (missions/<m>/SPEC.md, slices/*/SPEC.md: approved-spec-*, approved-*) with append-only audit rows daemon-side; the decision text behind each stamp is in the gate packet's transitions here.

Instances exported: 17; packets exported: 204.

## Packets (workflow · step · state · owner)

| Packet | Workflow | Step | State | Owner |
|---|---|---|---|---|
| qitem-20261002212902-0f6e128b | 00-hello | decompose | handed-off | orchestration-lead |
| qitem-20261002213817-6d848ac6 | 00-hello | mission_plan_lock | handed-off | orchestration-lead |
| qitem-20261002220222-65f568f1 | 00-hello | wave_integration | handed-off | orchestration-lead |
| qitem-20261002220259-281a4efc | urlshort-slice | requirements | handed-off | requirements-agent |
| qitem-20261002221159-a35e50bc | urlshort-slice | design | handed-off | design-agent |
| qitem-20261002225126-b5c94d4f | urlshort-slice | design_review | done | review-agent |
| qitem-20261002225911-1ad25865 | urlshort-slice | design | handed-off | design-agent |
| qitem-20261002230841-dc5cdf2b | urlshort-slice | design_review | handed-off | review-agent |
| qitem-20261002231236-cdda3066 | urlshort-slice | plan_lock | handed-off | design-agent |
| qitem-20261002232943-d0903176 | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261002235024-355f67c0 | urlshort-slice | qa_check | done | qa-agent |
| qitem-20261003003003-9cb0ed55 | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261003004117-c920fe49 | urlshort-slice | qa_check | handed-off | qa-agent |
| qitem-20261003005806-5aaa453e | urlshort-slice | code_review | handed-off | review-agent |
| qitem-20261003010318-807bbf8a | urlshort-slice | security_review | handed-off | review-agent |
| qitem-20261003013220-670f8532 | urlshort-slice | integrate | handed-off | orchestration-lead |
| qitem-20261003013423-99b71ff1 | urlshort-slice | slice_accept | done | qa-agent |
| qitem-20261003015049-51d1ba00 | 00-hello | wave_review | handed-off | review-agent |
| qitem-20261003020412-57cea2d9 | 00-hello | release_prep | handed-off | release-agent |
| qitem-20261003023502-f807af1f | 00-hello | ship_signoff | handed-off | release-agent |
| qitem-20261003024639-d36d76cc | 00-hello | evidence_export | handed-off | release-agent |
| qitem-20261003030415-a2011998 | 00-hello | mission_close | done | orchestration-lead |
| qitem-20261003033825-0148d3da | 01-greenfield-core | decompose | handed-off | orchestration-lead |
| qitem-20261003040709-b481dc8d | 01-greenfield-core | decomposition_review | handed-off | review-agent |
| qitem-20261003042553-03ac8b4b | 01-greenfield-core | mission_plan_lock | handed-off | orchestration-lead |
| qitem-20261003044200-384e9544 | 01-greenfield-core | wave_integration | handed-off | orchestration-lead |
| qitem-20261003044232-a7194f1d | urlshort-slice | requirements | handed-off | requirements-agent |
| qitem-20261003050304-b7cdaab5 | urlshort-slice | requirements_review | done | review-agent |
| qitem-20261003050836-02bc7593 | urlshort-slice | requirements | handed-off | requirements-agent |
| qitem-20261003051110-97f19fd2 | urlshort-slice | requirements_review | handed-off | review-agent |
| qitem-20261003051242-83fa8c2c | urlshort-slice | design | handed-off | design-agent |
| qitem-20261003051852-2c3bd470 | - | - | done | orchestration-lead |
| qitem-20261003052736-7830d02a | - | - | done | orchestration-lead |
| qitem-20261003054407-8b9603ab | - | - | done | orchestration-lead |
| qitem-20261003055347-01d2d207 | urlshort-slice | design_review | done | review-agent |
| qitem-20261003060343-3eb84b99 | urlshort-slice | design | handed-off | design-agent |
| qitem-20261003062552-16fc9d48 | urlshort-slice | design_review | handed-off | review-agent |
| qitem-20261003063348-182b23df | urlshort-slice | plan_lock | handed-off | orchestration-lead |
| qitem-20261003063930-0cb9c7eb | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261003071605-40d1d3aa | urlshort-slice | qa_check | handed-off | qa-agent |
| qitem-20261003074021-e3821d80 | urlshort-slice | code_review | handed-off | review-agent |
| qitem-20261003075859-7a5a6e93 | urlshort-slice | integrate | done | orchestration-lead |
| qitem-20261003080033-20dc594e | - | - | done | qa-agent |
| qitem-20261003080426-686ea065 | urlshort-slice-delegated | requirements | handed-off | requirements-agent |
| qitem-20261003080438-4f449e0f | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261003081312-64ea3c87 | urlshort-slice-delegated-b | requirements_review | done | review2-agent |
| qitem-20261003081331-87faf358 | urlshort-slice-delegated | requirements_review | handed-off | review-agent |
| qitem-20261003081851-f8f25b02 | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261003082131-b709b118 | urlshort-slice-delegated-b | requirements_review | handed-off | review2-agent |
| qitem-20261003082147-22c46f0c | urlshort-slice-delegated | design | handed-off | design-agent |
| qitem-20261003082416-b1057e7a | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003082940-4da408ce | - | - | done | orchestration-lead |
| qitem-20261003084518-9957256a | urlshort-slice-delegated | design_review | done | review-agent |
| qitem-20261003084948-76a4a31d | - | - | done | orchestration-lead |
| qitem-20261003090152-625de326 | urlshort-slice-delegated | design | handed-off | design-agent |
| qitem-20261003091111-32efdbe2 | urlshort-slice-delegated-b | design_review | done | review2-agent |
| qitem-20261003092226-699086e9 | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003092345-52dd8a6e | urlshort-slice-delegated | design_review | handed-off | review-agent |
| qitem-20261003093000-222473fe | urlshort-slice-delegated-b | design_review | handed-off | review2-agent |
| qitem-20261003094019-7bfaf4ab | urlshort-slice-delegated-b | plan_lock | handed-off | orchestration-lead |
| qitem-20261003094226-6580fb8d | urlshort-slice-delegated-b | implement | handed-off | development-agent |
| qitem-20261003094649-844b6a83 | urlshort-slice-delegated | plan_lock | handed-off | orchestration-lead |
| qitem-20261003094850-c667801b | urlshort-slice-delegated | implement | handed-off | development-agent |
| qitem-20261003101510-d5f18be9 | urlshort-slice-delegated | qa_check | handed-off | qa-agent |
| qitem-20261003103240-18e29a17 | - | - | done | orchestration-lead |
| qitem-20261003104116-a5a61dcb | urlshort-slice-delegated | code_review | done | review-agent |
| qitem-20261003105943-d93e6776 | urlshort-slice-delegated | implement | handed-off | development-agent |
| qitem-20261003110223-756ec012 | urlshort-slice-delegated | qa_check | handed-off | qa-agent |
| qitem-20261003112133-94584287 | urlshort-slice-delegated | code_review | handed-off | review-agent |
| qitem-20261003112744-2dd7d3cb | urlshort-slice-delegated | integrate | done | orchestration-lead |
| qitem-20261003112820-9f4000bb | - | - | done | qa-agent |
| qitem-20261003113714-4cbdf1e0 | 03-ambiguous-analytics | decompose | handed-off | orchestration-lead |
| qitem-20261003114044-007e2031 | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261003114345-f739a1d5 | 03-ambiguous-analytics | decomposition_review | handed-off | review-agent |
| qitem-20261003114944-9bd32a00 | 03-ambiguous-analytics | mission_plan_lock | handed-off | orchestration-lead |
| qitem-20261003115108-61e03251 | 02-brownfield | decompose | handed-off | orchestration-lead |
| qitem-20261003115740-6f882a4a | 02-brownfield | decomposition_review | handed-off | review-agent |
| qitem-20261003120551-4e8acd30 | 02-brownfield | mission_plan_lock | handed-off | orchestration-lead |
| qitem-20261003120849-f4cbfa97 | - | - | done | orchestration-lead |
| qitem-20261003121811-d329afbc | urlshort-slice-delegated-b | code_review | done | review2-agent |
| qitem-20261003123104-ccd07ab0 | urlshort-slice-delegated-b | implement | handed-off | development-agent |
| qitem-20261003130258-8d163c70 | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261003134551-e4b16c7a | urlshort-slice-delegated-b | code_review | handed-off | review2-agent |
| qitem-20261003135618-4e62dbf5 | - | - | done | qa2-agent |
| qitem-20261003135649-046d003e | urlshort-slice-delegated-b | integrate | done | orchestration-lead |
| qitem-20261003135957-0f6e0c8b | 01-greenfield-core | wave_review | handed-off | review-agent |
| qitem-20261003142414-ac899454 | 01-greenfield-core | release_prep | handed-off | release-agent |
| qitem-20261003153114-04cfbe0a | 01-greenfield-core | release_review | handed-off | review-agent |
| qitem-20261003154114-c0dd70b0 | 03-ambiguous-analytics | wave_integration | handed-off | orchestration-lead |
| qitem-20261003154117-c072093e | 02-brownfield | wave_integration | handed-off | orchestration-lead |
| qitem-20261003154347-19e96a75 | urlshort-slice | requirements | handed-off | requirements-agent |
| qitem-20261003154415-a6020a5f | urlshort-slice-delegated | requirements | handed-off | requirements-agent |
| qitem-20261003154427-8eccec4f | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261003154828-764bc65c | urlshort-slice-delegated | requirements_review | done | review-agent |
| qitem-20261003155116-26bce90f | urlshort-slice-delegated-b | requirements_review | handed-off | review2-agent |
| qitem-20261003161333-c4da0117 | urlshort-slice | requirements_review | handed-off | review-agent |
| qitem-20261003163546-70ed3e48 | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003163608-f96c8e6c | urlshort-slice-delegated | requirements | handed-off | requirements-agent |
| qitem-20261003163921-0a8e2926 | urlshort-slice-delegated | requirements_review | handed-off | review-agent |
| qitem-20261003164151-fbee7e97 | urlshort-slice | design | handed-off | design-agent |
| qitem-20261003164258-4a95943e | urlshort-slice-delegated | design | handed-off | design-agent |
| qitem-20261003165209-ce7abb0e | 01-greenfield-core | ship_signoff | handed-off | release-agent |
| qitem-20261003165532-4a59045f | urlshort-drill | implement | done | release-agent |
| qitem-20261003165738-c5d9694f | urlshort-drill | implement | canceled | release-agent |
| qitem-20261003171157-d2ed38d0 | 01-greenfield-core | evidence_export | handed-off | release-agent |
| qitem-20261003172504-1e5d5337 | urlshort-slice-delegated-b | design_review | done | review2-agent |
| qitem-20261003173636-643c7c17 | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003174406-936f9d9c | urlshort-slice-delegated | design_review | done | review-agent |
| qitem-20261003174521-eb318067 | 01-greenfield-core | mission_close | done | orchestration-lead |
| qitem-20261003175330-fb054f2f | - | - | done | orchestration-lead |
| qitem-20261003175700-cbb50861 | urlshort-slice-delegated | design | handed-off | design-agent |
| qitem-20261003181452-6bc9f909 | urlshort-slice-delegated-b | design_review | done | review2-agent |
| qitem-20261003182016-5973c5c1 | urlshort-slice-delegated | design_review | handed-off | review-agent |
| qitem-20261003182019-5b56b6ce | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003182604-c8ff8e2d | urlshort-slice-delegated | plan_lock | handed-off | orchestration-lead |
| qitem-20261003182724-0843aca3 | urlshort-slice-delegated | implement | handed-off | development-agent |
| qitem-20261003182906-74ccb677 | - | - | done | orchestration-lead |
| qitem-20261003182930-370196d0 | urlshort-slice-delegated-b | design_review | handed-off | review2-agent |
| qitem-20261003183612-3452b207 | urlshort-slice-delegated-b | plan_lock | handed-off | orchestration-lead |
| qitem-20261003183953-9526315e | urlshort-slice-delegated-b | implement | handed-off | development-agent |
| qitem-20261003184238-e2f2be28 | urlshort-slice | design_review | handed-off | review-agent |
| qitem-20261003184336-f1b2d2a0 | urlshort-slice-delegated-b | implement | handed-off | dev2-agent |
| qitem-20261003184357-822e2d22 | - | 5 | done | orchestration-lead |
| qitem-20261003184740-263527f6 | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261003184751-a000203f | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261003184802-67941cc3 | urlshort-slice-delegated | requirements | handed-off | requirements-agent |
| qitem-20261003185217-9f98ac8d | urlshort-slice-delegated-b | requirements_review | handed-off | review2-agent |
| qitem-20261003185423-545a1365 | urlshort-slice-delegated | qa_check | handed-off | qa-agent |
| qitem-20261003185548-520967ab | urlshort-slice-delegated-b | requirements_review | handed-off | review2-agent |
| qitem-20261003185640-3b837606 | urlshort-slice | plan_lock | handed-off | orchestration-lead |
| qitem-20261003185656-e4c8b68d | urlshort-slice-delegated-b | design | handed-off | design2-agent |
| qitem-20261003185900-adac5cf2 | urlshort-slice-delegated | requirements_review | handed-off | review-agent |
| qitem-20261003190115-8ce22243 | urlshort-slice-delegated-b | design | handed-off | design2-agent |
| qitem-20261003190148-7795836d | urlshort-slice-delegated-b | design | handed-off | design-agent |
| qitem-20261003190448-afa9115b | urlshort-slice-delegated | design | handed-off | design-agent |
| qitem-20261003190948-2c1e2288 | urlshort-slice-delegated-b | design_review | handed-off | review2-agent |
| qitem-20261003191531-57bcb230 | urlshort-slice-delegated-b | plan_lock | handed-off | orchestration-lead |
| qitem-20261003192547-28bfe85e | urlshort-slice-delegated-b | design_review | handed-off | review2-agent |
| qitem-20261003193135-f2f66c25 | urlshort-slice-delegated-b | plan_lock | handed-off | orchestration-lead |
| qitem-20261003193154-cf5ec0b6 | urlshort-slice-delegated | design_review | handed-off | review-agent |
| qitem-20261003193221-175d6855 | urlshort-slice-delegated-b | implement | handed-off | dev2-agent |
| qitem-20261003194307-78d5bcf1 | urlshort-slice-delegated | plan_lock | handed-off | orchestration-lead |
| qitem-20261003195138-8eb72ecb | - | - | done | orchestration-lead |
| qitem-20261003195527-d2973fff | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261003195938-d8b8a9c3 | urlshort-slice-delegated | code_review | done | review-agent |
| qitem-20261003200223-d361adc6 | urlshort-slice-delegated | implement | handed-off | development-agent |
| qitem-20261003200325-7115d4e3 | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261003200331-90c97f87 | urlshort-slice | implement | handed-off | dev2-agent |
| qitem-20261003201620-c74de839 | urlshort-slice-delegated | implement | handed-off | development-agent |
| qitem-20261003202154-65e6d10f | urlshort-slice-delegated | qa_check | handed-off | qa-agent |
| qitem-20261003202216-eba453ef | urlshort-slice-delegated-b | code_review | handed-off | review2-agent |
| qitem-20261003203123-a785ee4c | urlshort-slice-delegated-b | integrate | done | orchestration-lead |
| qitem-20261003213051-77ffaad3 | urlshort-slice-delegated | code_review | handed-off | review-agent |
| qitem-20261003214414-12d75069 | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261003214424-a56c0575 | urlshort-slice-delegated | integrate | done | orchestration-lead |
| qitem-20261003215524-d56667a8 | urlshort-slice-delegated | qa_check | handed-off | qa-agent |
| qitem-20261003223352-97e556cd | urlshort-slice-delegated | code_review | handed-off | review-agent |
| qitem-20261003223543-96dcbd88 | urlshort-slice-delegated-b | implement | handed-off | dev2-agent |
| qitem-20261003223549-c479c189 | urlshort-slice-delegated-b | implement | handed-off | development-agent |
| qitem-20261003223905-762d759f | urlshort-slice-delegated-b | code_review | handed-off | review2-agent |
| qitem-20261003224749-c43612f6 | urlshort-slice-delegated | integrate | done | orchestration-lead |
| qitem-20261003225239-ed926320 | urlshort-slice-delegated-b | integrate | done | orchestration-lead |
| qitem-20261003231403-839a47f3 | urlshort-slice | qa_check | handed-off | qa-agent |
| qitem-20261003231955-aed54014 | urlshort-drill | implement | handed-off | release-agent |
| qitem-20261003232046-aa44c49f | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261003232132-8f1e1f16 | urlshort-drill | qa_check | done | qa-agent |
| qitem-20261003232904-3c07d8d6 | urlshort-drill | implement | handed-off | release-agent |
| qitem-20261003233041-b3b78d0d | urlshort-drill | qa_check | done | qa-agent |
| qitem-20261003235705-60b5c52b | urlshort-slice-delegated-b | code_review | handed-off | review2-agent |
| qitem-20261004000110-d0d07fdb | urlshort-slice | code_review | handed-off | review-agent |
| qitem-20261004001239-8ee629f7 | urlshort-slice-delegated-b | integrate | done | orchestration-lead |
| qitem-20261004001800-cc58cd9d | urlshort-slice | integrate | done | orchestration-lead |
| qitem-20261004002455-b7ea811b | 02-brownfield | wave_review | handed-off | review-agent |
| qitem-20261004002509-de60eb81 | 03-ambiguous-analytics | wave_review | handed-off | review-agent |
| qitem-20261004002516-ee95930d | 03-ambiguous-analytics | wave_review | handed-off | review2-agent |
| qitem-20261004003331-0b5245ad | - | - | done | orchestration-lead |
| qitem-20261004003538-62f6c81e | urlshort-slice-delegated-b | requirements | handed-off | requirements-agent |
| qitem-20261004004814-04c1642a | 03-ambiguous-analytics | release_prep | handed-off | release-agent |
| qitem-20261004004828-05d2aab9 | 03-ambiguous-analytics | release_prep | handed-off | release2-agent |
| qitem-20261004004915-91f40366 | urlshort-slice-delegated-b | requirements_review | handed-off | review2-agent |
| qitem-20261004005331-36607695 | - | of | blocked | orchestration-lead |
| qitem-20261004005458-a071fd65 | urlshort-slice-delegated-b | design | handed-off | design2-agent |
| qitem-20261004011019-f67b1c6d | urlshort-slice-delegated-b | design_review | handed-off | review2-agent |
| qitem-20261004012350-fdb23cbc | urlshort-slice-delegated-b | plan_lock | handed-off | orchestration-lead |
| qitem-20261004012507-efa30d6a | urlshort-slice-delegated-b | implement | handed-off | dev2-agent |
| qitem-20261004013841-c2fa8b43 | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261004020758-49ceba8e | 03-ambiguous-analytics | release_review | handed-off | review-agent |
| qitem-20261004023723-f58044d0 | 03-ambiguous-analytics | ship_signoff | handed-off | release-agent |
| qitem-20261004024212-e72494e7 | 03-ambiguous-analytics | evidence_export | handed-off | release-agent |
| qitem-20261004024238-651b51b1 | 03-ambiguous-analytics | evidence_export | handed-off | release2-agent |
| qitem-20261004024533-3e9ae475 | urlshort-slice-delegated-b | code_review | done | review2-agent |
| qitem-20261004025620-a869dc4a | urlshort-slice-delegated-b | implement | handed-off | dev2-agent |
| qitem-20261004025925-a5886b4c | urlshort-slice-delegated-b | qa_check | handed-off | qa2-agent |
| qitem-20261004031539-0bd7032a | 03-ambiguous-analytics | mission_close | done | orchestration-lead |
| qitem-20261004032752-d162e1b3 | urlshort-slice-delegated-b | code_review | handed-off | review2-agent |
| qitem-20261004033137-1400c73d | urlshort-slice-delegated-b | integrate | done | orchestration-lead |
| qitem-20261004034522-8dfeeeca | 02-brownfield | release_prep | handed-off | release-agent |
| qitem-20261004041605-18a958ae | - | - | done | qa2-agent |
| qitem-20261004041607-09149d20 | - | - | done | qa-agent |
| qitem-20261004042358-6587e4be | - | - | done | qa2-agent |
| qitem-20261004050142-3fad2270 | 02-brownfield | release_review | handed-off | review-agent |
| qitem-20261004052127-b89c249b | 02-brownfield | ship_signoff | handed-off | release-agent |
| qitem-20261004052612-42705c30 | 02-brownfield | evidence_export | in-progress | release-agent |
| qitem-20261004052725-7bd25702 | - | - | blocked | qa-agent |
| qitem-recovery-84c436485c1dd14a | - | - | done | release2-agent |

## Step trails (closed at · step · exit · packet · actor)

### 01M3Z8AJ1EDFTHQ1HPAWPNP2YN

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T03:06:00.711Z | mission_close | done | qitem-20261003030415-a2011998 | orchestration-lead |
| 2026-10-03T03:04:15.796Z | evidence_export | handoff | qitem-20261003024639-d36d76cc | release-agent |
| 2026-10-03T02:46:39.191Z | ship_signoff | handoff | qitem-20261003023502-f807af1f | release-agent |
| 2026-10-03T02:35:02.387Z | release_prep | handoff | qitem-20261003020412-57cea2d9 | release-agent |
| 2026-10-03T02:04:12.497Z | wave_review | handoff | qitem-20261003015049-51d1ba00 | review-agent |
| 2026-10-03T01:50:49.320Z | wave_integration | handoff | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T01:34:35.690Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T01:03:31.983Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:58:22.054Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:41:36.327Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:30:18.276Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:50:40.073Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:30:00.966Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:12:47.752Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:08:58.502Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:59:27.677Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:52:38.946Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:12:19.674Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:03:49.310Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:02:22.490Z | mission_plan_lock | handoff | qitem-20261002213817-6d848ac6 | orchestration-lead |
| 2026-10-02T21:38:17.102Z | decompose | handoff | qitem-20261002212902-0f6e128b | orchestration-lead |

### 01M3ZA8Q39QEB3R1QDQCVTER18

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T01:49:36.477Z | slice_accept | done | qitem-20261003013423-99b71ff1 | qa-agent |
| 2026-10-03T01:34:23.192Z | integrate | handoff | qitem-20261003013220-670f8532 | orchestration-lead |
| 2026-10-03T01:32:20.584Z | security_review | handoff | qitem-20261003010318-807bbf8a | review-agent |
| 2026-10-03T01:03:18.901Z | code_review | handoff | qitem-20261003005806-5aaa453e | review-agent |
| 2026-10-03T00:58:06.246Z | qa_check | handoff | qitem-20261003004117-c920fe49 | qa-agent |
| 2026-10-03T00:41:17.523Z | implement | handoff | qitem-20261003003003-9cb0ed55 | development-agent |
| 2026-10-03T00:30:03.244Z | qa_check | failed | qitem-20261002235024-355f67c0 | qa-agent |
| 2026-10-02T23:50:24.208Z | implement | handoff | qitem-20261002232943-d0903176 | development-agent |
| 2026-10-02T23:29:43.738Z | plan_lock | handoff | qitem-20261002231236-cdda3066 | design-agent |
| 2026-10-02T23:12:36.097Z | design_review | handoff | qitem-20261002230841-dc5cdf2b | review-agent |
| 2026-10-02T23:08:40.997Z | design | handoff | qitem-20261002225911-1ad25865 | design-agent |
| 2026-10-02T22:59:11.100Z | design_review | failed | qitem-20261002225126-b5c94d4f | review-agent |
| 2026-10-02T22:51:26.673Z | design | handoff | qitem-20261002221159-a35e50bc | design-agent |
| 2026-10-02T22:11:59.875Z | requirements | handoff | qitem-20261002220259-281a4efc | requirements-agent |

### 01M3ZXEXAMS945ZS0QZ26KZK1V

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T17:46:17.223Z | mission_close | done | qitem-20261003174521-eb318067 | orchestration-lead |
| 2026-10-03T17:45:21.982Z | evidence_export | handoff | qitem-20261003171157-d2ed38d0 | release-agent |
| 2026-10-03T17:12:26.612Z | evidence_export | waiting | qitem-20261003171157-d2ed38d0 | release-agent |
| 2026-10-03T17:11:57.816Z | ship_signoff | handoff | qitem-20261003165209-ce7abb0e | release-agent |
| 2026-10-03T16:52:09.275Z | release_review | handoff | qitem-20261003153114-04cfbe0a | review-agent |
| 2026-10-03T15:47:51.610Z | release_review | waiting | qitem-20261003153114-04cfbe0a | review-agent |
| 2026-10-03T15:31:14.056Z | release_prep | handoff | qitem-20261003142414-ac899454 | release-agent |
| 2026-10-03T14:24:14.154Z | wave_review | handoff | qitem-20261003135957-0f6e0c8b | review-agent |
| 2026-10-03T13:59:57.356Z | wave_integration | handoff | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T11:41:04.904Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T11:29:31.786Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T11:21:46.415Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T11:02:38.215Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T11:00:02.277Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T10:41:33.577Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T10:40:51.175Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T10:15:38.780Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T09:49:00.057Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T09:26:12.191Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T09:02:14.029Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T08:45:33.762Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T08:22:17.950Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T08:13:45.957Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T08:13:27.654Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T08:05:09.306Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T07:40:58.858Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T07:17:13.275Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T06:39:44.980Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T06:26:15.830Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T06:04:06.060Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T05:54:11.549Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T05:12:57.943Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T05:11:26.592Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T05:09:04.240Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T05:03:40.638Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T04:43:02.288Z | wave_integration | waiting | qitem-20261003044200-384e9544 | orchestration-lead |
| 2026-10-03T04:42:00.874Z | mission_plan_lock | handoff | qitem-20261003042553-03ac8b4b | orchestration-lead |
| 2026-10-03T04:25:53.362Z | decomposition_review | handoff | qitem-20261003040709-b481dc8d | review-agent |
| 2026-10-03T04:21:59.809Z | decomposition_review | waiting | qitem-20261003040709-b481dc8d | review-agent |
| 2026-10-03T04:21:03.168Z | decomposition_review | waiting | qitem-20261003040709-b481dc8d | review-agent |
| 2026-10-03T04:14:31.602Z | decomposition_review | waiting | qitem-20261003040709-b481dc8d | review-agent |
| 2026-10-03T04:12:50.359Z | decomposition_review | waiting | qitem-20261003040709-b481dc8d | review-agent |
| 2026-10-03T04:07:09.739Z | decompose | handoff | qitem-20261003033825-0148d3da | orchestration-lead |

### 01M40149S2BGCAKVK83B6VCNKG

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T08:01:09.221Z | integrate | done | qitem-20261003075859-7a5a6e93 | orchestration-lead |
| 2026-10-03T07:58:59.678Z | code_review | handoff | qitem-20261003074021-e3821d80 | review-agent |
| 2026-10-03T07:40:21.577Z | qa_check | handoff | qitem-20261003071605-40d1d3aa | qa-agent |
| 2026-10-03T07:16:05.623Z | implement | handoff | qitem-20261003063930-0cb9c7eb | development-agent |
| 2026-10-03T06:39:30.000Z | plan_lock | handoff | qitem-20261003063348-182b23df | orchestration-lead |
| 2026-10-03T06:33:48.728Z | design_review | handoff | qitem-20261003062552-16fc9d48 | review-agent |
| 2026-10-03T06:25:52.415Z | design | handoff | qitem-20261003060343-3eb84b99 | design-agent |
| 2026-10-03T06:03:43.883Z | design_review | failed | qitem-20261003055347-01d2d207 | review-agent |
| 2026-10-03T05:53:47.632Z | design | handoff | qitem-20261003051242-83fa8c2c | design-agent |
| 2026-10-03T05:12:42.124Z | requirements_review | handoff | qitem-20261003051110-97f19fd2 | review-agent |
| 2026-10-03T05:11:10.253Z | requirements | handoff | qitem-20261003050836-02bc7593 | requirements-agent |
| 2026-10-03T05:08:36.724Z | requirements_review | failed | qitem-20261003050304-b7cdaab5 | review-agent |
| 2026-10-03T05:03:04.140Z | requirements | handoff | qitem-20261003044232-a7194f1d | requirements-agent |

### 01M40CP0JVZZWR8226MNN8XCCV

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T11:29:27.481Z | integrate | done | qitem-20261003112744-2dd7d3cb | orchestration-lead |
| 2026-10-03T11:27:44.089Z | code_review | handoff | qitem-20261003112133-94584287 | review-agent |
| 2026-10-03T11:21:33.827Z | qa_check | handoff | qitem-20261003110223-756ec012 | qa-agent |
| 2026-10-03T11:02:23.898Z | implement | handoff | qitem-20261003105943-d93e6776 | development-agent |
| 2026-10-03T10:59:43.067Z | code_review | failed | qitem-20261003104116-a5a61dcb | review-agent |
| 2026-10-03T10:41:16.455Z | qa_check | handoff | qitem-20261003101510-d5f18be9 | qa-agent |
| 2026-10-03T10:15:10.589Z | implement | handoff | qitem-20261003094850-c667801b | development-agent |
| 2026-10-03T09:48:50.141Z | plan_lock | handoff | qitem-20261003094649-844b6a83 | orchestration-lead |
| 2026-10-03T09:46:49.512Z | design_review | handoff | qitem-20261003092345-52dd8a6e | review-agent |
| 2026-10-03T09:39:02.047Z | design_review | waiting | qitem-20261003092345-52dd8a6e | review-agent |
| 2026-10-03T09:37:55.962Z | design_review | waiting | qitem-20261003092345-52dd8a6e | review-agent |
| 2026-10-03T09:23:45.379Z | design | handoff | qitem-20261003090152-625de326 | design-agent |
| 2026-10-03T09:01:52.157Z | design_review | failed | qitem-20261003084518-9957256a | review-agent |
| 2026-10-03T08:45:18.453Z | design | handoff | qitem-20261003082147-22c46f0c | design-agent |
| 2026-10-03T08:21:47.891Z | requirements_review | handoff | qitem-20261003081331-87faf358 | review-agent |
| 2026-10-03T08:13:31.356Z | requirements | handoff | qitem-20261003080426-686ea065 | requirements-agent |

### 01M40CPBYR97637QGHT57BNEBY

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T13:58:53.675Z | integrate | done | qitem-20261003135649-046d003e | orchestration-lead |
| 2026-10-03T13:56:49.620Z | code_review | handoff | qitem-20261003134551-e4b16c7a | review2-agent |
| 2026-10-03T13:45:51.270Z | qa_check | handoff | qitem-20261003130258-8d163c70 | qa2-agent |
| 2026-10-03T13:02:58.455Z | implement | handoff | qitem-20261003123104-ccd07ab0 | development-agent |
| 2026-10-03T12:31:04.976Z | code_review | failed | qitem-20261003121811-d329afbc | review2-agent |
| 2026-10-03T12:18:11.240Z | qa_check | handoff | qitem-20261003114044-007e2031 | qa2-agent |
| 2026-10-03T11:40:44.001Z | implement | handoff | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T11:27:57.967Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T11:21:47.777Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T11:02:32.908Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T11:00:08.812Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T10:41:33.513Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T10:28:36.770Z | implement | waiting | qitem-20261003094226-6580fb8d | development-agent |
| 2026-10-03T09:42:26.627Z | plan_lock | handoff | qitem-20261003094019-7bfaf4ab | orchestration-lead |
| 2026-10-03T09:40:19.662Z | design_review | handoff | qitem-20261003093000-222473fe | review2-agent |
| 2026-10-03T09:30:00.378Z | design | handoff | qitem-20261003092226-699086e9 | design-agent |
| 2026-10-03T09:22:26.537Z | design_review | failed | qitem-20261003091111-32efdbe2 | review2-agent |
| 2026-10-03T09:11:11.837Z | design | handoff | qitem-20261003082416-b1057e7a | design-agent |
| 2026-10-03T08:24:16.107Z | requirements_review | handoff | qitem-20261003082131-b709b118 | review2-agent |
| 2026-10-03T08:21:31.622Z | requirements | handoff | qitem-20261003081851-f8f25b02 | requirements-agent |
| 2026-10-03T08:18:51.809Z | requirements_review | failed | qitem-20261003081312-64ea3c87 | review2-agent |
| 2026-10-03T08:13:12.907Z | requirements | handoff | qitem-20261003080438-4f449e0f | requirements-agent |

### 01M40RVNDQ0KT7FPWN1KJW0DC3

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-04T03:16:47.236Z | mission_close | done | qitem-20261004031539-0bd7032a | orchestration-lead |
| 2026-10-04T03:15:39.170Z | evidence_export | handoff | qitem-20261004024238-651b51b1 | release2-agent |
| 2026-10-04T02:45:37.223Z | evidence_export | waiting | qitem-20261004024238-651b51b1 | release2-agent |
| 2026-10-04T02:42:12.694Z | ship_signoff | handoff | qitem-20261004023723-f58044d0 | release-agent |
| 2026-10-04T02:37:23.264Z | release_review | handoff | qitem-20261004020758-49ceba8e | review-agent |
| 2026-10-04T02:17:07.184Z | release_review | waiting | qitem-20261004020758-49ceba8e | review-agent |
| 2026-10-04T02:07:58.977Z | release_prep | handoff | qitem-20261004004828-05d2aab9 | release2-agent |
| 2026-10-04T00:48:14.139Z | wave_review | handoff | qitem-20261004002516-ee95930d | review2-agent |
| 2026-10-04T00:25:09.237Z | wave_integration | handoff | qitem-20261003154114-c0dd70b0 | orchestration-lead |
| 2026-10-03T15:41:14.371Z | mission_plan_lock | handoff | qitem-20261003114944-9bd32a00 | orchestration-lead |
| 2026-10-03T11:49:44.054Z | decomposition_review | handoff | qitem-20261003114345-f739a1d5 | review-agent |
| 2026-10-03T11:43:45.395Z | decompose | handoff | qitem-20261003113714-4cbdf1e0 | orchestration-lead |

### 01M40SN34E37K96B38JPG9K41X

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-04T05:26:12.694Z | ship_signoff | handoff | qitem-20261004052127-b89c249b | release-agent |
| 2026-10-04T05:21:27.014Z | release_review | handoff | qitem-20261004050142-3fad2270 | review-agent |
| 2026-10-04T05:01:42.124Z | release_prep | handoff | qitem-20261004034522-8dfeeeca | release-agent |
| 2026-10-04T03:45:22.728Z | wave_review | handoff | qitem-20261004002455-b7ea811b | review-agent |
| 2026-10-04T00:55:49.808Z | wave_review | waiting | qitem-20261004002455-b7ea811b | review-agent |
| 2026-10-04T00:49:13.783Z | wave_review | waiting | qitem-20261004002455-b7ea811b | review-agent |
| 2026-10-04T00:43:26.777Z | wave_review | waiting | qitem-20261004002455-b7ea811b | review-agent |
| 2026-10-04T00:38:09.518Z | wave_review | waiting | qitem-20261004002455-b7ea811b | review-agent |
| 2026-10-04T00:24:55.689Z | wave_integration | handoff | qitem-20261003154117-c072093e | orchestration-lead |
| 2026-10-03T15:41:17.312Z | mission_plan_lock | handoff | qitem-20261003120551-4e8acd30 | orchestration-lead |
| 2026-10-03T12:05:51.889Z | decomposition_review | handoff | qitem-20261003115740-6f882a4a | review-agent |
| 2026-10-03T11:57:40.256Z | decompose | handoff | qitem-20261003115108-61e03251 | orchestration-lead |

### 01M416Z3CM54YQTX93V4KG0CPS

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-04T00:24:28.721Z | integrate | done | qitem-20261004001800-cc58cd9d | orchestration-lead |
| 2026-10-04T00:21:33.232Z | integrate | waiting | qitem-20261004001800-cc58cd9d | orchestration-lead |
| 2026-10-04T00:19:48.462Z | integrate | waiting | qitem-20261004001800-cc58cd9d | orchestration-lead |
| 2026-10-04T00:18:00.297Z | code_review | handoff | qitem-20261004000110-d0d07fdb | review-agent |
| 2026-10-04T00:01:10.021Z | qa_check | handoff | qitem-20261003231403-839a47f3 | qa-agent |
| 2026-10-03T23:14:03.725Z | implement | handoff | qitem-20261003200331-90c97f87 | dev2-agent |
| 2026-10-03T22:52:58.246Z | implement | waiting | qitem-20261003200331-90c97f87 | dev2-agent |
| 2026-10-03T22:39:19.326Z | implement | waiting | qitem-20261003200331-90c97f87 | dev2-agent |
| 2026-10-03T21:45:42.131Z | implement | waiting | qitem-20261003200331-90c97f87 | dev2-agent |
| 2026-10-03T20:30:43.817Z | implement | waiting | qitem-20261003200331-90c97f87 | dev2-agent |
| 2026-10-03T20:03:25.466Z | plan_lock | handoff | qitem-20261003185640-3b837606 | orchestration-lead |
| 2026-10-03T18:56:40.280Z | design_review | handoff | qitem-20261003184238-e2f2be28 | review-agent |
| 2026-10-03T18:42:38.609Z | design | handoff | qitem-20261003164151-fbee7e97 | design-agent |
| 2026-10-03T16:41:51.871Z | requirements_review | handoff | qitem-20261003161333-c4da0117 | review-agent |
| 2026-10-03T16:13:33.159Z | requirements | handoff | qitem-20261003154347-19e96a75 | requirements-agent |

### 01M416ZY5N11CDGZBM2DT4GAXS

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T21:52:12.443Z | integrate | done | qitem-20261003214424-a56c0575 | orchestration-lead |
| 2026-10-03T21:44:24.776Z | code_review | handoff | qitem-20261003213051-77ffaad3 | review-agent |
| 2026-10-03T21:30:51.009Z | qa_check | handoff | qitem-20261003202154-65e6d10f | qa-agent |
| 2026-10-03T20:21:54.419Z | implement | handoff | qitem-20261003201620-c74de839 | development-agent |
| 2026-10-03T20:16:20.807Z | code_review | failed | qitem-20261003195938-d8b8a9c3 | review-agent |
| 2026-10-03T19:59:38.647Z | qa_check | handoff | qitem-20261003185423-545a1365 | qa-agent |
| 2026-10-03T18:54:23.675Z | implement | handoff | qitem-20261003182724-0843aca3 | development-agent |
| 2026-10-03T18:27:24.881Z | plan_lock | handoff | qitem-20261003182604-c8ff8e2d | orchestration-lead |
| 2026-10-03T18:26:04.705Z | design_review | handoff | qitem-20261003182016-5973c5c1 | review-agent |
| 2026-10-03T18:20:16.931Z | design | handoff | qitem-20261003175700-cbb50861 | design-agent |
| 2026-10-03T17:57:00.851Z | design_review | failed | qitem-20261003174406-936f9d9c | review-agent |
| 2026-10-03T17:44:06.368Z | design | handoff | qitem-20261003164258-4a95943e | design-agent |
| 2026-10-03T16:42:58.860Z | requirements_review | handoff | qitem-20261003163921-0a8e2926 | review-agent |
| 2026-10-03T16:39:21.889Z | requirements | handoff | qitem-20261003163608-f96c8e6c | requirements-agent |
| 2026-10-03T16:36:08.985Z | requirements_review | failed | qitem-20261003154828-764bc65c | review-agent |
| 2026-10-03T15:48:28.989Z | requirements | handoff | qitem-20261003154415-a6020a5f | requirements-agent |

### 01M4170AA9E72WW1BXEA5PX0AP

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T23:08:34.760Z | integrate | done | qitem-20261003225239-ed926320 | orchestration-lead |
| 2026-10-03T22:52:39.981Z | code_review | handoff | qitem-20261003223905-762d759f | review2-agent |
| 2026-10-03T22:39:05.832Z | qa_check | handoff | qitem-20261003214414-12d75069 | qa2-agent |
| 2026-10-03T21:44:13.971Z | implement | handoff | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T21:31:08.675Z | implement | waiting | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T20:22:24.972Z | implement | waiting | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T20:17:16.652Z | implement | waiting | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T19:59:58.782Z | implement | waiting | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T19:12:10.306Z | implement | waiting | qitem-20261003184336-f1b2d2a0 | dev2-agent |
| 2026-10-03T18:39:53.928Z | plan_lock | handoff | qitem-20261003183612-3452b207 | orchestration-lead |
| 2026-10-03T18:36:12.919Z | design_review | handoff | qitem-20261003182930-370196d0 | review2-agent |
| 2026-10-03T18:29:30.279Z | design | handoff | qitem-20261003182019-5b56b6ce | design-agent |
| 2026-10-03T18:20:19.037Z | design_review | failed | qitem-20261003181452-6bc9f909 | review2-agent |
| 2026-10-03T18:14:52.520Z | design | handoff | qitem-20261003173636-643c7c17 | design-agent |
| 2026-10-03T17:36:36.777Z | design_review | failed | qitem-20261003172504-1e5d5337 | review2-agent |
| 2026-10-03T17:25:04.031Z | design | handoff | qitem-20261003163546-70ed3e48 | design-agent |
| 2026-10-03T16:35:46.921Z | requirements_review | handoff | qitem-20261003155116-26bce90f | review2-agent |
| 2026-10-03T15:51:16.642Z | requirements | handoff | qitem-20261003154427-8eccec4f | requirements-agent |

### 01M41B1ABGY3WR0DKEPZCJE9D3

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T16:57:48.793Z | implement | failed | qitem-20261003165738-c5d9694f | orchestration-lead |
| 2026-10-03T16:57:21.339Z | implement | failed | qitem-20261003165532-4a59045f | release-agent |

### 01M41HFT4CTYPNWWNB8PJ99NFE

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T20:33:20.856Z | integrate | done | qitem-20261003203123-a785ee4c | orchestration-lead |
| 2026-10-03T20:31:23.360Z | code_review | handoff | qitem-20261003202216-eba453ef | review2-agent |
| 2026-10-03T20:22:16.897Z | qa_check | handoff | qitem-20261003195527-d2973fff | qa2-agent |
| 2026-10-03T19:55:27.654Z | implement | handoff | qitem-20261003193221-175d6855 | dev2-agent |
| 2026-10-03T19:32:21.419Z | plan_lock | handoff | qitem-20261003193135-f2f66c25 | orchestration-lead |
| 2026-10-03T19:31:35.128Z | design_review | handoff | qitem-20261003192547-28bfe85e | review2-agent |
| 2026-10-03T19:25:47.515Z | design | handoff | qitem-20261003185656-e4c8b68d | design2-agent |
| 2026-10-03T18:56:56.531Z | requirements_review | handoff | qitem-20261003185217-9f98ac8d | review2-agent |
| 2026-10-03T18:52:17.325Z | requirements | handoff | qitem-20261003184740-263527f6 | requirements-agent |

### 01M41HG4P6DEVAKTCJC6J9QAWP

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-04T00:24:31.316Z | integrate | done | qitem-20261004001239-8ee629f7 | orchestration-lead |
| 2026-10-04T00:15:58.549Z | integrate | waiting | qitem-20261004001239-8ee629f7 | orchestration-lead |
| 2026-10-04T00:12:39.968Z | code_review | handoff | qitem-20261003235705-60b5c52b | review2-agent |
| 2026-10-03T23:57:05.799Z | qa_check | handoff | qitem-20261003232046-aa44c49f | qa2-agent |
| 2026-10-03T23:20:46.347Z | implement | handoff | qitem-20261003223549-c479c189 | development-agent |
| 2026-10-03T22:45:16.962Z | implement | waiting | qitem-20261003223549-c479c189 | development-agent |
| 2026-10-03T22:35:43.497Z | plan_lock | handoff | qitem-20261003191531-57bcb230 | orchestration-lead |
| 2026-10-03T19:15:31.504Z | design_review | handoff | qitem-20261003190948-2c1e2288 | review2-agent |
| 2026-10-03T19:09:48.449Z | design | handoff | qitem-20261003190148-7795836d | design-agent |
| 2026-10-03T19:01:15.631Z | requirements_review | handoff | qitem-20261003185548-520967ab | review2-agent |
| 2026-10-03T18:55:48.206Z | requirements | handoff | qitem-20261003184751-a000203f | requirements-agent |

### 01M41HGF6AHB6AZR3Q0KQ8MQR7

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T22:49:55.369Z | integrate | done | qitem-20261003224749-c43612f6 | orchestration-lead |
| 2026-10-03T22:47:49.717Z | code_review | handoff | qitem-20261003223352-97e556cd | review-agent |
| 2026-10-03T22:33:52.552Z | qa_check | handoff | qitem-20261003215524-d56667a8 | qa-agent |
| 2026-10-03T21:55:24.945Z | implement | handoff | qitem-20261003200223-d361adc6 | development-agent |
| 2026-10-03T20:09:10.470Z | implement | waiting | qitem-20261003200223-d361adc6 | development-agent |
| 2026-10-03T20:02:23.719Z | plan_lock | handoff | qitem-20261003194307-78d5bcf1 | orchestration-lead |
| 2026-10-03T19:43:07.602Z | design_review | handoff | qitem-20261003193154-cf5ec0b6 | review-agent |
| 2026-10-03T19:31:54.172Z | design | handoff | qitem-20261003190448-afa9115b | design-agent |
| 2026-10-03T19:04:48.520Z | requirements_review | handoff | qitem-20261003185900-adac5cf2 | review-agent |
| 2026-10-03T18:59:00.073Z | requirements | handoff | qitem-20261003184802-67941cc3 | requirements-agent |

### 01M4212A8BKA6JRZHZQBD90D07

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T23:32:07.695Z | qa_check | done | qitem-20261003233041-b3b78d0d | qa-agent |
| 2026-10-03T23:30:41.601Z | implement | handoff | qitem-20261003232904-3c07d8d6 | release-agent |
| 2026-10-03T23:29:04.090Z | qa_check | failed | qitem-20261003232132-8f1e1f16 | qa-agent |
| 2026-10-03T23:21:32.117Z | implement | handoff | qitem-20261003231955-aed54014 | release-agent |

### 01M425CY9Z4K03G14Y677AT0PA

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-04T03:45:35.625Z | integrate | done | qitem-20261004033137-1400c73d | orchestration-lead |
| 2026-10-04T03:37:35.520Z | integrate | waiting | qitem-20261004033137-1400c73d | orchestration-lead |
| 2026-10-04T03:33:52.923Z | integrate | waiting | qitem-20261004033137-1400c73d | orchestration-lead |
| 2026-10-04T03:31:37.315Z | code_review | handoff | qitem-20261004032752-d162e1b3 | review2-agent |
| 2026-10-04T03:27:52.590Z | qa_check | handoff | qitem-20261004025925-a5886b4c | qa2-agent |
| 2026-10-04T02:59:25.589Z | implement | handoff | qitem-20261004025620-a869dc4a | dev2-agent |
| 2026-10-04T02:56:20.397Z | code_review | failed | qitem-20261004024533-3e9ae475 | review2-agent |
| 2026-10-04T02:45:33.578Z | qa_check | handoff | qitem-20261004013841-c2fa8b43 | qa2-agent |
| 2026-10-04T01:38:41.837Z | implement | handoff | qitem-20261004012507-efa30d6a | dev2-agent |
| 2026-10-04T01:25:07.857Z | plan_lock | handoff | qitem-20261004012350-fdb23cbc | orchestration-lead |
| 2026-10-04T01:23:50.737Z | design_review | handoff | qitem-20261004011019-f67b1c6d | review2-agent |
| 2026-10-04T01:10:19.303Z | design | handoff | qitem-20261004005458-a071fd65 | design2-agent |
| 2026-10-04T00:54:58.619Z | requirements_review | handoff | qitem-20261004004915-91f40366 | review2-agent |
| 2026-10-04T00:49:15.260Z | requirements | handoff | qitem-20261004003538-62f6c81e | requirements-agent |

## Final export and custody

This is the final mission02 audit export after independent review446eca31 PASS, human2078 local-use approval, mission delivery10df955a and six slice stamps2020d53b. [RELEASE](../../../missions/02-brownfield/RELEASE.md) is the claim-to-evidence map. Current mission proof is ready64/64. The lead rechecked all six slices after stamps and explicitly waived the unnecessary SPEC-only round because no item drifted; no QA blocker for mission02 remains.

- Human approval checkpoints and cross-stage lineage: [ship gate transitions](packets/qitem-20261004052127-b89c249b.transitions.json) preserve the exact decision and qualified RELEASE path; [review trail](instances/01M40SN34E37K96B38JPG9K41X.trace.json) records release_review and ship_signoff handoffs. Stamps live in committed SPEC frontmatter, not in this export directory.
- Audit-grade observability and controlled autonomy: [proof readiness](proof-readiness.json), all packet shows/transitions and generated tables preserve attributed judgments and current custody. [Final validation](final-validation.json) checks committed proof-reference hashes, delivery stamps, raw JSON and links.
- Explicit dependency graph and dynamic re-planning: the compiled graph is the authored disk projection; bound revisions and adoption history remain in instance shows. Other missions and drills are also exported; directory labels do not establish mission membership.
- Reliability metrics and rollback: [reviewed preparation export and dated metrics](../../../missions/02-brownfield/release/final-evidence-export/preparation-export-ad0c01f6.tar.gz) preserve the complete prior snapshot. Shared metrics still freeze at04:46:43Z and derive from their archived inputs, not from these later refreshed records. All50 reviewed final-preparation files remain unchanged. Historical8d discrepancy, text-match rollback/resume heuristic, mean-of-instance MTTR and omitted custom waits remain explicit. [Derivations](../../metrics/README.md) and [four drills](../../scenarios/drills.md) retain those boundaries.
- Safe-stop and ordering: [mission03 QA7bd25702](packets/qitem-20261004052725-7bd25702.show.json) is parked on this export and runs afterwards; it does not block mission02. A connection failure prevented the intended pre-handoff lead notification; successful delivery followed05:26Z and the lead authorized export05:27Z. [Export transitions](packets/qitem-20261004052612-42705c30.transitions.json) correct the earlier timing claim.

The frozen preparation validation refers to the archived preparation snapshot; final-validation.json covers current records. Usage is a rolling24h snapshot, not a mission-only bill. This capture precedes its own evidence_export closure and mission_close, so those later events are not falsely claimed present. No product rerun, push, release tag, publication or remote exposure.

Final validation parses477 raw JSON files (excluding its own generated report),17 instance trails and205 packet shows. The exporter header counts204 trail-named packets; the table additionally includes the downstream mission03 QA packet. All174 distinct current proof-reference files match committed bytes;148 local links/anchors open. All50 frozen preparation files and the archived preparation manifest match their reviewed pin.

## Self-check

Final verification opens current raw records and local links, checks human decision verbatim, all seven delivery stamps and ready64/64 proof against committed bytes, and validates every frozen preparation file plus the prior export archive. No original artifact, metrics or known gap was silently rewritten. Final report counts are recorded in final-validation.json. Later mission03 QA and the cross-mission FINAL-SUMMARY remain lead custody.
