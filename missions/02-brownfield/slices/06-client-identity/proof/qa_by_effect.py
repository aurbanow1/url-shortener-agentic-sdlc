"""Independent HTTP/JDBC observations on original production and exact candidate.

The external runner supplies the AC's clock/peer inputs and a bounded slow store.
CR-01 container rewriting is separately observed with the unchanged jar replay.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import hashlib
import json
import shutil
import socket
import subprocess
import tempfile
import threading
import time

ROOT = Path(__file__).resolve().parents[5]
PROOF = Path(__file__).resolve().parent
RUN = 'run-' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
OUT = ROOT / 'docs/qa/06-client-identity/effects' / RUN
OUT.mkdir(parents=True)
(PROOF / 'qa-last-run.txt').write_text(str(OUT.relative_to(ROOT)) + '\n')
SCRATCH = Path(tempfile.mkdtemp(prefix='urlshort-qa2-identity-effects-', dir='/private/tmp'))
JAVA = '/opt/homebrew/opt/openjdk@21/bin/java'
CONTROL_ROOT = Path('/private/tmp/urlshort-qa2-identity-controls')
APPS, REQUESTS, CHECKS = [], [], []
LOCK = threading.Lock()
P, Q, U, V, OTHER, SETUP = '10.9.9.9', '10.9.9.8', '203.0.113.7', '203.0.113.8', '192.0.2.200', '198.51.100.250'
DAY = '2026-10-01'
TARGET = 'https://example.com/QA06-AUDIT-TARGET-CANARY'
UA = 'Mozilla/5.0 QA06-FULL-AGENT-CANARY'
REFERRER = 'https://ref.example/QA06-PATH-CANARY?q=QA06-QUERY-CANARY#QA06-FRAGMENT-CANARY'
INBOUND = 'QA06-INBOUND-ID-CANARY'
CANARIES = [P, Q, U, V, OTHER, SETUP, UA, 'QA06-PATH-CANARY', 'QA06-QUERY-CANARY',
            'QA06-FRAGMENT-CANARY', INBOUND]


def save(name, value):
    (OUT / (name + '.json')).write_text(json.dumps(value, indent=2) + '\n')


def check(claim, result, detail=None):
    with LOCK:
        CHECKS.append({'claim': claim, 'pass': bool(result), 'detail': detail})
        with (OUT / 'assertions.jsonl').open('a') as file:
            file.write(json.dumps(CHECKS[-1]) + '\n')
    if not result:
        raise AssertionError(claim + ': ' + str(detail))


class App:
    def __init__(self, lane, name, trusted='', limits=None, directory=None):
        self.lane, self.name = lane, lane + '-' + name
        self.directory = directory or SCRATCH / self.name
        self.directory.mkdir(exist_ok=True)
        self.control = SCRATCH / (self.name + '-control')
        self.control.mkdir()
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            self.port = sock.getsockname()[1]
        cp = (CONTROL_ROOT / lane / 'classpath.txt').read_text()
        argv = [JAVA, '-Dqa.dir=' + str(self.control), '-cp', cp, 'dev.urlshort.click.QaIdentityRunner',
                '--server.address=127.0.0.1', '--server.port=' + str(self.port),
                '--spring.datasource.url=jdbc:h2:file:' + str(self.directory / 'urlshort')
                + ';MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE', '--urlshort.rate-limit.trusted-proxies=' + trusted]
        if limits:
            argv += ['--urlshort.rate-limit.create-per-minute=' + str(limits),
                     '--urlshort.rate-limit.redirect-per-minute=' + str(limits)]
        self.logpath = OUT / (self.name + '.jsonl')
        self.log = self.logpath.open('wb')
        self.process = subprocess.Popen(argv, cwd=self.directory, stdout=self.log, stderr=subprocess.STDOUT)
        APPS.append(self)
        save(self.name + '-launch', {'argv': argv, 'pid': self.process.pid, 'db': str(self.directory)})
        deadline = time.monotonic() + 50
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError(self.name + ' exited: ' + self.logpath.read_text()[-2500:])
            probe = subprocess.run([str(ROOT / 'scripts/http'), '-sS', '--max-time', '1',
                                    'http://localhost:' + str(self.port) + '/actuator/health/readiness'], capture_output=True)
            if probe.returncode == 0 and b'"UP"' in probe.stdout and (self.control / 'ready').exists():
                break
            time.sleep(.1)
        else:
            raise TimeoutError(self.name + ' readiness')

    def command(self, op, **values):
        ack = self.control / 'ack.json'
        if ack.exists():
            ack.unlink()
        temporary = self.control / 'command.tmp'
        temporary.write_text(json.dumps({'op': op, **values}))
        temporary.rename(self.control / 'command.json')
        deadline = time.monotonic() + 20
        while not ack.exists():
            if time.monotonic() > deadline:
                raise TimeoutError(self.name + ' ' + op)
            time.sleep(.005)
        result = json.loads(ack.read_text())
        ack.unlink()
        if isinstance(result, dict) and 'error' in result:
            raise RuntimeError(result['error'])
        return result

    def sql(self, sql):
        result = self.command('sql', sql=sql)
        with (OUT / (self.name + '-sql.jsonl')).open('a') as file:
            file.write(json.dumps({'sql': sql, 'result': result}) + '\n')
        return result

    def at(self, minute=0):
        instant = dt.datetime(2026, 10, 1, 12, tzinfo=dt.timezone.utc) + dt.timedelta(minutes=minute)
        return self.command('clock', instant=instant.isoformat().replace('+00:00', 'Z'))

    def peer(self, peer):
        return self.command('peer', peer=peer)

    def settle(self):
        return self.command('settle')

    def stop(self):
        if self.process.poll() is None:
            self.command('stop')
            self.process.wait(timeout=20)
        self.log.close()
        with socket.socket() as sock:
            refused = sock.connect_ex(('127.0.0.1', self.port)) != 0
        check(self.name + ' stopped/port closed', self.process.poll() is not None and refused,
              {'pid': self.process.pid, 'port': self.port, 'exit': self.process.returncode})


def http(app, name, method, path, status, headers=(), data=None, peer=None):
    if peer is not None:
        app.peer(peer)
    overrides = {key.lower(): value for key, value in headers if value and not value.strip()}
    # curl omits all-space header values. The external wrapper supplies these
    # exact Servlet values; empty headers and all other headers go on the wire.
    app.command('headers', headers=overrides)
    argv = [str(ROOT / 'scripts/http'), '-sS', '--max-time', '10', '-i']
    argv += ['--head'] if method == 'HEAD' else ['-X', method]
    argv += ['-H', 'X-Request-Id: ' + INBOUND]
    for key, value in headers:
        argv += ['-H', key + (': ' + value if value else ';')]
    if data is not None:
        argv += ['-H', 'Content-Type: application/json', '--data', json.dumps(data)]
    argv += ['http://localhost:' + str(app.port) + path]
    start = time.monotonic()
    result = subprocess.run(argv, capture_output=True)
    elapsed = time.monotonic() - start
    filename = app.name + '-' + name + '.http'
    (OUT / filename).write_bytes(result.stdout)
    check(app.name + '/' + name + ' transport', result.returncode == 0, result.stderr.decode())
    head, body = result.stdout.decode().split('\r\n\r\n', 1)
    lines = head.splitlines()
    actual = int(lines[0].split()[1])
    response_headers = dict((key.lower(), value.strip()) for key, value in
                            (line.split(':', 1) for line in lines[1:]))
    try:
        parsed = json.loads(body) if body else None
    except json.JSONDecodeError:
        parsed = None
    record = {'lane': app.lane, 'app': app.name, 'name': name, 'argv': argv, 'method': method,
              'path': path, 'status': actual, 'headers': response_headers, 'json': parsed,
              'body': body, 'raw': filename, 'headerOverrides': overrides, 'elapsedSeconds': elapsed}
    with LOCK:
        REQUESTS.append(record)
        with (OUT / 'requests.jsonl').open('a') as file:
            file.write(json.dumps(record) + '\n')
    check(app.name + '/' + name + ' status', actual == status, actual)
    request_id = response_headers.get('x-request-id')
    check(app.name + '/' + name + ' issued id', bool(request_id) and request_id != INBOUND, request_id)
    if method == 'HEAD':
        check(app.name + '/' + name + ' empty HEAD', body == '')
    elif status >= 400:
        check(app.name + '/' + name + ' problem', response_headers.get('content-type') == 'application/problem+json'
              and isinstance(parsed, dict) and parsed.get('status') == status, parsed)
        check(app.name + '/' + name + ' correlated problem', parsed.get('instance') == 'urn:uuid:' + request_id)
    return record


def create(app, name, peer=SETUP, headers=(), key=None, url=TARGET, status=201):
    headers = list(headers) + ([('Idempotency-Key', key)] if key else [])
    return http(app, name, 'POST', '/api/links', status, headers, {'url': url}, peer)


def open_link(app, name, code, peer, headers=(), method='GET', status=302):
    headers = list(headers) + [('User-Agent', UA), ('Referer', REFERRER)]
    result = http(app, name, method, '/' + code, status, headers, peer=peer)
    if status == 302:
        check(app.name + '/' + name + ' target/cache', result['headers'].get('location') == TARGET
              and result['headers'].get('cache-control') == 'no-store', result['headers'])
    return result


def figures(app, name, code, clicks, unique):
    app.settle()
    result = http(app, name, 'GET', '/api/links/' + code + '/stats', 200, peer=SETUP)
    expected = {'date': DAY, 'clicks': clicks, 'uniqueVisitors': unique, 'botClicks': 0}
    check(app.name + '/' + name + ' exact day/grouping', result['json']['totalClicks'] == clicks
          and result['json']['clicksPerDay'] == [expected], result['json'])
    rows = app.sql("SELECT clicked_on,referrer,user_agent_class,client_hash FROM click WHERE link_id="
                   + "(SELECT id FROM link WHERE code='" + code + "') ORDER BY id")
    check(app.name + '/' + name + ' private rows', len(rows) == clicks and
          len({r['client_hash'] for r in rows}) == unique and
          not any(canary in json.dumps(rows) for canary in CANARIES), rows)
    return result


MATRIX = [
    ('', P, U, [('Forwarded', 'for=' + V), ('X-Real-IP', V)], P),
    (P, '10.0.0.5', U, [], '10.0.0.5'),
    (P, P, U, [], U),
    (P, P, '198.51.100.1, ' + U, [], U),
    (P + ',' + Q, P, V + ', ' + U + ', ' + Q, [], U),
    (P, P, '  ' + U + ' ,  ', [], U),
    (P + ',' + Q, P, P + ', ' + Q, [], P),
    (P, P, None, [], P),
    (P, P, '', [], P),
    (P, P, ' , ', [], P),
    (P, P, None, [('Forwarded', 'for=' + U), ('X-Real-IP', U)], P),
    (P, P, U, [('Forwarded', 'for=' + V), ('X-Real-IP', V)], U),
]


def matrix(lane):
    apps = {trust: App(lane, 'matrix-' + str(index), trust, 2)
            for index, trust in enumerate(['', P, P + ',' + Q])}
    for index, (trust, peer, forwarded, others, reference) in enumerate(MATRIX, 1):
        app = apps[trust]
        prefix = 'M%02d-' % index
        headers = ([('X-Forwarded-For', forwarded)] if forwarded is not None else []) + others
        app.at(index * 8)
        for turn in [1, 2]:
            create(app, prefix + 'create-spend-' + str(turn), reference)
        rejected = create(app, prefix + 'create-shared-429', peer, headers, status=429)
        check(app.name + '/' + prefix + 'create retry30', rejected['headers'].get('retry-after') == '30')
        code = create(app, prefix + 'create-independent', OTHER)['json']['code']
        app.at(index * 8 + 2)
        for turn in [1, 2]:
            open_link(app, prefix + 'redirect-spend-' + str(turn), code, reference)
        rejected = open_link(app, prefix + 'redirect-shared-429', code, peer, headers, status=429)
        check(app.name + '/' + prefix + 'redirect retry30/no target', rejected['headers'].get('retry-after') == '30'
              and 'location' not in rejected['headers'])
        open_link(app, prefix + 'redirect-independent', code, OTHER)
        app.at(index * 8 + 4)
        code = create(app, prefix + 'stats-fixture')['json']['code']
        open_link(app, prefix + 'stats-row', code, peer, headers)
        open_link(app, prefix + 'stats-reference', code, reference)
        open_link(app, prefix + 'stats-independent', code, OTHER)
        figures(app, prefix + 'stats', code, 3, 2)
    app = apps['']
    app.at(120)
    before = app.sql('SELECT * FROM audit_log ORDER BY id')
    bad = [('X-Forwarded-For', U), ('Accept', 'text/html')]
    http(app, 'precedence-available-403', 'GET', '/api/audit?limit=0', 403, bad, peer='127.0.0.1')
    http(app, 'precedence-spend', 'GET', '/api/links/zzzzzzzz', 404, peer='127.0.0.1')
    refused = http(app, 'precedence-spent-429', 'GET', '/api/audit?limit=0', 429, bad, peer='127.0.0.1')
    check(app.name + ' precedence retry30', refused['headers'].get('retry-after') == '30')
    check(app.name + ' precedence rows unchanged', before == app.sql('SELECT * FROM audit_log ORDER BY id'))
    for app in apps.values():
        app.stop()


def default_budgets(lane):
    app = App(lane, 'defaults')
    code = create(app, 'setup-default-link')['json']['code']
    before = len(app.sql('SELECT id FROM link'))
    for turn in range(1, 62):
        headers = [('X-Forwarded-For', '198.51.100.' + str(turn % 250)),
                   ('Forwarded', 'for=203.0.113.' + str(turn % 250)), ('X-Real-IP', '192.0.2.' + str(turn % 250))]
        create(app, 'default-create-%03d' % turn, '10.10.1.1', headers, status=201 if turn <= 60 else 429)
    check(app.name + ' 61st creates no link', len(app.sql('SELECT id FROM link')) == before + 60)
    for turn in range(1, 602):
        headers = [('X-Forwarded-For', '198.51.100.' + str(turn % 250)),
                   ('Forwarded', 'for=203.0.113.' + str(turn % 250)), ('X-Real-IP', '192.0.2.' + str(turn % 250))]
        result = open_link(app, 'default-redirect-%03d' % turn, code, '10.10.1.2', headers,
                           status=302 if turn <= 600 else 429)
    check(app.name + ' 601st has no Location', 'location' not in result['headers'])
    figures(app, 'default-counted-clicks', code, 600, 1)
    app.stop()


LOOPBACKS = ['127.0.0.1', '127.0.0.2', '127.255.255.254', '::1', '0:0:0:0:0:0:0:1', '::ffff:127.0.0.1']
NONLOCAL = ['192.0.2.10', '10.0.0.7', '::ffff:192.0.2.10', 'fe80::1']


def audit_matrix(lane):
    for trusted in [False, True]:
        app = App(lane, 'audit-' + str(int(trusted)), ','.join(LOOPBACKS + NONLOCAL) if trusted else '', 5000)
        create(app, 'audit-canary-fixture')
        before = app.sql('SELECT * FROM audit_log ORDER BY id')
        for index, peer in enumerate(LOOPBACKS):
            http(app, 'loopback-' + str(index), 'GET', '/api/audit', 200, peer=peer)
        for index, peer in enumerate(NONLOCAL):
            for method in ['GET', 'HEAD']:
                http(app, 'nonlocal-' + str(index) + '-' + method, method, '/api/audit', 403, peer=peer)
        for peer_index, peer in enumerate(['127.0.0.1', '192.0.2.10']):
            for key in ['X-Forwarded-For', 'Forwarded']:
                values = [U, '127.0.0.1', '', '   '] if key == 'X-Forwarded-For' else ['for=' + U, 'for=127.0.0.1', '', '   ']
                for value_index, value in enumerate(values):
                    http(app, 'forwarded-%d-%s-%d' % (peer_index, key, value_index), 'GET', '/api/audit', 403,
                         [(key, value)], peer=peer)
        check(app.name + ' reads/refusals change no row', before == app.sql('SELECT * FROM audit_log ORDER BY id'))
        app.stop()


def core(lane, directory, active, retired):
    app = App(lane, 'core', directory=directory)
    app.at(2)
    original = app.sql('SELECT * FROM link ORDER BY id')
    create(app, 'status-201', peer='127.0.0.1')
    open_link(app, 'status-302', active, '127.0.0.1')
    open_link(app, 'head-302', active, '127.0.0.1', method='HEAD')
    audit_before = app.sql('SELECT * FROM audit_log ORDER BY id')
    http(app, 'audit-200', 'GET', '/api/audit', 200, peer='127.0.0.1')
    http(app, 'audit-400', 'GET', '/api/audit?limit=0', 400, peer='127.0.0.1')
    http(app, 'audit-403', 'GET', '/api/audit', 403, [('X-Forwarded-For', U)], peer='127.0.0.1')
    open_link(app, 'unknown-404', 'zzzzzzzz', '127.0.0.1', status=404)
    http(app, 'audit-405', 'POST', '/api/audit', 405, peer='127.0.0.1')
    open_link(app, 'active-post-405', active, '127.0.0.1', method='POST', status=405)
    open_link(app, 'retired-410', retired, '127.0.0.1', status=410)
    create(app, 'invalid-url-400', '127.0.0.1', url='javascript:alert(1)', status=400)
    app.sql('ALTER TABLE audit_log RENAME TO qa_audit_saved')
    try:
        http(app, 'audit-injected-500', 'GET', '/api/audit', 500, peer='127.0.0.1')
    finally:
        app.sql('ALTER TABLE qa_audit_saved RENAME TO audit_log')
    check(app.name + ' error/read paths preserve audit', audit_before == app.sql('SELECT * FROM audit_log ORDER BY id'))
    app.settle()
    check(app.name + ' errors and HEAD record no click', len(app.sql('SELECT id FROM click')) == 1)
    remaining = app.sql('SELECT * FROM link ORDER BY id')
    check(app.name + ' baseline-origin rows untouched', remaining[:len(original)] == original)
    app.at(4)
    first = create(app, 'duplicate-first', '127.0.0.1', key='QA06-IDEMPOTENCY-CANARY')
    replay = create(app, 'duplicate-replay', '127.0.0.1', key='QA06-IDEMPOTENCY-CANARY')
    check(app.name + ' duplicate same link', first['json'] == replay['json'])
    create(app, 'duplicate-mismatch-422', '127.0.0.1', key='QA06-IDEMPOTENCY-CANARY',
           url='https://example.com/different', status=422)
    app.at(24 * 60 + 6)
    expired = create(app, 'expired-key-new-link', '127.0.0.1', key='QA06-IDEMPOTENCY-CANARY')
    check(app.name + ' expired key released', expired['json']['code'] != first['json']['code'])
    for turn in range(1, 62):
        result = http(app, 'correlated-budget-%02d' % turn, 'GET', '/api/links/zzzzzzzz',
                      429 if turn == 60 else 404, peer='127.0.0.1')
        if result['status'] == 429:
            break
    check(app.name + ' observed correlated429', result['status'] == 429)
    app.stop()


def click_semantics(lane, trusted):
    app = App(lane, 'clicks-' + str(int(trusted)), P if trusted else '', 5000)
    code = create(app, 'click-fixture')['json']['code']
    headers = [('X-Forwarded-For', U)]
    app.command('hold')
    slow = open_link(app, 'slow-302', code, P, headers)
    deadline = time.monotonic() + 2
    while not app.command('held') and time.monotonic() < deadline:
        time.sleep(.01)
    check(app.name + ' writer held and redirect completed', app.command('held') and slow['elapsedSeconds'] < 1, slow['elapsedSeconds'])
    check(app.name + ' held row not yet stored', app.sql('SELECT id FROM click') == [])
    app.command('release')
    app.settle()
    app.sql('ALTER TABLE click RENAME TO qa_click_saved')
    try:
        failed = open_link(app, 'failed-store-302', code, P, headers)
        app.settle()
    finally:
        app.sql('ALTER TABLE qa_click_saved RENAME TO click')
    app.peer(P)
    def concurrent(index):
        return open_link(app, 'concurrent-%02d' % index, code, P,
                         [('X-Forwarded-For', U if index % 2 == 0 else V)])
    # Set peer once; command-file control itself is sequential. HTTP is concurrent.
    original_command = app.command
    app.command('headers', headers={})
    def fixed_inputs(op, **kwargs):
        if op == 'peer':
            return P
        if op == 'headers':
            return {}
        return original_command(op, **kwargs)
    app.command = fixed_inputs
    try:
        with ThreadPoolExecutor(max_workers=8) as pool:
            list(pool.map(concurrent, range(20)))
    finally:
        app.command = original_command
    open_link(app, 'successful-head-no-click', code, P, headers, method='HEAD')
    open_link(app, 'unsuccessful-no-click', 'zzzzzzzz', P, headers, status=404)
    result = figures(app, 'click-semantics-stats', code, 21, 2 if trusted else 1)
    recorded = http(app, 'recorded-counter', 'GET', '/actuator/metrics/urlshort.clicks.recorded', 200)
    lost = http(app, 'lost-counter', 'GET', '/actuator/metrics/urlshort.clicks.lost?tag=reason:write%20failed', 200)
    check(app.name + ' recorded/lost counters', recorded['json']['measurements'][0]['value'] == 21
          and lost['json']['measurements'][0]['value'] == 1)
    scrape = http(app, 'privacy-prometheus', 'GET', '/actuator/prometheus', 200)
    private_rows = app.sql('SELECT * FROM click ORDER BY id')
    hashes = [row['client_hash'] for row in private_rows]
    check(app.name + ' private surfaces', not any(value in json.dumps(result['json']) + scrape['body']
          for value in CANARIES + hashes))
    app.stop()
    check(app.name + ' private hashes absent from logs', not any(value in app.logpath.read_text() for value in hashes))
    events = [json.loads(line) for line in app.logpath.read_text().splitlines() if line.startswith('{')]
    warnings = [event for event in events if event.get('message') == 'click lost']
    check(app.name + ' one correlated lost-click warning', len(warnings) == 1
          and warnings[0]['requestId'] == failed['headers']['x-request-id']
          and warnings[0]['reason'] == 'write failed' and warnings[0]['log']['level'] == 'WARN', warnings)


def correlated_logs():
    joins = []
    for app in APPS:
        events = [json.loads(line) for line in app.logpath.read_text().splitlines() if line.startswith('{')]
        for request in [r for r in REQUESTS if r['app'] == app.name]:
            matching = [e for e in events if e.get('requestId') == request['headers']['x-request-id']]
            completed = [e for e in matching if e.get('message') == 'request completed']
            check(app.name + '/' + request['name'] + ' log correlation', len(completed) == 1
                  and completed[0]['status'] == request['status'] and completed[0]['log']['level'] == 'INFO', matching)
            joins.append({'app': app.name, 'name': request['name'], 'events': matching})
        check(app.name + ' no private values in logs', not any(canary in app.logpath.read_text()
              for canary in CANARIES + ['QA06-AUDIT-TARGET-CANARY', 'QA06-IDEMPOTENCY-CANARY']))
    save('request-log-joins', joins)
    return joins


def compare(joins):
    logs = {(j['app'], j['name']): j['events'] for j in joins}
    normalized = {}
    substitutions = {}
    for lane in ['baseline', 'candidate']:
        requests = [r for r in REQUESTS if r['lane'] == lane and not r['app'].endswith('-seed')]
        values = {}
        for r in requests:
            scope = r['app'].removeprefix(lane + '-') + '/' + r['name']
            values[r['headers']['x-request-id']] = '<request:' + scope + '>'
            if r['status'] == 201:
                values.setdefault(r['json']['code'], '<code:' + scope + '>')
        substitutions[lane] = values
        def replace(value):
            if isinstance(value, str):
                for original, token in values.items():
                    value = value.replace(original, token)
                return value
            if isinstance(value, list):
                return [replace(v) for v in value]
            if isinstance(value, dict):
                return {k: replace(v) for k, v in value.items()}
            return value
        normalized[lane] = {}
        for r in requests:
            scope = r['app'].removeprefix(lane + '-') + '/' + r['name']
            headers = replace(r['headers'])
            if 'date' in headers:
                headers['date'] = '<HTTP-Date>'
            events = replace(logs[r['app'], r['name']])
            for index, event in enumerate(events):
                event['@timestamp'] = '<log-time:' + str(index) + '>'
                event['process']['pid'] = '<pid:' + r['app'].removeprefix(lane + '-') + '>'
            normalized[lane][scope] = {'method': r['method'], 'path': replace(r['path']),
                                      'status': r['status'], 'headers': headers, 'json': replace(r['json']),
                                      'body': None if r['json'] is not None else replace(r['body']), 'events': events}
    save('substitutions', substitutions)
    save('normalized-baseline', normalized['baseline'])
    save('normalized-candidate', normalized['candidate'])
    all_names = sorted(set(normalized['baseline']) | set(normalized['candidate']))
    differences = [{'scope': name, 'baseline': normalized['baseline'].get(name),
                    'candidate': normalized['candidate'].get(name)} for name in all_names
                   if normalized['baseline'].get(name) != normalized['candidate'].get(name)]
    save('comparison', {'requestsPerLane': {k: len(v) for k, v in normalized.items()},
                        'differenceCount': len(differences), 'differences': differences})
    check('all controlled HTTP/log comparisons identical', not differences, differences[:5])


try:
    save('fixture', {'scratch': str(SCRATCH), 'helperSha256': hashlib.sha256((PROOF / 'QaIdentityRunner.java').read_bytes()).hexdigest(),
                     'baselineProductionTree': '50387c729aa575a5eebc505f86c542ed22ff2995',
                     'candidateSha': 'fb63a88a9b92c1fec97ba74686af1a2f30304160'})
    seed = App('baseline', 'seed', limits=5000)
    active = create(seed, 'baseline-active')['json']['code']
    retired = create(seed, 'baseline-retired')['json']['code']
    http(seed, 'baseline-retire', 'DELETE', '/api/links/' + retired, 204, peer=SETUP)
    save('baseline-origin-fixtures', {'active': active, 'retired': retired, 'target': TARGET,
                                     'links': seed.sql('SELECT * FROM link ORDER BY id'),
                                     'audit': seed.sql('SELECT * FROM audit_log ORDER BY id')})
    seed.stop()
    for lane in ['baseline', 'candidate']:
        copied = SCRATCH / (lane + '-core-db')
        shutil.copytree(seed.directory, copied)
        core(lane, copied, active, retired)
        default_budgets(lane)
        matrix(lane)
        audit_matrix(lane)
        click_semantics(lane, False)
        click_semantics(lane, True)
    joins = correlated_logs()
    compare(joins)
    save('summary', {'pass': True, 'requests': len(REQUESTS), 'assertions': len(CHECKS),
                     'appsStopped': len(APPS), 'output': str(OUT.relative_to(ROOT))})
    print(json.dumps(json.loads((OUT / 'summary.json').read_text())))
finally:
    for app in APPS:
        if app.process.poll() is None:
            app.stop()
