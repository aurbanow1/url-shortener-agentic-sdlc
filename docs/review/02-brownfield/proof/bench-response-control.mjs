// Reviewer control: the actual pinned benchmark must not print a result for a cut response.
import http from 'node:http';
import {spawn} from 'node:child_process';
import {resolve} from 'node:path';
const target = resolve('.worktrees/review-wave02-ed2b940/tools/bench.mjs');
for (const mode of ['complete', 'truncated']) {
  const server = http.createServer((req, res) => {
    if (mode === 'complete') { res.writeHead(302, {'Content-Length': '0'}); res.end(); }
    else { res.writeHead(200, {'Content-Length': '20'}); res.write('cut'); setTimeout(() => res.destroy(), 20); }
  });
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const child = spawn(process.execPath, [target, `http://127.0.0.1:${server.address().port}`, '1', JSON.stringify([{label:mode, method:'GET',path:'/',rate:1}])]);
  let stdout = '', stderr = '', timedOut = false;
  child.stdout.on('data', c => stdout += c);
  child.stderr.on('data', c => stderr += c);
  const timer = setTimeout(() => { timedOut = true; child.kill('SIGTERM'); }, 5000);
  const result = await new Promise(r => child.on('close', (code, signal) => r({code, signal})));
  clearTimeout(timer); server.closeAllConnections(); await new Promise(r => server.close(r));
  console.log(JSON.stringify({mode,...result,timedOut,stdout:stdout.trim(),stderr:stderr.trim()}));
  if ((mode === 'complete' && result.code !== 0) || (mode === 'truncated' && (result.code === 0 || timedOut))) process.exitCode = 1;
}
