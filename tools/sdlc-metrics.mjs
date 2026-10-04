#!/usr/bin/env node
// Derive SDLC reliability metrics from OpenRig's own records: workflow step
// trails (append-only) and queue transition logs. OpenRig has no built-in
// rollup for success rate / retries / rollbacks / MTTR / latency, so this tool
// computes them from the exported evidence (docs/evidence/**/instances,
// docs/evidence/**/packets) or, with --live, straight from the daemon.
//
//   node tools/sdlc-metrics.mjs [--live] [--out docs/metrics] [--save-inputs docs/evidence/run-end] [--check]
//
//   --save-inputs <dir>  write the exact traces, transitions and packet records used, so an offline run
//                        reproduces the result (the folder sits under docs/evidence/ and is read back)
//   --check              regenerate offline and compare with the committed metrics.json; exit 1 on a difference
//
// Definitions (also printed into docs/metrics/README.md):
//   e2e latency     createdAt -> completedAt of an instance (or now, if active)
//   step closure    one trail entry; closureReason handoff|done = success, failed = artifact rejected
//   retry           a failed closure (QA / review / security sent the candidate back) or a step re-entered
//                   with a non-waiting exit; `waiting` re-presentations of the same step are waits, not retries
//   rollback        an executed rollback listed in docs/metrics/rollbacks.json with its raw evidence (every
//                   file must exist), plus reverts that landed on main; plans and descriptions do not count
//   recovery        engine resumes (resumeCount) and aborts, reported apart from rollbacks
//   MTTR            failed closure of step S -> next handoff/done closure of S (a `waiting` closure is not a recovery)
//   human wait      packet parked on human@kernel -> transition recorded by human@kernel
//   queue wait      packet created -> claimed; work time = claimed -> closed
// When the same record appears in several exports, the freshest copy wins (trace: instance.version;
// transitions: highest transitionId; packet: tsUpdated). On a tie the first copy read is kept; tied
// copies differ only in volatile fields (pickup/waiting) that no number uses, so the numbers do not
// depend on folder order.
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const argv = process.argv.slice(2);
const live = argv.includes("--live");
const root = execFileSync("git", ["rev-parse", "--show-toplevel"], { encoding: "utf8" }).trim();
const outArg = argv.includes("--out") ? argv[argv.indexOf("--out") + 1] : "docs/metrics";
const outDir = path.isAbsolute(outArg) ? outArg : path.join(root, outArg);
const saveArg = argv.includes("--save-inputs") ? argv[argv.indexOf("--save-inputs") + 1] : null;
const check = argv.includes("--check");
if (check && live) throw new Error("--check regenerates offline; drop --live");
// --check measures against the committed generation time, so a still-active instance or an
// unreleased park cannot make an identical regeneration differ.
const now = check ? Date.parse(JSON.parse(fs.readFileSync(path.join(outDir, "metrics.json"), "utf8")).totals.generatedAt) : Date.now();

function rig(...args) {
  const out = execFileSync("rig", [...args, "--json"], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  return JSON.parse(out);
}
const ts = (s) => (s ? Date.parse(s) : NaN);
const sec = (ms) => (Number.isFinite(ms) ? Math.round(ms / 1000) : null);
const readJson = (p) => JSON.parse(fs.readFileSync(p, "utf8"));
const glob = (dir, suffix) => (fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => f.endsWith(suffix)).map((f) => path.join(dir, f)) : []);

