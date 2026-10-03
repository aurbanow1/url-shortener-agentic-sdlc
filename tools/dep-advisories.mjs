#!/usr/bin/env node
// Query OSV (https://osv.dev) for known advisories against the resolved runtime
// classpath. Input: the text output of `scripts/gw dependencies --configuration
// runtimeClasspath` (saved with --log). Output: one JSON file holding the raw
// OSV querybatch response plus a flat findings list, so a reader can see exactly
// what the database returned on the day. Needs network; run from a seat that has it.
//   node tools/dep-advisories.mjs <gradle-dependencies-log> <out.json>
import fs from "node:fs";

const [input, outJson] = process.argv.slice(2);
if (!input || !outJson) {
  console.error("usage: node tools/dep-advisories.mjs <dependencies-log> <out.json>");
  process.exit(2);
}

// Tree lines look like `+--- org.x:y:1.0 -> 1.2 (*)` or `\--- org.x:y -> 1.2 (c)`;
// the version after `->` is the resolved one, `(*)`/`(c)` markers are ignored.
const coords = new Map();
for (const line of fs.readFileSync(input, "utf8").split("\n")) {
  const m = line.match(/[+\\]--- ([\w.-]+):([\w.-]+)(?::(\S+))?(?: -> (\S+))?/);
  if (!m) continue;
  const version = m[4] ?? m[3];
  if (version) coords.set(`${m[1]}:${m[2]}`, version);
}
const queries = [...coords].map(([name, version]) => ({ package: { ecosystem: "Maven", name }, version }));

const res = await fetch("https://api.osv.dev/v1/querybatch", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ queries }),
});
if (!res.ok) {
  console.error(`OSV querybatch failed: ${res.status} ${res.statusText}`);
  process.exit(1);
}
const raw = await res.json();
const findings = [];
(raw.results ?? []).forEach((r, i) => {
  for (const v of r.vulns ?? []) findings.push({ dependency: `${queries[i].package.name}:${queries[i].version}`, id: v.id, modified: v.modified });
});

// One detail fetch per distinct advisory: summary, severity and the first fixed
// version for the affected Maven package, so the release record can say what
// each hit is and what would close it.
const details = {};
for (const id of new Set(findings.map((f) => f.id))) {
  const d = await (await fetch(`https://api.osv.dev/v1/vulns/${id}`)).json();
  const fixed = {};
  for (const a of d.affected ?? []) {
    if (a.package?.ecosystem !== "Maven") continue;
    for (const r of a.ranges ?? []) for (const e of r.events ?? []) if (e.fixed) (fixed[a.package.name] ??= []).push(e.fixed);
  }
  const cvss = (d.severity ?? []).map((s) => s.score).join(" ");
  details[id] = { summary: d.summary ?? "", aliases: d.aliases ?? [], severity: d.database_specific?.severity ?? cvss, cvss, fixed, published: d.published };
}
for (const f of findings) Object.assign(f, { summary: details[f.id].summary, severity: details[f.id].severity, fixed: details[f.id].fixed[f.dependency.split(":").slice(0, 2).join(":")] ?? [] });

const report = { queriedAt: new Date().toISOString(), source: input, dependencies: queries.map((q) => `${q.package.name}:${q.version}`), findings, details, raw };
fs.writeFileSync(outJson, JSON.stringify(report, null, 2) + "\n");
console.log(`${queries.length} runtime dependencies queried against OSV at ${report.queriedAt}; ${findings.length} advisories`);
for (const f of findings) console.log(`  ${f.dependency}  ${f.id}  ${f.severity}  fixed: ${f.fixed.join(", ") || "?"}  ${f.summary}`);
