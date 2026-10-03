#!/usr/bin/env bash
# Export the OpenRig audit trail for one mission into docs/evidence/<mission>/.
# Everything is raw daemon output (--json) so a reader who cannot run OpenRig
# still gets the primary record: compiled dependency graph, scope audit, proof
# readiness, every workflow instance's append-only step trail, and the queue
# transition log of every packet those trails name.
#
#   tools/evidence-export.sh <mission-dir-name> [instance-id ...]
#
# Without instance ids, every workflow instance known to the daemon is exported
# (there are few; exporting all keeps this idempotent and complete).
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
MISSION="${1:?mission dir name, e.g. 00-hello}"; shift || true
OUT="$ROOT/docs/evidence/$MISSION"
mkdir -p "$OUT/instances" "$OUT/packets"
cd "$ROOT"

stamp() { date -u +%Y-%m-%dT%H:%M:%SZ; }
echo "exporting evidence for $MISSION at $(stamp)"

rig workflow compile "$ROOT/missions/$MISSION" --json > "$OUT/compiled-graph.json" || true
rig scope audit --mission "$MISSION" --json > "$OUT/scope-audit.json" 2>/dev/null || true
rig proof show "$MISSION" --json > "$OUT/proof-readiness.json" 2>/dev/null || true
rig workflow list --json > "$OUT/workflow-list.json" 2>/dev/null || true
rig workflow status --json > "$OUT/workflow-status.json" 2>/dev/null || true
rig queue list --json > "$OUT/queue-active.json" 2>/dev/null || true
rig usage top --window 24h --json > "$OUT/usage-top.json" 2>/dev/null || true   # a mission day, not the 1h default

if [ "$#" -gt 0 ]; then
  INSTANCES=("$@")
else
  # Accept any of the id field spellings the list may use; dedupe.
  INSTANCES=($(node -e '
    let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
      let j; try{ j=JSON.parse(s) }catch{ process.exit(0) }
      const rows=Array.isArray(j)?j:(j.instances||j.rows||j.items||[]);
      const ids=new Set(rows.map(r=>r.instanceId||r.instance_id||r.id).filter(Boolean));
      console.log([...ids].join(" "));
    })' < "$OUT/workflow-list.json" 2>/dev/null || true))
fi

PACKETS=()
for id in "${INSTANCES[@]:-}"; do
  [ -z "$id" ] && continue
  rig workflow trace "$id" --json > "$OUT/instances/$id.trace.json" 2>/dev/null || continue
  rig workflow show "$id" --json > "$OUT/instances/$id.show.json" 2>/dev/null || true
  # every qitem id mentioned anywhere in the trail
  while read -r q; do PACKETS+=("$q"); done < <(grep -oE 'qitem-[A-Za-z0-9_-]+|"packetId":"[^"]+"' "$OUT/instances/$id.trace.json" | sed -E 's/"packetId":"//; s/"$//' | sort -u)
done

for q in $(printf '%s\n' "${PACKETS[@]:-}" | sort -u); do
  [ -z "$q" ] && continue
  rig queue transitions "$q" --json > "$OUT/packets/$q.transitions.json" 2>/dev/null || true
  rig queue show "$q" --full --json > "$OUT/packets/$q.show.json" 2>/dev/null || true
done

{
  echo "# Evidence export — $MISSION"
  echo
  echo "Exported $(stamp) by tools/evidence-export.sh from OpenRig $(rig --version)."
  echo
  echo "| Artifact | Governance clause (docs/GOVERNANCE.md) |"
  echo "|---|---|"
  echo "| compiled-graph.json | Explicit dependency graph with entry/exit gates: the mission DAG as compiled from the authored sources at export time (if a running instance was bound to an earlier version, the lead keeps that version here and the disk compile beside it as compiled-graph.authored-*.json; see Dynamic re-planning) |"
  echo "| instances/*.trace.json | Cross-stage context and decision lineage; Bounded retries: the append-only step trail, one entry per closed packet with closureReason handoff / waiting / failed / done, actor and evidence_ref; failed → implement hops are the retries |"
  echo "| instances/*.show.json | Dynamic re-planning; Fallback: bound sources and digests, revisionHistory (rig workflow revise receipts), the reconciliation block comparing the bound graph with the authored one, exception routing, resumeCount |"
  echo "| packets/*.transitions.json | Human approval checkpoints; Safe-stop; Audit-grade observability: every state change of every packet with actor and timestamp, including the engine's gate park on human@kernel and the human's decision text on rig queue resolve |"
  echo "| packets/*.show.json | Human approval checkpoints: the packet as last seen, with summary, evidence_ref, tier, tags (step, gate) and chain of record |"
  echo "| proof-readiness.json | Controlled agent autonomy; Audit-grade observability: attributed proof judgments per slice (rig proof judge receipts, judge seat, subject commit, evidence hashes) and readiness |"
  echo "| scope-audit.json | Policy guardrails: convention audit of mission and slice files (advisory) |"
  echo "| workflow-status.json / workflow-list.json | Sequential and parallel paths: instance states and attention classes at export time (overlapping instance timestamps show pipeline parallelism) |"
  echo "| queue-active.json | Safe-stop: live queue rows at export time (what was still held or parked) |"
  echo "| usage-top.json | Reliability metrics: per-seat token burn over the window (24 h) |"
  echo "| ../../metrics/metrics.json, ../../metrics/README.md | Reliability metrics: success rate, retries, rollbacks, MTTR, latency, human wait derived from these files by tools/sdlc-metrics.mjs |"
  echo
  echo "Approval stamps are not in this export: they live in the stamped files' frontmatter (missions/<m>/SPEC.md, slices/*/SPEC.md: approved-spec-*, approved-*) with append-only audit rows daemon-side; the decision text behind each stamp is in the gate packet's transitions here."
  echo
  echo "Instances exported: ${#INSTANCES[@]}; packets exported: $(ls "$OUT/packets" 2>/dev/null | grep -c transitions || true)."
} > "$OUT/INDEX.md"
# packets and step trails as tables, so no seat has to hand-roll shell loops over the export
node "$ROOT/tools/evidence-index.mjs" "$OUT" >> "$OUT/INDEX.md" 2>/dev/null || true
# mission-specific notes written by the release agent survive re-runs
if [ -f "$OUT/INDEX-notes.md" ]; then { echo; cat "$OUT/INDEX-notes.md"; } >> "$OUT/INDEX.md"; fi
echo "done → $OUT"