// ---- load instances + packets -------------------------------------------------
const instances = new Map(); // instanceId -> { trace, mission }
const packetTransitions = new Map(); // qitemId -> transitions[] (freshest copy)
const packetShows = new Map(); // qitemId -> show (freshest copy)
// Freshness of each record kind; a copy replaces the held one only when it is strictly fresher.
const traceAge = (t) => t?.instance?.version ?? -1; // the engine's monotonic instance version
const transitionsAge = (tr) => Math.max(-1, ...(tr ?? []).map((t) => t.transitionId ?? -1));
const showAge = (s) => ts(s?.tsUpdated) || -1;
const keepFresher = (map, key, value, age) => { if (!map.has(key) || age(value) > age(map.get(key))) map.set(key, value); };
const evidenceRoot = path.join(root, "docs", "evidence");
if (fs.existsSync(evidenceRoot)) {
  for (const mission of fs.readdirSync(evidenceRoot)) {
    for (const f of glob(path.join(evidenceRoot, mission, "instances"), ".trace.json")) {
      const trace = readJson(f);
      const id = trace?.instance?.instanceId;
      if (id && (!instances.has(id) || traceAge(trace) > traceAge(instances.get(id).trace))) instances.set(id, { trace, mission });
    }
    for (const f of glob(path.join(evidenceRoot, mission, "packets"), ".transitions.json")) keepFresher(packetTransitions, path.basename(f, ".transitions.json"), readJson(f), transitionsAge);
    for (const f of glob(path.join(evidenceRoot, mission, "packets"), ".show.json")) keepFresher(packetShows, path.basename(f, ".show.json"), readJson(f), showAge);
  }
}
if (live) {
  // The daemon is the freshest source: its records replace exported copies.
  for (const row of rig("workflow", "list")) {
    try { instances.set(row.instanceId, { trace: rig("workflow", "trace", row.instanceId), mission: "(live)" }); } catch { /* skip */ }
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
  if (live) { try { packetTransitions.set(id, rig("queue", "transitions", id)); } catch { /* keep the exported copy */ } }
  return packetTransitions.get(id) ?? [];
}
function loadShow(id) {
  if (live) { try { packetShows.set(id, rig("queue", "show", id, "--full")); } catch { /* keep the exported copy */ } }
  return packetShows.get(id) ?? null;
}

// Executed rollbacks: the committed register, every evidence file verified, plus reverts that landed on main.
const register = readJson(path.join(root, "docs", "metrics", "rollbacks.json"));
for (const e of register.events) for (const f of e.evidence) {
  if (!fs.existsSync(path.join(root, f))) throw new Error(`rollbacks.json: evidence missing for ${e.instanceId}: ${f}`);
}
for (const e of register.events) if (!instances.has(e.instanceId)) throw new Error(`rollbacks.json: unknown instance ${e.instanceId}`);
const rollbacksByInstance = register.events.reduce((m, e) => m.set(e.instanceId, (m.get(e.instanceId) ?? 0) + 1), new Map());
// Reverts that landed on main: commits reachable from main (a revert merged in from a branch counts),
// never the checked-out branch, so an unmerged revert cannot inflate the number.
const mainRev = ["main", "origin/main"].find((r) => {
  try { execFileSync("git", ["-C", root, "rev-parse", "--verify", "--quiet", `${r}^{commit}`], { stdio: "ignore" }); return true; } catch { return false; }
});
if (!mainRev) throw new Error("cannot count reverts on main: neither main nor origin/main exists (fetch main first)");
const revertsOnMain = execFileSync("git", ["-C", root, "log", mainRev, "--format=%s"], { encoding: "utf8" }).split("\n").filter((s) => s.startsWith('Revert "')).length;
const used = { traces: [], transitions: new Map(), shows: new Map() };

// ---- per-instance metrics ---------------------------------------------------------
const report = [];
for (const [id, inst] of instances) {
  const { instance, trail = [] } = inst.trace;
  const stepOf = new Map();
  for (const t of trail) if (t.priorQitemId) stepOf.set(t.priorQitemId, t.stepId);
  for (const f of inst.trace.frontier ?? []) if (f.packetId) stepOf.set(f.packetId, f.stepId);

  used.traces.push(inst.trace);
  const packets = [];
  for (const q of packetIds(inst)) {
    const raw = loadTransitions(q);
    const tr = [...raw].sort((a, b) => ts(a.ts) - ts(b.ts));
    if (!tr.length) continue;
    const show = loadShow(q);
    used.transitions.set(q, raw);
    if (show) used.shows.set(q, show);
    const step = stepOf.get(q) ?? (show?.tags ?? []).find((t) => t.startsWith("step:"))?.slice(5) ?? "?";
    const created = ts(tr[0].ts);
    const claimed = ts(tr.find((t) => t.state === "in-progress" && /claim/i.test(t.transitionNote ?? ""))?.ts);
    const closedRow = tr.find((t) => ["handed-off", "done", "failed", "canceled", "denied"].includes(t.state));
    const closed = ts(closedRow?.ts);
    let humanWait = 0;
    for (let i = 0; i < tr.length; i++) {
      const t = tr[i];
      // Only the engine's own gate park counts; an owner's note that merely mentions the gate (e.g. the integrator waiting on a parked slice) does not.
      const parkedOnHuman = t.state === "blocked" && (t.blockedOn === "human@kernel" || /^workflow gate: parked on human@kernel/.test(t.transitionNote ?? ""));
      if (!parkedOnHuman) continue;
      const release = tr.slice(i + 1).find((u) => u.actorSession === "human@kernel" || u.state !== "blocked");
      humanWait += (release ? ts(release.ts) : now) - ts(t.ts);
    }
    packets.push({ qitemId: q, step, queueWaitSec: sec(claimed - created), workSec: sec(closed - claimed), humanWaitSec: sec(humanWait), closedState: closedRow?.state ?? null });
  }

  const closures = trail.map((t) => ({ step: t.stepId, reason: t.closureReason, at: ts(t.closedAt) }));
  const failed = closures.filter((c) => c.reason === "failed");
  // A `waiting` closure re-presents the same step (e.g. the integrator waiting on a slice's proof); it is a wait, not a retry.
  const stepCounts = closures.filter((c) => c.reason !== "waiting").reduce((m, c) => m.set(c.step, (m.get(c.step) ?? 0) + 1), new Map());
  const reentries = [...stepCounts.values()].reduce((a, n) => a + Math.max(0, n - 1), 0);
  const mttrs = failed.map((f) => {
    // Recovery means the same step later passed: a handoff or done closure, never a wait.
    const fix = closures.find((c) => c.step === f.step && c.at > f.at && (c.reason === "handoff" || c.reason === "done"));
    return fix ? fix.at - f.at : null;
  }).filter((x) => x !== null);
  const rollbacks = rollbacksByInstance.get(id) ?? 0;
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
    repairsSec: mttrs.map(sec),
    humanWaitSec: packets.reduce((a, p) => a + (p.humanWaitSec ?? 0), 0),
    queueWaitSec: packets.reduce((a, p) => a + (p.queueWaitSec ?? 0), 0),
    workSec: packets.reduce((a, p) => a + (p.workSec ?? 0), 0),
    packets,
  });
}

