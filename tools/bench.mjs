#!/usr/bin/env node
// Open-loop load for `scripts/smoke.sh --bench` (AC-27; NFR-L1, L2, L3).
//   node tools/bench.mjs <loopback-base-url> <seconds> '<scenarios json>'
//   scenarios: [{"label":"redirect GET","method":"GET","path":"/abc123","rate":100,"body":"{...}"?}, ...]
// Every scenario runs at once. Request i of a scenario is due at t0 + i/rate whatever the earlier requests
// are doing, so a slow response delays no later start and the offered rate is the stated one. Latency runs
// from the request's due time to the end of its response, so time a request spends waiting for a late
// timer or a free connection counts against the service, not for it (no coordinated omission).
import http from 'node:http';

const [base, seconds, scenariosJson] = process.argv.slice(2);
const url = new URL(base ?? '');
if (!['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname)) {
	console.error(`bench: refusing non-loopback base URL: ${base}`);
	process.exit(2);
}
const secs = Number(seconds);
const agent = new http.Agent({ keepAlive: true, maxSockets: 64 });

function run({ label, method, path, rate, body }) {
	const total = Math.round(rate * secs);
	const results = []; // [status, ms]
	const t0 = performance.now();
	let sent = 0;
	return new Promise((resolve) => {
		const fire = (due) => {
			const req = http.request(new URL(path, url), { method, agent, headers: body ? { 'Content-Type': 'application/json' } : {} }, (res) => {
				res.resume();
				res.on('end', () => done(res.statusCode, due));
			});
			req.on('error', () => done(0, due));
			req.end(body);
		};
		const done = (status, due) => {
			results.push([status, performance.now() - due]);
			if (results.length === total) resolve(report(label, rate, results, performance.now() - t0));
		};
		const tick = setInterval(() => {
			const now = performance.now();
			while (sent < total && t0 + (sent * 1000) / rate <= now) fire(t0 + (sent++ * 1000) / rate);
			if (sent === total) clearInterval(tick);
		}, 1);
	});
}

function report(label, rate, results, elapsed) {
	const ms = results.map(([, t]) => t).sort((a, b) => a - b);
	const at = (q) => ms[Math.ceil(ms.length * q) - 1].toFixed(1);
	const bad = results.filter(([s]) => s < 200 || s >= 400).length;
	const line = `${label}: ${ms.length} requests in ${Math.round(elapsed)} ms (offered ${rate} req/s for ${secs} s), ` +
		`achieved ${((ms.length * 1000) / elapsed).toFixed(1)} req/s, non-2xx/3xx ${bad}, ` +
		`p50 ${at(0.5)} ms, p95 ${at(0.95)} ms, p99 ${at(0.99)} ms`;
	console.log(line);
	return { label, p95: Number(at(0.95)) };
}

const outcomes = await Promise.all(JSON.parse(scenariosJson).map(run));
agent.destroy();
if (outcomes.some((o) => Number.isNaN(o.p95))) process.exit(1);
