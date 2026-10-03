"""Exercise the handed-off load_loop with one real request per controlled peer."""
import json
import socketserver
import subprocess
import sys
import tempfile
import threading
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TARGET = sys.argv[1] if len(sys.argv) > 1 else 'ad83fb6'
records = {'candidate': TARGET, 'cases': {}}
source = subprocess.check_output(['git', 'show', TARGET + ':scripts/smoke.sh'], cwd=ROOT, text=True)
function = source[source.index('load_loop() {'):source.index('\ncheck_link() {')]

class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        self.request.recv(8192)
        self.server.requests += 1
        if self.server.mode == 'empty':
            return
        if self.server.mode == 'truncated':
            self.request.sendall(b'HTTP/1.1 200 OK\r\nContent-Length: 20\r\nConnection: close\r\n\r\nx')
        else:
            self.request.sendall(b'HTTP/1.1 302 Found\r\nLocation: https://example.com/\r\nContent-Length: 0\r\nConnection: close\r\n\r\n')

for mode in ['empty', 'complete', 'truncated']:
    with tempfile.TemporaryDirectory(prefix='release-count-probe-') as tmp, socketserver.ThreadingTCPServer(('127.0.0.1', 0), Handler) as server:
        server.mode = mode
        server.requests = 0
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
        records['cases'][mode] = {'requests': server.requests, 'exit': result.returncode,
                                 'statusRows': output.read_text().splitlines(), 'stderr': result.stderr}
        server.shutdown()
        thread.join()
OUT = ROOT / ('docs/review/01-greenfield-core/release-count-controls-' + TARGET + '.json')
OUT.write_text(json.dumps(records, indent=2) + '\n')
print(json.dumps(records, indent=2))
