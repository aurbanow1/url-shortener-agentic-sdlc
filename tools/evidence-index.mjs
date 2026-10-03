#!/usr/bin/env node
// Tabulate an evidence export for INDEX.md: packets (step · state · owner) and
// each instance's step trail. Read-only; prints Markdown to stdout.
// Usage: node tools/evidence-index.mjs docs/evidence/<mission>
import fs from "node:fs";
import path from "node:path";

const out = process.argv[2];
if (!out || !fs.existsSync(out)) {
  console.error("usage: node tools/evidence-index.mjs docs/evidence/<mission>");
  process.exit(2);
}
const load = (f) => {
  try {
    const j = JSON.parse(fs.readFileSync(f, "utf8"));
    return Array.isArray(j) ? j[0] : j;
  } catch {
    return null;
  }
};
const seat = (s) => String(s || "-").replace("@urlshort-factory", "");
const list = (dir, suffix) =>
  fs.existsSync(dir) ? fs.readdirSync(dir).filter((n) => n.endsWith(suffix)).sort() : [];

// step per packet: from the trails first (priorQitemId → stepId), then from the packet body
const stepOf = new Map();
for (const f of list(path.join(out, "instances"), ".trace.json")) {
  const t = load(path.join(out, "instances", f));
  const trail = (t && (t.trail || (t.instance && t.instance.trail))) || [];
  for (const e of trail) {
    for (const id of [e.priorQitemId, e.qitemId, e.packetId]) if (id && e.stepId) stepOf.set(id, e.stepId);
  }
}

console.log("\n## Packets (workflow · step · state · owner)\n");
console.log("| Packet | Workflow | Step | State | Owner |");
console.log("|---|---|---|---|---|");
for (const f of list(path.join(out, "packets"), ".show.json")) {
  const q = load(path.join(out, "packets", f));
  if (!q) continue;
  const it = q.item || q.qitem || q;
  const tags = it.tags || [];
  const id = it.qitemId || f.replace(".show.json", "");
  const wf = (tags.find((x) => /^workflow:/.test(x)) || "").replace("workflow:", "").replace("lifecycle-urlshort-", "") || "-";
  const bodyStep = (String(it.body || "").match(/\bstep ([A-Za-z0-9_-]+)/) || [])[1];
  const step =
    (tags.find((x) => /^step:/.test(x)) || "").replace("step:", "") || stepOf.get(id) || bodyStep || (tags.includes("gate") ? "gate" : "-");
  console.log(`| ${id} | ${wf} | ${step} | ${it.state || "-"} | ${seat(it.destinationSession)} |`);
}

console.log("\n## Step trails (closed at · step · exit · packet · actor)\n");
for (const f of list(path.join(out, "instances"), ".trace.json")) {
  const t = load(path.join(out, "instances", f));
  const trail = (t && (t.trail || (t.instance && t.instance.trail))) || [];
  console.log(`### ${f.replace(".trace.json", "")}\n`);
  console.log("| Closed at | Step | Exit | Packet | Actor |");
  console.log("|---|---|---|---|---|");
  for (const e of trail) {
    console.log(
      `| ${e.closedAt || e.at || "-"} | ${e.stepId || e.step || "-"} | ${e.closureReason || e.exit || "-"} | ${e.priorQitemId || e.qitemId || e.packetId || "-"} | ${seat(e.actorSession)} |`,
    );
  }
  console.log("");
}
