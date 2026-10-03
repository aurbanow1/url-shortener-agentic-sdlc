"""Independent controls for the exact release smoke and benchmark tools; localhost only."""
import json
import socketserver
import subprocess
import tempfile
import threading
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'docs/review/01-greenfield-core/release-review-tool-controls.json'
TARGET = '40067fc'
records = {}
bench_dir = tempfile.TemporaryDirectory(prefix='release-review-bench-')
bench = Path(bench_dir.name) / 'bench.mjs'
bench.write_bytes(subprocess.check_output(['git', 'show', TARGET + ':tools/bench.mjs'], cwd=ROOT))


class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        self.request.recv(8192)
        if self.server.mode == 'empty':
            return
        elif self.server.mode == 'truncated':
            self.request.sendall(b'HTTP/1.1 200 OK\r\nContent-Length: 20\r\nConnection: close\r\n\r\nx')
        else:
            status = b'500 Internal Server Error' if self.server.mode == '500' else b'200 OK'
            self.request.sendall(b'HTTP/1.1 ' + status + b'\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok')


for mode in ['200', '500', 'truncated']:
    with socketserver.ThreadingTCPServer(('127.0.0.1', 0), Handler) as server:
        server.mode = mode
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        cmd = ['node', str(bench),
               'http://127.0.0.1:' + str(server.server_address[1]), '0.1',
               json.dumps([{'label': mode, 'method': 'GET', 'path': '/', 'rate': 20}])]
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=3)
            records[mode] = {'exit': result.returncode, 'stdout': result.stdout, 'stderr': result.stderr}
        except subprocess.TimeoutExpired as error:
            records[mode] = {'timeoutSeconds': 3, 'stdout': (error.stdout or b'').decode(),
                             'stderr': (error.stderr or b'').decode()}
        finally:
            server.shutdown()
            thread.join()
    OUT.write_text(json.dumps(records, indent=2) + '\n')

# Run the actual load_loop once against a local peer that closes without replying.
# Replacing its sleep with removal of its sentinel ends exactly one iteration.
source = subprocess.check_output(['git', 'show', TARGET + ':scripts/smoke.sh'], cwd=ROOT, text=True)
function = source[source.index('load_loop() {'):source.index('\ncheck_link() {')]
with tempfile.TemporaryDirectory(prefix='release-review-load-') as tmp, socketserver.ThreadingTCPServer(('127.0.0.1', 0), Handler) as server:
    server.mode = 'empty'
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    work = Path(tmp)
    stop = work / 'stop'
    stop.touch()
    output = work / 'statuses'
    runner = work / 'one-loop.sh'
    runner.write_text('set -euo pipefail\nHTTP="$1"\nSTOP="$4"\n' + function +
                      '\nsleep() { rm "$STOP"; }\nload_loop "$2" probe "$3" "$4"\n')
    result = subprocess.run(['bash', str(runner), str(ROOT / 'scripts/http'),
                             'http://127.0.0.1:' + str(server.server_address[1]), str(output), str(stop)],
                            capture_output=True, text=True, timeout=5)
    records['oneFailedLoadAttempt'] = {'exit': result.returncode,
                                        'statusRows': output.read_text().splitlines(),
                                        'stderr': result.stderr}
    server.shutdown()
    thread.join()
OUT.write_text(json.dumps(records, indent=2) + '\n')
bench_dir.cleanup()
print(json.dumps(records, indent=2))
