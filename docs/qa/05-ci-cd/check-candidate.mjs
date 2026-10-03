// Independent, offline QA evidence checker. Does not modify the candidate.
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';
const [wt, pinsPath] = process.argv.slice(2);
const { parseDocument } = createRequire(import.meta.url)('/Users/andrzej/.nvm/versions/node/v24.18.0/lib/node_modules/@openrig/cli/node_modules/yaml');
const candidate = 'add7ab5ca37dcd6f51aef3cd43c85455e1be6d14';
const git = (...args) => execFileSync('git', ['-C', wt, ...args], { encoding: 'utf8' }).trim();
assert.equal(git('rev-parse', 'HEAD'), candidate);
assert.equal(git('status', '--porcelain'), '');
console.log(`Candidate ${candidate}; clean product worktree; YAML 1.2 parser`);
const files = ['.github/workflows/ci.yml', '.github/workflows/cd.yml', '.github/dependabot.yml'];
const texts = files.map(f => readFileSync(`${wt}/${f}`, 'utf8'));
const docs = texts.map((t, i) => {
  const d = parseDocument(t, { version: '1.2' });
  assert.deepEqual([...d.errors, ...d.warnings], [], files[i]);
  console.log(`PARSE PASS ${files[i]}`);
  return d.toJS();
});
const [ci, cd, dep] = docs;
const gate = ci.jobs.gate, pkg = cd.jobs.package;
const ac = (n, check, evidence) => { check(); console.log(`AC-${n} PASS ${evidence}`); };
const actions = j => j.steps.filter(s => s.uses);
const byAction = (j, prefix) => actions(j).filter(s => s.uses.startsWith(prefix));
ac(1, () => {
  assert.deepEqual(Object.keys(ci.on).sort(), ['pull_request','push','workflow_dispatch']);
  assert.equal(ci.on.pull_request, null);
  assert.deepEqual(ci.on.push, { branches: ['main'] });
}, 'ci.yml:5-11; unfiltered PR event, main push, dispatch');
ac(2, () => {
  assert.equal(gate.name ?? 'gate', 'gate');
  const co = byAction(gate, 'actions/checkout@')[0]; assert(co);
  assert.equal(co.with['persist-credentials'], false);
  const java = byAction(gate, 'actions/setup-java@')[0];
  assert.equal(java.with.distribution, 'temurin'); assert.equal(java.with['java-version'], '21');
  assert(gate.steps.some(s => s.run === './gradlew check --no-daemon'));
  for (const job of Object.values(ci.jobs)) {
    assert(!Object.hasOwn(job, 'continue-on-error'));
    for (const s of job.steps) {
      assert(!Object.hasOwn(s, 'continue-on-error'));
      if (s.run) assert(!/scripts\/gw|(?:^|\s)-x(?:\s|$)|\|\|\s*true|set\s+\+e|exit\s+0/.test(s.run));
    }
  }
}, 'ci.yml:27-43; checkout, Temurin 21, own wrapper check; no skips or tolerated failures');
ac(3, () => {
  const r = byAction(gate, 'actions/upload-artifact@')[0];
  assert(gate.steps.indexOf(r) > gate.steps.findIndex(s => s.run?.includes('gradlew check')));
  assert.equal(r.if, 'always()'); assert(r.with['retention-days'] >= 30);
  for (const p of ['build/reports/','build/test-results/','build/docs/javadoc/']) assert(r.with.path.includes(p));
}, 'ci.yml:44-53; reports after check, always(), 30 days');
ac(4, () => {
  assert.deepEqual(Object.keys(cd.on).sort(), ['push','workflow_dispatch']);
  assert.deepEqual(cd.on.push, { branches: ['main'] });
}, 'cd.yml:6-9; main push and dispatch only');
ac(5, () => {
  assert.equal(Object.keys(cd.jobs).length, 1);
  assert(pkg.steps.some(s => s.run === './gradlew bootJar --no-daemon'));
  assert(pkg.steps.some(s => s.run === 'docker build --tag urlshort:cd .'));
  const smoke = pkg.steps.find(s => s.run?.includes('scripts/smoke.sh'));
  assert(smoke.run.includes('scripts/smoke.sh --jar build/libs/urlshort.jar 18091 build/smoke/jar.log'));
  assert.equal(smoke.env.URLSHORT_JAVA_HOME, '${{ env.JAVA_HOME }}');
  assert.equal(cd.defaults.run.shell, 'bash');
  const uploads = byAction(pkg, 'actions/upload-artifact@');
  assert.equal(uploads.length, 2);
  for (const u of uploads) assert(u.with['retention-days'] >= 30);
  assert(uploads.some(s => s.with.path === 'build/libs/urlshort.jar'));
  assert(uploads.some(s => s.with.path === 'build/smoke/' && s.if === 'always()'));
  const source = readFileSync(`${wt}/scripts/smoke.sh`, 'utf8');
  const jarMode = source.slice(source.indexOf('jar_mode()'), source.indexOf('# ---------------------------------------------------------------- --inspect'));
  assert(jarMode.includes('--server.address=127.0.0.1'));
  assert(jarMode.includes('smoke "$BASE"')); assert(jarMode.includes('kill -TERM "$pid"'));
  assert(jarMode.includes('wait "$pid"')); assert(jarMode.includes('Graceful shutdown complete'));
}, 'cd.yml:24-64 plus unchanged scripts/smoke.sh jar_mode; own jar smoke separately captured');
ac(6, () => {
  for (const w of [ci, cd]) {
    const grants = [w.permissions, ...Object.values(w.jobs).map(j => j.permissions).filter(Boolean)];
    for (const p of grants) for (const [k,v] of Object.entries(p)) assert(v !== 'write', `${k}: ${v}`);
    assert(!JSON.stringify(w).includes('secrets.'));
    for (const j of Object.values(w.jobs)) for (const s of j.steps) {
      if (s.run) assert(!/docker\s+(?:login|push)|git\s+push|gh\s+release|publish|deploy/.test(s.run));
      if (s.uses) assert(/^(actions\/(?:checkout|setup-java|upload-artifact)|gradle\/actions\/setup-gradle)@/.test(s.uses));
    }
  }
}, 'all steps and grants in both parsed workflows; no publishing, deployment, secret or write grant');
ac(7, () => {
  for (const w of [ci, cd]) {
    assert.deepEqual(w.permissions, { contents: 'read' });
    for (const j of Object.values(w.jobs)) assert(!j.permissions);
  }
}, 'ci.yml:13-14; cd.yml:11-12; no job-level grants');
ac(8, () => {
  for (const w of [ci, cd]) for (const j of Object.values(w.jobs)) for (const s of j.steps) {
    if (s.run) assert(!/\$\{\{\s*github\.(?:event\.|head_ref)/.test(s.run));
  }
}, 'ci.yml:43; cd.yml:41,54-56,64; all run blocks checked');
ac(9, () => {
  const capture = readFileSync(pinsPath, 'utf8');
  const expected = new Map();
  const blocks = capture.split('$ git ls-remote --tags https://github.com/').slice(1);
  for (const block of blocks) {
    const repo = block.match(/^([^\s]+)/)[1];
    for (const m of block.matchAll(/^([a-f0-9]{40})\s+refs\/tags\/(v[\d.]+)(\^\{\})?$/gm)) {
      const key = `${repo}#${m[2]}`;
      if (!expected.has(key) || m[3]) expected.set(key, m[1]);
    }
  }
  let count = 0;
  for (let f = 0; f < 2; f++) for (const line of texts[f].split('\n')) {
    if (!/^\s*(?:-\s*)?uses:/.test(line)) continue;
    const m = line.match(/uses:\s+([^@]+)@([a-f0-9]{40})\s+#\s+(v[\d.]+)/);
    assert(m, line);
    const repo = m[1].split('/').slice(0,2).join('/');
    assert.equal(m[2], expected.get(`${repo}#${m[3]}`), line); count++;
  }
  assert.equal(count, 9); assert.equal(expected.size, 4);
}, '9 uses across 4 actions match builder 19:34Z tag capture; annotated gradle/actions uses peeled commit');
ac(10, () => {
  for (const w of [ci, cd]) {
    assert.equal(w.concurrency.group, '${{ github.workflow }}-${{ github.ref }}');
    assert.equal(w.concurrency['cancel-in-progress'], true);
    for (const j of Object.values(w.jobs)) assert(j['timeout-minutes'] > 0);
  }
}, 'ci.yml:16-18,29; cd.yml:14-16,27; both 20 minutes');
ac(11, () => {
  assert.equal(dep.version, 2); assert.equal(dep.updates.length, 2);
  assert.deepEqual(dep.updates.map(u => u['package-ecosystem']).sort(), ['github-actions','gradle']);
  for (const u of dep.updates) { assert.equal(u.directory, '/'); assert.equal(u.schedule.interval, 'weekly'); }
}, 'dependabot.yml:3-12; version 2, root actions and Gradle weekly');
console.log('AC-12 PARSE/DIFF PASS; build, smoke, coverage and builder image capture are separate evidence');
console.log('AC-13 PENDING human push; no GitHub run claimed');
const base = git('rev-parse', '06818ca');
const changed = git('diff','--name-only',base,candidate).split('\n');
assert.deepEqual(changed, ['.github/dependabot.yml','.github/workflows/cd.yml','.github/workflows/ci.yml']);
console.log(`Base ${base}\n${git('diff','--stat',base,candidate)}`);
console.log('ALL FILE CHECKS PASS (AC-1 through AC-11)');