report.sort((a, b) => (a.instanceId < b.instanceId ? -1 : 1)); // stable order whatever the load order

// ---- totals -------------------------------------------------------------------------
const terminal = report.filter((r) => ["completed", "failed", "aborted"].includes(r.status));
const completed = report.filter((r) => r.status === "completed");
const allClosures = report.reduce((a, r) => a + r.stepClosures, 0);
const allFailed = report.reduce((a, r) => a + r.failedClosures, 0);
const mttrAll = report.flatMap((r) => r.repairsSec); // every individual repair, not per-instance means
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
  rollbacks: register.events.length + revertsOnMain,
  rollbacksRehearsedOrDrilled: register.events.length,
  revertsOnMain,
  resumes: report.reduce((a, r) => a + r.resumeCount, 0),
  mttrMeanSec: mttrAll.length ? Math.round(mttrAll.reduce((a, b) => a + b, 0) / mttrAll.length) : null,
  e2eP50Sec: pct(completed.map((r) => r.e2eLatencySec), 0.5),
  e2eP95Sec: pct(completed.map((r) => r.e2eLatencySec), 0.95),
  // A gate packet can appear in several instances' trails (a slice gate is also a blocker of the mission's wave_integration); count each packet once.
  humanWaitTotalSec: [...new Map(report.flatMap((r) => r.packets.map((p) => [p.qitemId, p.humanWaitSec ?? 0]))).values()].reduce((a, b) => a + b, 0),
};

// ---- check: an offline run must reproduce the committed numbers ---------------------
if (check) {
  const committed = readJson(path.join(outDir, "metrics.json"));
  // Only the generation time and the source label may differ between a live and an offline run.
  const norm = (m) => JSON.stringify({ totals: { ...m.totals, generatedAt: null }, instances: m.instances.map((r) => ({ ...r, mission: null })) });
  if (norm(committed) === norm({ totals, instances: report })) { console.log(`metrics check: offline regeneration matches ${path.relative(root, outDir)}/metrics.json`); process.exit(0); }
  const diff = Object.keys(totals).filter((k) => k !== "generatedAt" && JSON.stringify(totals[k]) !== JSON.stringify(committed.totals[k]));
  console.error(`metrics check FAILED: totals differ in ${diff.join(", ") || "per-instance rows"}`);
  process.exit(1);
}

