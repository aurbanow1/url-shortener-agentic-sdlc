"""Exercise the candidate's unmodified r0 functions and final status predicate.

The loopback peer deliberately sends a 201 header with a promised body, then EOF.
This checks the smoke instrument, not the application's normal response behavior.
"""
from pathlib import Path
import socket
import subprocess
import threading

repo = Path(__file__).resolve().parents[4]
candidate = repo / '.worktrees/03-operate/scripts/smoke.sh'
source = candidate.read_text().split('# ---------------------------------------------------------------- --restart (compose)')[0]
listener = socket.socket()
listener.bind(('127.0.0.1', 0))
listener.listen(1)
port = listener.getsockname()[1]
errors = []

def serve():
    try:
        conn, _ = listener.accept()
        with conn:
            conn.settimeout(5)
            request = b''
            while b'\r\n\r\n' not in request:
                request += conn.recv(4096)
            head, body = request.split(b'\r\n\r\n', 1)
            size = int(next(line.split(b':', 1)[1] for line in head.split(b'\r\n')
                            if line.lower().startswith(b'content-length:')))
            while len(body) < size:
                body += conn.recv(4096)
            conn.sendall(b'HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n'
                         b'Content-Length: 100\r\nX-Request-Id: r0-truncated-control\r\n'
                         b'Connection: close\r\n\r\n')
            # No promised body bytes are sent.
    except Exception as error:
        errors.append(str(error))
    finally:
        listener.close()

thread = threading.Thread(target=serve, daemon=True)
thread.start()
harness = source + f'\nr0_open {port}\nr0="$(r0_finish)"\n' + '''
echo "candidate r0_finish returned: $r0"
echo "captured R0:"
cat "$WORK/r0.txt"
# Exact final R0 status predicate from drain_mode:
echo "$r0" | grep -q '^HTTP/1\\.1 2' || fail "held request R0 answered '$r0', expected 2xx"
echo 'R0 status gate accepted the truncated response'
# drain_mode also credits this header as a completely received request:
grep -i '^x-request-id:' "$WORK/r0.txt" | tr -d '\\r' | awk '{print "credited client id: " $2}'
'''
result = subprocess.run(['bash', '-c', harness, str(candidate)], text=True, capture_output=True, timeout=15)
thread.join(timeout=2)
if thread.is_alive() or errors:
    raise RuntimeError(f'control failed: {errors}')
output = ('candidate=a7c533ffef55650e5b422377ffe0c4e38d41400c\n'
          'peer advertised Content-Length=100; actual body bytes=0\n'
          + result.stdout + result.stderr + f'R0_GATE_EXIT={result.returncode} (expected rejection)\n')
(Path(__file__).parent / 'smoke-r0-boundary-a7c533f.txt').write_text(output)
print(output)
