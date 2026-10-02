#!/usr/bin/env node
// Derive SDLC reliability metrics from OpenRig's own records: workflow step
// trails (append-only) and queue transition logs. OpenRig has no built-in
// rollup for success rate / retries / rollbacks / MTTR / latency, so this tool
// computes them from the exported evidence (docs/evidence/**/instances,
// docs/evidence/**/packets) or, with --live, straight from the daemon.
//
//   node tools/sdlc-metrics.mjs [--live] [--out docs/metrics]
//
// Definitions (also printed into docs/metrics/README.md):
//   e2e latency     createdAt -> completedAt of an instance (or now, if active)
//   step closure    one trail entry; closureReason handoff|done = success, failed = artifact rejected
//   retry           a failed closure (QA / review / security sent the candidate back) or a step re-entered
//   rollback        a transition or evidence note mentioning revert/rollback, plus engine resumes (resumeCount)
//   MTTR            failed closure of step S -> next successful closure of S (mean over occurrences)
//   human wait      packet parked on human@kernel -> transition recorded by human@kernel
//   queue wait      packet created -> claimed; work time = claimed -> closed
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const argv = process.argv.slice(2);
const live = argv.includes("--live");
const root = execFileSync("git", ["rev-parse", "--show-toplevel"], { encoding: "utf8" }).trim();
const outArg = argv.includes("--out") ? argv[argv.indexOf("--out") + 1] : "docs/metrics";
const outDir = path.isAbsolute(outArg) ? outArg : path.join(root, outArg);
const now = Date.now();

function rig(...args) {
  const out = execFileSync("rig", [...args, "--json"], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  return JSON.parse(out);
}
const ts = (s) => (s ? Date.parse(s) : NaN);
const sec = (ms) => (Number.isFinite(ms) ? Math.round(ms / 1000) : null);
const readJson = (p) => JSON.parse(fs.readFileSync(p, "utf8"));
const glob = (dir, suffix) => (fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => f.endsWith(suffix)).map((f) => path.join(dir, f)) : []);

// ---- load instances + packets -------------------------------------------------
const instances = new Map(); // instanceId -> { trace, packets: Map<qitemId, transitions[]>, shows: Map<qitemId, show> }
const packetFiles = new Map();
const showFiles = new Map();
const evidenceRoot = path.join(root, "docs", "evidence");
if (fs.existsSync(evidenceRoot)) {
  for (const mission of fs.readdirSync(evidenceRoot)) {
    for (const f of glob(path.join(evidenceRoot, mission, "instances"), ".trace.json")) {
      const trace = readJson(f);
      if (trace?.instance?.instanceId) instances.set(trace.instance.instanceId, { trace, packets: new Map(), shows: new Map(), mission });
    }
    for (const f of glob(path.join(evidenceRoot, mission, "packets"), ".transitions.json")) packetFiles.set(path.basename(f, ".transitions.json"), f);
    for (const f of glob(path.join(evidenceRoot, mission, "packets"), ".show.json")) showFiles.set(path.basename(f, ".show.json"), f);
  }
}
if (live) {
  for (const row of rig("workflow", "list")) {
    try { instances.set(row.instanceId, { trace: rig("workflow", "trace", row.instanceId), packets: new Map(), shows: new Map(), mission: "(live)" }); } catch { /* skip */ }
  }
}

function packetIds(inst) {
  const ids = new Set(inst.trace.instance?.currentFrontier ?? []);
  for (const t of inst.trace.trail ?? []) {
    if (t.priorQitemId) ids.add(t.priorQitemId);
    if (t.nextQitemId) ids.add(t.nextQitemId);
    for (const id of t.closureEvidence?.next_qitem_ids ?? []) ids.add(id);
  }
  return [...ids];
}
function loadTransitions(id) {
  if (packetFiles.has(id)) return readJson(packetFiles.get(id));
  if (live) { try { return rig("queue", "transitions", id); } catch { return []; } }
  return [];
}
function loadShow(id) {
  if (showFiles.has(id)) return readJson(showFiles.get(id));
  if (live) { try { return rig("queue", "show", id, "--full"); } catch { return null; } }
  return null;
}