// ---- write -------------------------------------------------------------------------
if (saveArg) {
  const dir = path.isAbsolute(saveArg) ? saveArg : path.join(root, saveArg);
  fs.mkdirSync(path.join(dir, "instances"), { recursive: true });
  fs.mkdirSync(path.join(dir, "packets"), { recursive: true });
  for (const t of used.traces) fs.writeFileSync(path.join(dir, "instances", `${t.instance.instanceId}.trace.json`), JSON.stringify(t, null, 2) + "\n");
  for (const [q, tr] of used.transitions) fs.writeFileSync(path.join(dir, "packets", `${q}.transitions.json`), JSON.stringify(tr, null, 2) + "\n");
  for (const [q, s] of used.shows) fs.writeFileSync(path.join(dir, "packets", `${q}.show.json`), JSON.stringify(s, null, 2) + "\n");
  console.log(`inputs: ${used.traces.length} traces, ${used.transitions.size} packets → ${path.relative(root, dir)}`);
}
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
| Rollbacks executed (rehearsals and drills with raw evidence + reverts on \`main\`) | ${totals.rollbacks} (${totals.rollbacksRehearsedOrDrilled} + ${totals.revertsOnMain}) |
| Engine recoveries (resumes / aborted instances) | ${totals.resumes} / ${totals.aborted} |
| MTTR, mean over every repair (failed closure → next handoff or done closure of that step) | ${fmt(totals.mttrMeanSec)} (${mttrAll.length} repairs) |
| End-to-end latency, completed instances p50 / p95 | ${fmt(totals.e2eP50Sec)} / ${fmt(totals.e2eP95Sec)} |
| Time parked on the human (all gates) | ${fmt(totals.humanWaitTotalSec)} |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
${rows}

## Derivations and honest limits

- **Source of truth**: \`rig workflow trace --json\` (append-only step trail; one entry per closed packet with \`closureReason\` handoff/done/failed) and \`rig queue transitions --json\` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Drills are included**: DRILL 1's deliberate QA rejection and DRILL 4's deliberately failed step count as failed closures and retries, and DRILL 4's abort as the one aborted instance.
- **Retry** counts an artifact verdict of \`failed\` (QA, code review or security review sent the candidate back) plus any step closed more than once with a non-\`waiting\` exit. A \`waiting\` closure re-presents the same step (the integrator waiting on a slice's proof) and is not counted. One remediation round therefore shows as one failed closure plus two re-entries (the checking step and the building step both run again). A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts only rollbacks that were executed: a merged change reverted, or a migration rolled back with the earlier binary started on the rolled-back data, each checked by the gate or an installed smoke. They are listed in [\`rollbacks.json\`](rollbacks.json) with their raw evidence, and the generator refuses to run if a listed file is missing; reverts that landed on \`main\` are counted from \`git log\`. Plans and descriptions of rollbacks do not count, and engine resumes and aborts are reported separately as recoveries. OpenRig 0.6.3 has no rollback event of its own, so the register is the record; every entry points at raw files.
- **MTTR** is measured from a failed closure to the next \`handoff\` or \`done\` closure of the same step, i.e. the time to repair the candidate and pass that check again; a \`waiting\` closure is not a recovery. It measures repair of a rejected candidate, not production incident recovery: no production incident occurred. The total is the mean over every individual repair; the per-instance column is the mean of that instance's repairs. Instances with no failure have no MTTR (shown as –), not zero.
- **Reproducibility**: when the same record appears in several exports, the freshest copy wins (trace \`instance.version\`, highest \`transitionId\`, packet \`tsUpdated\`). \`docs/evidence/run-end/\` holds the exact inputs of this report, and \`node tools/sdlc-metrics.mjs --check\` regenerates it offline and fails on any difference.
- **Human wait** is time a packet spent parked on \`human@kernel\`; it is reported separately so agent throughput and human latency are not conflated. The per-instance column sums the gate packets in that instance's trail; the total counts each packet once, because a slice gate also appears in the mission trail as the blocker of \`wave_integration\`.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: \`rig usage top --json\` in \`docs/evidence/<mission>/usage-top.json\`.
- Notes on earlier snapshots, generated under the previous definitions, are kept in [\`HISTORY.md\`](HISTORY.md).
`;
fs.writeFileSync(path.join(outDir, "README.md"), md);
console.log(`metrics: ${report.length} instance(s) → ${path.relative(root, path.join(outDir, "metrics.json"))}, ${path.relative(root, path.join(outDir, "README.md"))}`);
console.log(JSON.stringify(totals));
