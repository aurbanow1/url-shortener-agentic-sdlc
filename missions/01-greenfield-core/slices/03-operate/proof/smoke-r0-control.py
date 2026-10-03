"""Builder control for the smoke instrument's held request R0 (code review CR-02).

Runs the candidate's own r0_open / r0_finish (the script text before the --restart section, the same
splice the review probe used) against one loopback peer per case, and prints the instrument's verdict.
Only a complete 2xx/3xx no later than 10 s after the stop may pass; every other case must be rejected.
Usage, from the repo root: python3 missions/01-greenfield-core/slices/03-operate/proof/smoke-r0-control.py
"""
from pathlib import Path
import socket
import subprocess
import sys
import threading
import time

repo = Path(__file__).resolve().parents[5]
candidate = repo / '.worktrees/03-operate/scripts/smoke.sh'
source = candidate.read_text().split('# ---------------------------------------------------------------- --restart (compose)')[0]

BODY = b'{"code":"r0ctl","url":"https://example.com/held-r0"}'
CASES = [  # name, response bytes, delay before answering (s), expected verdict
    ('complete 201', b'HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: %d\r\n'
     b'X-Request-Id: r0-complete\r\nConnection: close\r\n\r\n' % len(BODY) + BODY, 0, 'pass'),
    ('201 header, 100-byte body promised, 0 sent', b'HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n'
     b'Content-Length: 100\r\nX-Request-Id: r0-truncated\r\nConnection: close\r\n\r\n', 0, 'reject'),
    ('201 header, 100-byte body promised, 10 sent', b'HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n'
     b'Content-Length: 100\r\nX-Request-Id: r0-short\r\nConnection: close\r\n\r\n' + BODY[:10], 0, 'reject'),
    ('connection closed without a response', b'', 0, 'reject'),
    ('complete 500', b'HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n', 0, 'reject'),
    ('complete 201 sent 11 s after the stop', b'HTTP/1.1 201 Created\r\nContent-Length: %d\r\n'
     b'Connection: close\r\n\r\n' % len(BODY) + BODY, 11, 'reject'),
]


def serve(listener, response, delay, seen):
    conn, _ = listener.accept()
    with conn:
        conn.settimeout(20)
        data = b''
        while b'\r\n\r\n' not in data:
            data += conn.recv(4096)
        seen['headers_at'] = time.monotonic()
        seen['head'] = data.split(b'\r\n\r\n', 1)[0]
        while not data.endswith(b'0\r\n\r\n'):  # chunked body: wait for the terminating chunk
            data += conn.recv(4096)
        seen['body_done_at'] = time.monotonic()
        time.sleep(delay)
        conn.sendall(response)
    listener.close()


failures = 0
for name, response, delay, expected in CASES:
    listener = socket.socket()
    listener.bind(('127.0.0.1', 0))
    listener.listen(1)
    port = listener.getsockname()[1]
    seen = {}
    thread = threading.Thread(target=serve, args=(listener, response, delay, seen), daemon=True)
    thread.start()
    harness = source + f'\nr0_open http://127.0.0.1:{port}\nsleep 0.5\nstarted="$(now_ms)"\nr0_finish "$started"\n' \
        + 'echo "R0_OK=$R0_OK R0_RC=$R0_RC R0=$R0"\n'
    result = subprocess.run(['bash', '-c', harness, str(candidate)], text=True, capture_output=True, timeout=40)
    thread.join(timeout=5)
    verdict = 'pass' if 'R0_OK=1 ' in result.stdout else 'reject'
    held = seen.get('body_done_at', 0) - seen.get('headers_at', 0)
    chunked = b'transfer-encoding: chunked' in seen.get('head', b'').lower()
    ok = verdict == expected and chunked and held >= 0.4
    failures += 0 if ok else 1
    print(f'{"OK  " if ok else "FAIL"} {name}: expected {expected}, instrument {verdict}; '
          f'request chunked={chunked}, headers held {held:.2f} s before the body completed; '
          f'{result.stdout.strip()} {result.stderr.strip()}'.rstrip())
print(f'{len(CASES) - failures}/{len(CASES)} cases as expected')
sys.exit(1 if failures else 0)