// ---- per-instance metrics ---------------------------------------------------------
const report = [];
for (const [id, inst] of instances) {
  const { instance, trail = [] } = inst.trace;
  const stepOf = new Map();
  for (const t of trail) if (t.priorQitemId) stepOf.set(t.priorQitemId, t.stepId);
  for (const f of inst.trace.frontier ?? []) if (f.packetId) stepOf.set(f.packetId, f.stepId);

  const packets = [];
  for (const q of packetIds(inst)) {
    const tr = [...loadTransitions(q)].sort((a, b) => ts(a.ts) - ts(b.ts));
    if (!tr.length) continue;
    const show = loadShow(q);
    const step = stepOf.get(q) ?? (show?.tags ?? []).find((t) => t.startsWith("step:"))?.slice(5) ?? "?";
    const created = ts(tr[0].ts);
    const claimed = ts(tr.find((t) => t.state === "in-progress" && /claim/i.test(t.transitionNote ?? ""))?.ts);
    const closedRow = tr.find((t) => ["handed-off", "done", "failed", "canceled", "denied"].includes(t.state));
    const closed = ts(closedRow?.ts);
    let humanWait = 0;
    for (let i = 0; i < tr.length; i++) {
      const t = tr[i];
      const parkedOnHuman = t.state === "blocked" && /human@kernel/.test(`${t.transitionNote ?? ""} ${t.closureTarget ?? ""}`);
      if (!parkedOnHuman) continue;
      const release = tr.slice(i + 1).find((u) => u.actorSession === "human@kernel" || u.state !== "blocked");
      humanWait += (release ? ts(release.ts) : now) - ts(t.ts);
    }
    const rollbackNotes = tr.filter((t) => /revert|rollback|rolled back/i.test(t.transitionNote ?? "")).length;
    packets.push({ qitemId: q, step, queueWaitSec: sec(claimed - created), workSec: sec(closed - claimed), humanWaitSec: sec(humanWait), closedState: closedRow?.state ?? null, rollbackNotes });
  }

  const closures = trail.map((t) => ({ step: t.stepId, reason: t.closureReason, at: ts(t.closedAt), note: `${t.closureEvidence?.evidence_ref ?? ""}` }));
  const failed = closures.filter((c) => c.reason === "failed");
  const stepCounts = closures.reduce((m, c) => m.set(c.step, (m.get(c.step) ?? 0) + 1), new Map());
  const reentries = [...stepCounts.values()].reduce((a, n) => a + Math.max(0, n - 1), 0);
  const mttrs = failed.map((f) => {
    const fix = closures.find((c) => c.step === f.step && c.at > f.at && c.reason !== "failed");
    return fix ? fix.at - f.at : null;
  }).filter((x) => x !== null);
  const rollbacks = closures.filter((c) => /revert|rollback/i.test(c.note)).length + packets.reduce((a, p) => a + p.rollbackNotes, 0) + (instance.resumeCount ?? 0);
  const end = instance.completedAt ? ts(instance.completedAt) : now;

  report.push({
    instanceId: id,
    workflow: instance.workflowName,
    mission: inst.mission,
    status: instance.status,
    createdAt: instance.createdAt,
    completedAt: instance.completedAt ?? null,
    e2eLatencySec: sec(end - ts(instance.createdAt)),
    hops: instance.hopCount ?? trail.length,
    stepClosures: closures.length,
    failedClosures: failed.length,
    reentries,
    retries: failed.length + reentries,
    rollbacks,
    resumeCount: instance.resumeCount ?? 0,
    mttrSec: mttrs.length ? sec(mttrs.reduce((a, b) => a + b, 0) / mttrs.length) : null,
    humanWaitSec: packets.reduce((a, p) => a + (p.humanWaitSec ?? 0), 0),
    queueWaitSec: packets.reduce((a, p) => a + (p.queueWaitSec ?? 0), 0),
    workSec: packets.reduce((a, p) => a + (p.workSec ?? 0), 0),
    packets,
  });
}

