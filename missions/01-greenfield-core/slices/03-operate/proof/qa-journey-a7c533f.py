"""Independent HTTP effects; every request executes the repo's loopback scripts/http.
The disposable launcher controls time and database availability, never product code.
Run from the main checkout: python3 missions/.../proof/qa-journey-a7c533f.py
"""
import csv
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import tempfile
import zipfile

ROOT = Path.cwd()
WT = ROOT / '.worktrees/03-operate'
PROOF = ROOT / 'missions/01-greenfield-core/slices/03-operate/proof'
TMP = Path(json.loads((ROOT / 'docs/qa/03-operate/runtime-path.json').read_text())['temporary'])
JAVA = '/opt/homebrew/opt/openjdk@21/bin/java'
HTTP = ROOT / 'scripts/http'
CP = f'{WT}/build/classes/java/main:{WT}/build/resources/main:{TMP}/libs/*'
SOURCE = TMP / 'QaControl.java'
shutil.copyfile(PROOF / 'qa-control-a7c533f.java', SOURCE)
EXCHANGES = []
RESULTS = []
PROCESSES = []

def available_port():
    with socket.socket() as s:
        s.bind(('127.0.0.1', 0))
        return s.getsockname()[1]

def wait_for(condition, label, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if condition():
            return
        time.sleep(.02)
    raise AssertionError('Timed out: ' + label)

class App:
    def __init__(self, name, env=None, jar=False):
        self.name, self.seq = name, 0
        self.dir = Path(tempfile.mkdtemp(dir=TMP, prefix=name + '-'))
        self.port = available_port()
        self.base = f'http://127.0.0.1:{self.port}'
        self.log_path = PROOF / f'qa-{name}-log-a7c533f.jsonl'
        self.log = self.log_path.open('w')
        settings = dict(os.environ)
        settings.update(env or {})
        settings['SPRING_DATASOURCE_URL'] = f'jdbc:h2:file:{self.dir}/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE'
        args = [f'--server.port={self.port}', '--server.address=127.0.0.1', '--spring.main.banner-mode=off']
        command = [JAVA, '-jar', str(WT / 'build/libs/urlshort.jar')] if jar else [JAVA, f'-Dqa.control.dir={self.dir}', '-cp', CP, str(SOURCE)]
        self.proc = subprocess.Popen(command + args, cwd=WT, env=settings, stdin=subprocess.PIPE, stdout=self.log, stderr=subprocess.STDOUT, text=True)
        PROCESSES.append(self)
        if jar:
            wait_for(lambda: self.request('/actuator/health/readiness', record=False, tolerate=True)[0] == 200, name + ' jar ready')
        else:
            wait_for(lambda: (self.dir / 'ready').exists() or self.proc.poll() is not None, name + ' controlled ready')
        assert self.proc.poll() is None, self.log_path.read_text()

    def command(self, value):
        self.seq += 1
        self.proc.stdin.write(value + '\n')
        self.proc.stdin.flush()
        wait_for(lambda: (self.dir / 'ack').exists() and (self.dir / 'ack').read_text().startswith(str(self.seq) + ' '), value)
        with (PROOF / 'qa-controls-a7c533f.txt').open('a') as f:
            f.write(self.name + ': ' + (self.dir / 'ack').read_text() + '\n')

    def request(self, path, method='GET', body=None, headers=None, peer='127.0.0.1', record=True, tolerate=False, raw=False):
        args = [str(HTTP), '--max-time', '5', '--path-as-is', '-sS', '-i', '-X', method]
        if peer != '127.0.0.1':
            args += ['-H', 'X-Qa-Peer: ' + peer]
        for key, value in (headers or {}).items():
            args += ['-H', key + ': ' + value]
        if body is not None:
            args += ['-H', 'Content-Type: application/json', '--data-binary', body if raw else json.dumps(body)]
        args += [self.base + path]
        proc = subprocess.run(args, capture_output=True, text=True)
        if proc.returncode and tolerate:
            return (0, {}, '')
        assert proc.returncode == 0, (args, proc.stderr)
        head, text = proc.stdout.split('\n\n', 1)
        status = int(head.splitlines()[0].split()[1])
        fields = {key.lower(): value.strip() for key, value in (line.split(':', 1) for line in head.splitlines()[1:] if ':' in line)}
        if record:
            EXCHANGES.append({'app': self.name, 'command': args, 'status': status, 'headers': fields, 'body': text, 'exchange': proc.stdout})
        return status, fields, text

    def create(self, peer='127.0.0.1', headers=None, **extra):
        return self.request('/api/links', 'POST', {'url': 'https://example.com/qa03', **extra}, headers, peer)

    def shift(self, millis=60000):
        self.command('SHIFT ' + str(millis))

    def snapshot(self, name):
        self.command('SNAPSHOT ' + name)
        for table in ['link', 'audit_log', 'click']:
            shutil.copyfile(self.dir / f'{name}-{table}.csv', PROOF / f'qa-{self.name}-{name}-{table}-a7c533f.csv')
        return list(csv.DictReader((self.dir / f'{name}-audit_log.csv').open()))

    def stop(self):
        if self.proc.poll() is None:
            if (self.dir / 'ready').exists():
                self.proc.stdin.write('STOP\n')
                self.proc.stdin.flush()
            else:
                self.proc.terminate()
            self.proc.wait(timeout=15)
        self.log.close()

def ok(label, detail):
    RESULTS.append({'criterion': label, 'result': 'PASS', 'effect': detail})
    print(label + ': ' + detail, flush=True)

def status(response, expected):
    assert response[0] == expected, response
    return response

def code(response):
    return json.loads(status(response, 201)[2])['code']

def problem(response):
    _, headers, text = status(response, 429)
    data = json.loads(text)
    assert headers['content-type'] == 'application/problem+json'
    assert int(headers['retry-after']) >= 1 and headers['x-request-id']
    assert data == {'instance': 'urn:uuid:' + headers['x-request-id'], 'status': 429, 'title': 'Too Many Requests'}
    assert 'location' not in headers
    return response

def metric(app, budget):
    body = json.loads(status(app.request('/actuator/metrics/urlshort.ratelimit.rejections?tag=budget:' + budget), 200)[2])
    return body['measurements'][0]['value']

def health(response, expected_status, expected_body):
    body = json.loads(status(response, expected_status)[2])
    assert body['status'] == expected_body
    assert set(body).issubset({'status', 'groups'}), body
    if 'groups' in body:
        assert body['groups'] == ['liveness', 'readiness']

try:
    a = App('default')
    before = a.snapshot('before')
    codes = [code(a.create()) for _ in range(60)]
    problem(a.create())
    after = a.snapshot('ac01')
    assert len(after) == len(before) + 60
    ok('AC-1', '60 x 201, 61st 429; exactly 60 new audit rows on real H2')
    c = codes[0]
    for _ in range(600): status(a.request('/' + c), 302)
    problem(a.request('/' + c))
    ok('AC-2', '600 x 302, 601st 429; no Location')
    a.shift(999)
    problem(a.create())
    a.shift(1)
    status(a.create(), 201)
    problem(a.create())
    a.shift(250)
    rejected = problem(a.create())
    a.shift(int(rejected[1]['retry-after']) * 1000)
    status(a.create(), 201)
    ok('AC-3', '999 ms refused, 1000 ms admitted exactly once; rounded-up Retry-After honored')
    a.shift()
    for _ in range(60): status(a.create(), 201)
    problem(a.create())
    ok('AC-4', 'quiet minute restores exactly 60 tokens')
    status(a.request('/' + c), 302)
    a.shift()
    for _ in range(600): status(a.request('/' + c), 302)
    problem(a.request('/' + c))
    status(a.create(), 201)
    ok('AC-5', 'exhausted create still redirects; exhausted redirect still creates')
    a.shift()
    for _ in range(60): status(a.create(peer='127.0.0.1'), 201)
    problem(a.create(peer='127.0.0.1'))
    status(a.create(peer='127.0.0.2'), 201)
    ok('AC-6', 'controlled request peer gets an independent bucket (SPEC suite mechanism)')
    a.shift()
    for i in range(60):
        status(a.create(headers={'X-Forwarded-For': f'198.51.100.{i}', 'Forwarded': f'for=203.0.113.{i}', 'X-Real-IP': f'192.0.2.{i}'}), 201)
    problem(a.create(headers={'X-Forwarded-For': '198.51.100.99'}))
    ok('AC-7', 'forged XFF/Forwarded/X-Real-IP never move default client budget')
    a.shift()
    for _ in range(60): status(a.request('/api/links', 'POST', {'url': 'ftp://invalid.example/'}), 400)
    problem(a.create())
    problem(a.request('/api/links', 'POST', '{"url":"https://example.com","pad":"' + 'x' * 16400 + '"}', raw=True))
    a.shift(1000)
    status(a.create(), 201)
    problem(a.create())
    ok('AC-9', '60 invalid requests consume tokens; valid/oversized then 429; rejection consumes no token')
    canaries = {'X-Forwarded-For': '192.0.2.201', 'User-Agent': 'qa03-ua-canary', 'X-Request-Id': 'qa03-inbound-canary'}
    refused = problem(a.create(headers=canaries, url='https://example.com/qa03-url-canary'))
    for marker in ['127.0.0.1', '192.0.2.201', 'qa03-ua-canary', 'qa03-url-canary', 'qa03-inbound-canary']:
        assert marker not in str(refused)
    ok('AC-11', 'exact problem media/body, whole-second retry, server id, no client/canaries')
    a.shift()
    for _ in range(60): status(a.create(), 201)
    for _ in range(600): status(a.request('/' + c), 302)
    for response in [a.create(headers=canaries, url='https://example.com/qa03-url-canary'), a.request('/' + c, headers=canaries), a.request('/api/links/' + c, headers=canaries)]:
        problem(response)
    a.request('/zzCanary99', peer='127.0.0.2')
    for path in ['/actuator/health', '/actuator/health/liveness', '/actuator/health/readiness']:
        health(a.request(path), 200, 'UP')
    ok('AC-13', 'health/liveness/readiness 200 and status UP')
    a.command('DOWN')
    health(a.request('/actuator/health/readiness'), 503, 'DOWN')
    health(a.request('/actuator/health'), 503, 'DOWN')
    health(a.request('/actuator/health/liveness'), 200, 'UP')
    a.command('UP')
    health(a.request('/actuator/health/readiness'), 200, 'UP')
    ok('AC-14', 'DataSource unavailable: readiness 503 DOWN, liveness 200 UP; restored readiness 200 UP')
    ok('AC-15', 'health bodies contain status and optional group names, no installation details')
    names = json.loads(status(a.request('/actuator/metrics'), 200)[2])['names']
    assert set(['http.server.requests', 'urlshort.ratelimit.rejections', 'hikaricp.connections.active', 'hikaricp.connections.idle']).issubset(names)
    ok('AC-16', 'timer, rejection counter, active and idle pool gauges listed')
    count_create, count_redirect = metric(a, 'create'), metric(a, 'redirect')
    for _ in range(2): problem(a.create())
    for _ in range(3): problem(a.request('/' + c))
    status(a.create(peer='127.0.0.2'), 201)
    status(a.request('/' + c, peer='127.0.0.2'), 302)
    assert metric(a, 'create') == count_create + 2
    assert metric(a, 'redirect') == count_redirect + 3
    tags = json.loads(a.request('/actuator/metrics/urlshort.ratelimit.rejections')[2])['availableTags']
    assert tags == [{'tag': 'budget', 'values': ['redirect', 'create']}] or tags == [{'tag': 'budget', 'values': ['create', 'redirect']}]
    ok('AC-17', 'exact +2 create/+3 redirect rejections, only budget tag')
    route = '/actuator/metrics/http.server.requests?tag=uri:/%7Bcode:%5BA-Za-z0-9%5D%7B6,32%7D%7D&tag=status:302'
    previous = json.loads(a.request(route)[2])['measurements'][0]['value']
    for _ in range(4): status(a.request('/' + c, peer='127.0.0.2'), 302)
    current = json.loads(a.request(route)[2])['measurements'][0]['value']
    assert current == previous + 4
    ok('AC-18', 'redirect count +4, uri template /{code:[A-Za-z0-9]{6,32}}')
    scrape = status(a.request('/actuator/prometheus'), 200)[2]
    for family in ['http_server_requests_seconds', 'urlshort_ratelimit_rejections_total', 'hikaricp_connections_active', 'hikaricp_connections_idle']:
        assert family in scrape
    for marker in [c, 'zzCanary99', '127.0.0.1', '192.0.2.201', 'qa03-ua-canary', 'https://example.com/qa03']:
        assert marker not in scrape, marker
    (PROOF / 'qa-prometheus-a7c533f.txt').write_text(scrape)
    ok('AC-19', 'Prometheus families present, client/code/URL/path canaries absent')
    live = json.loads(status(a.request('/v3/api-docs'), 200)[2])
    committed = json.loads((WT / 'docs/api/openapi.json').read_text())
    assert live == committed, 'Live and committed OpenAPI differ'
    operations = 0
    for path, entry in live['paths'].items():
        for method, operation in entry.items():
            if method not in ['get', 'post', 'delete', 'put', 'patch', 'head', 'options', 'trace']: continue
            rejection = operation['responses']['429']
            assert rejection['headers']['Retry-After']['schema']['type'] == 'integer'
            assert rejection['content']['application/problem+json']['examples']
            operations += 1
    assert operations == 6
    (PROOF / 'qa-openapi-a7c533f.json').write_text(json.dumps(live, sort_keys=True, indent=2) + '\n')
    (PROOF / 'qa-openapi-diff-a7c533f.txt').write_text('Six operations; entire key-sorted live and committed documents are identical; no normalization.\n')
    ok('AC-20', 'six operations carry 429, integer Retry-After, example; document comparison')
    for path, method in [('/api/ping', 'GET'), ('/api/links/' + c, 'GET'), ('/api/links/' + c, 'DELETE'), ('/api/links/' + c + '/stats', 'GET')]:
        problem(a.request(path, method))
    for path in ['/actuator/health', '/v3/api-docs', '/swagger-ui.html']:
        assert a.request(path)[0] != 429
    problem(a.request('/%61pi/links', 'POST', {'url': 'https://example.com/'}))
    ok('BR-1/3', 'all API operations share create budget, encoded API path charged, operator paths exempt')
    a.shift()
    status(a.request('/api/links', 'POST', {'url': 'ftp://invalid.example/'}), 400)
    status(a.request('/api/ping', 'POST'), 405)
    status(a.request('/api/links/missing99'), 404)
    status(a.request('/api/links', 'POST', '{"pad":"' + 'x' * 16400 + '"}', raw=True), 413)
    idem = code(a.create(headers={'Idempotency-Key': 'qa03-idem'}))
    status(a.create(headers={'Idempotency-Key': 'qa03-idem'}), 201)
    status(a.create(headers={'Idempotency-Key': 'qa03-idem'}, url='https://example.com/conflict'), 422)
    status(a.request('/api/links/' + idem, 'DELETE'), 204)
    status(a.request('/' + idem), 410)
    a.shift(86400000)
    renewed = code(a.create(headers={'Idempotency-Key': 'qa03-idem'}))
    assert renewed != idem
    ok('Inherited failure paths', '400 invalid URL, 405 method, 404 missing, 413 body cap, idempotent replay, 422 mismatch, 204 retire, 410 retired redirect; 24h idempotency expiry releases key')
    a.snapshot('final')
    a.stop()
    lines = []
    for line in a.log_path.read_text().splitlines():
        try: event = json.loads(line)
        except json.JSONDecodeError: continue
        if 'requestId' in event: lines.append(event)
    rejection_ids = [e['headers']['x-request-id'] for e in EXCHANGES if e['app'] == 'default' and e['status'] == 429]
    for rid in rejection_ids:
        events = [e for e in lines if e['requestId'] == rid]
        assert len(events) == 1 and events[0]['status'] == 429, (rid, events)
    full_log = a.log_path.read_text()
    for marker in ['127.0.0.1', '192.0.2.201', 'qa03-ua-canary', 'qa03-url-canary', 'qa03-inbound-canary', 'zzCanary99']:
        assert marker not in full_log, marker
    (PROOF / 'qa-correlated-429-a7c533f.jsonl').write_text(''.join(json.dumps(e) + '\n' for e in lines if e['requestId'] in rejection_ids))
    ok('AC-12', f'{len(rejection_ids)} rejected exchanges each correlate to exactly one JSON event; no client/canary in full log')

    p = App('trusted', {'URLSHORT_RATELIMIT_TRUSTEDPROXIES': '127.0.0.1'})
    for _ in range(60): status(p.create(headers={'X-Forwarded-For': '203.0.113.7'}), 201)
    problem(p.create(headers={'X-Forwarded-For': '198.51.100.1, 203.0.113.7'}))
    status(p.create(), 201)
    status(p.create(peer='127.0.0.2', headers={'X-Forwarded-For': '203.0.113.7'}), 201)
    ok('AC-8', 'trusted real loopback proxy uses right-most untrusted XFF; absent/controlled untrusted peers independent')
    p.stop()
    q = App('settings', {'URLSHORT_RATELIMIT_CREATEPERMINUTE': '2', 'URLSHORT_RATELIMIT_REDIRECTPERMINUTE': '3'})
    qc = code(q.create())
    status(q.create(), 201)
    problem(q.create())
    for _ in range(3): status(q.request('/' + qc), 302)
    problem(q.request('/' + qc))
    ok('AC-10', 'real environment settings: creates 201/201/429, redirects 302/302/302/429')
    q.stop()
    j = App('jar-settings', {'URLSHORT_RATELIMIT_CREATEPERMINUTE': '2', 'URLSHORT_RATELIMIT_REDIRECTPERMINUTE': '3', 'URLSHORT_RATELIMIT_TRUSTEDPROXIES': '127.0.0.1', 'URLSHORT_PUBLICBASEURL': 'https://short.example/qa'}, jar=True)
    first = j.create(headers={'X-Forwarded-For': '203.0.113.8'})
    jc = code(first)
    assert json.loads(first[2])['shortUrl'] == 'https://short.example/qa/' + jc
    status(j.create(headers={'X-Forwarded-For': '203.0.113.8'}), 201)
    problem(j.create(headers={'X-Forwarded-For': '198.51.100.1, 203.0.113.8'}))
    status(j.create(headers={'X-Forwarded-For': '203.0.113.9'}), 201)
    smoke = subprocess.run([str(WT / 'scripts/smoke.sh'), j.base], cwd=WT, capture_output=True, text=True, timeout=30)
    (PROOF / 'qa-smoke-jar-settings-a7c533f.txt').write_text(smoke.stdout + smoke.stderr)
    assert smoke.returncode == 0, smoke.stdout + smoke.stderr
    assert (j.dir / 'data/urlshort.mv.db').exists()
    ok('AC-26 supplemental jar', 'unmodified java -jar: public URL, 2-token create budget, trusted-proxy environment and data-path override all observed; smoke passes')
    j.stop()
    b = App('jar-bench', {'URLSHORT_RATELIMIT_CREATEPERMINUTE': '1000000', 'URLSHORT_RATELIMIT_REDIRECTPERMINUTE': '1000000'}, jar=True)
    smoke = subprocess.run([str(WT / 'scripts/smoke.sh'), b.base], cwd=WT, capture_output=True, text=True, timeout=30)
    (PROOF / 'qa-smoke-jar-a7c533f.txt').write_text(smoke.stdout + smoke.stderr)
    assert smoke.returncode == 0, smoke.stdout + smoke.stderr
    bench = subprocess.run([str(WT / 'scripts/smoke.sh'), '--bench', b.base], cwd=WT, capture_output=True, text=True, timeout=90)
    (PROOF / 'qa-bench-a7c533f.txt').write_text(bench.stdout + bench.stderr)
    assert bench.returncode == 0, bench.stdout + bench.stderr
    assert all(token in bench.stdout for token in ['achieved', 'p50', 'p95', 'p99', 'non-2xx/3xx'])
    ok('Proof item 12', '60s bench mode executed on unmodified jar; mode output captured, no release target verdict')
    b.stop()
    drain_port = available_port()
    drain = subprocess.run([str(WT / 'scripts/smoke.sh'), '--drain', str(WT / 'build/libs/urlshort.jar'), str(drain_port)], cwd=WT, capture_output=True, text=True, timeout=90)
    (PROOF / 'qa-drain-a7c533f.txt').write_text(drain.stdout + drain.stderr)
    assert drain.returncode == 0, drain.stdout + drain.stderr
    ok('AC-25 supplemental jar', drain.stdout.strip())
finally:
    for process in PROCESSES:
        process.stop()
    (PROOF / 'qa-http-a7c533f.json').write_text(json.dumps(EXCHANGES, indent=2) + '\n')
    (PROOF / 'qa-observed-a7c533f.json').write_text(json.dumps(RESULTS, indent=2) + '\n')
    (PROOF / 'qa-processes-stopped-a7c533f.txt').write_text('\n'.join(f'{p.name} pid={p.proc.pid} exit={p.proc.poll()}' for p in PROCESSES) + '\n')
print(f'{len(EXCHANGES)} exchanges recorded; {len(RESULTS)} effect groups passed', flush=True)