// ---- totals -------------------------------------------------------------------------
const terminal = report.filter((r) => ["completed", "failed", "aborted"].includes(r.status));
const completed = report.filter((r) => r.status === "completed");
const allClosures = report.reduce((a, r) => a + r.stepClosures, 0);
const allFailed = report.reduce((a, r) => a + r.failedClosures, 0);
const mttrAll = report.filter((r) => r.mttrSec !== null).map((r) => r.mttrSec);
const pct = (arr, p) => { if (!arr.length) return null; const s = [...arr].sort((a, b) => a - b); return s[Math.min(s.length - 1, Math.floor(p * (s.length - 1)))]; };
const totals = {
  generatedAt: new Date(now).toISOString(),
  instances: report.length,
  active: report.filter((r) => r.status === "active" || r.status === "waiting").length,
  completed: completed.length,
  failed: report.filter((r) => r.status === "failed").length,
  aborted: report.filter((r) => r.status === "aborted").length,
  instanceSuccessRate: terminal.length ? +(completed.length / terminal.length).toFixed(3) : null,
  stepClosures: allClosures,
  failedClosures: allFailed,
  stepSuccessRate: allClosures ? +((allClosures - allFailed) / allClosures).toFixed(3) : null,
  retries: report.reduce((a, r) => a + r.retries, 0),
  rollbacks: report.reduce((a, r) => a + r.rollbacks, 0),
  mttrMeanSec: mttrAll.length ? Math.round(mttrAll.reduce((a, b) => a + b, 0) / mttrAll.length) : null,
  e2eP50Sec: pct(completed.map((r) => r.e2eLatencySec), 0.5),
  e2eP95Sec: pct(completed.map((r) => r.e2eLatencySec), 0.95),
  humanWaitTotalSec: report.reduce((a, r) => a + r.humanWaitSec, 0),
};

// ---- write -------------------------------------------------------------------------
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, "metrics.json"), JSON.stringify({ totals, instances: report }, null, 2) + "\n");
const fmt = (s) => (s === null || s === undefined ? "–" : s >= 3600 ? `${(s / 3600).toFixed(1)} h` : s >= 60 ? `${Math.round(s / 60)} min` : `${s} s`);
const rows = report.map((r) => `| ${r.instanceId.slice(0, 10)}… | ${r.workflow.replace("lifecycle-urlshort-", "")} | ${r.status} | ${fmt(r.e2eLatencySec)} | ${r.hops} | ${r.stepClosures} | ${r.retries} | ${r.rollbacks} | ${fmt(r.humanWaitSec)} | ${fmt(r.mttrSec)} |`).join("\n");
const md = `# SDLC reliability metrics

Generated ${totals.generatedAt} by \`tools/sdlc-metrics.mjs\` from OpenRig workflow trails and queue transition logs (${live ? "live daemon read" : "docs/evidence exports"}).

## Totals

| Metric | Value |
|---|---|
| Workflow instances (active / completed / failed / aborted) | ${totals.instances} (${totals.active} / ${totals.completed} / ${totals.failed} / ${totals.aborted}) |
| Instance success rate (completed ÷ terminal) | ${totals.instanceSuccessRate ?? "– (no terminal instance yet)"} |
| Step closures (failed) | ${totals.stepClosures} (${totals.failedClosures}) |
| Step success rate | ${totals.stepSuccessRate ?? "–"} |
| Retries (failed closures + step re-entries) | ${totals.retries} |
| Rollbacks (revert notes + engine resumes) | ${totals.rollbacks} |
| MTTR, mean (failed closure → next successful closure of that step) | ${fmt(totals.mttrMeanSec)} |
| End-to-end latency, completed instances p50 / p95 | ${fmt(totals.e2eP50Sec)} / ${fmt(totals.e2eP95Sec)} |
| Time parked on the human (all gates) | ${fmt(totals.humanWaitTotalSec)} |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
${rows}

## Derivations and honest limits

- **Source of truth**: \`rig workflow trace --json\` (append-only step trail; one entry per closed packet with \`closureReason\` handoff/done/failed) and \`rig queue transitions --json\` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Retry** counts an artifact verdict of \`failed\` (QA, code review or security review sent the candidate back) plus any step closed more than once. A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts transition or evidence notes mentioning revert/rollback and engine \`resumeCount\`; a Git revert by the integrator is visible in \`git log\` as well.
- **MTTR** is measured from a failed closure to the next successful closure of the same step, i.e. the time to repair the candidate and pass that check again. Instances with no failure have no MTTR (shown as –), not zero.
- **Human wait** is time a packet spent parked on \`human@kernel\`; it is reported separately so agent throughput and human latency are not conflated.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: \`rig usage top --json\` in \`docs/evidence/<mission>/usage-top.json\`.
`;
fs.writeFileSync(path.join(outDir, "README.md"), md);
console.log(`metrics: ${report.length} instance(s) → ${path.relative(root, path.join(outDir, "metrics.json"))}, ${path.relative(root, path.join(outDir, "README.md"))}`);
console.log(JSON.stringify(totals));
