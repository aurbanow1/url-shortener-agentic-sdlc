"""Read-only jar baseline. Run from the repository; HTTP uses scripts/http."""
from pathlib import Path
import datetime as dt
import hashlib
import json
import shutil
import socket
import subprocess
import tempfile
import time

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
JAVA = '/opt/homebrew/opt/openjdk@21/bin/java'
SCRATCH = Path(tempfile.mkdtemp(prefix='urlshort-qa2-identity-', dir='/private/tmp'))
JAR = SCRATCH / 'urlshort.jar'
shutil.copy2(ROOT / 'build/libs/urlshort.jar', JAR)
SOURCE = json.loads((OUT / 'source.json').read_text())
APPS, REQUESTS, CHECKS = [], [], []


def save(name, value):
    (OUT / (name + '.json')).write_text(json.dumps(value, indent=2) + '\n')


def check(claim, value, detail=None):
    CHECKS.append({'claim': claim, 'pass': bool(value), 'detail': detail})
    save('assertions', CHECKS)
    if not value:
        raise AssertionError(claim + ': ' + str(detail))


def tree():
    return subprocess.check_output(['git', 'rev-parse', 'HEAD:src/main'], cwd=ROOT, text=True).strip()


class App:
    def __init__(self, name, settings=()):
        self.name = name
        with socket.socket() as reserve:
            reserve.bind(('127.0.0.1', 0))
            self.port = reserve.getsockname()[1]
        directory = SCRATCH / name
        directory.mkdir()
        self.argv = [JAVA, '-jar', str(JAR), '--server.address=127.0.0.1',
                     '--server.port=' + str(self.port),
                     '--spring.datasource.url=jdbc:h2:file:' + str(directory / 'urlshort')
                     + ';MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE'] + list(settings)
        self.logpath = OUT / (name + '.jsonl')
        self.log = self.logpath.open('wb')
        self.process = subprocess.Popen(self.argv, cwd=directory, stdout=self.log, stderr=subprocess.STDOUT)
        APPS.append(self)
        save(name + '-launch', {'argv': self.argv, 'pid': self.process.pid, 'database': str(directory)})
        deadline = time.monotonic() + 50
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError(name + ' exited: ' + self.logpath.read_text()[-2000:])
            probe = subprocess.run([str(ROOT / 'scripts/http'), '-sS', '--max-time', '1',
                                    'http://localhost:' + str(self.port) + '/actuator/health/readiness'],
                                   capture_output=True)
            if probe.returncode == 0 and b'"UP"' in probe.stdout:
                break
            time.sleep(.1)
        else:
            raise TimeoutError(name + ' readiness')

    def stop(self):
        if self.process.poll() is None:
            self.process.terminate()
            self.process.wait(timeout=20)
        self.log.close()
        with socket.socket() as probe:
            refused = probe.connect_ex(('127.0.0.1', self.port)) != 0
        check(self.name + ' stopped and port closed', self.process.poll() is not None and refused,
              {'pid': self.process.pid, 'exit': self.process.returncode, 'port': self.port})


def http(app, name, path, expected, headers=(), method='GET', data=None, peer=None):
    stem = '%03d-%s' % (len(REQUESTS) + 1, name)
    argv = [str(ROOT / 'scripts/http'), '-sS', '--max-time', '8', '-i',
            '--trace-ascii', str(OUT / (stem + '.trace'))]
    argv += ['--head'] if method == 'HEAD' else ['-X', method]
    if peer:
        argv += ['--interface', peer]
    for key, value in headers:
        # curl's trailing semicolon transmits an empty header instead of removing it.
        argv += ['-H', key + (': ' + value if value else ';')]
    if data is not None:
        argv += ['-H', 'Content-Type: application/json', '--data', json.dumps(data)]
    argv += ['http://localhost:' + str(app.port) + path]
    result = subprocess.run(argv, capture_output=True)
    (OUT / (stem + '.http')).write_bytes(result.stdout)
    check(name + ' curl exit', result.returncode == 0, result.stderr.decode())
    head, body = result.stdout.decode().split('\r\n\r\n', 1)
    lines = head.splitlines()
    status = int(lines[0].split()[1])
    response_headers = dict((key.lower(), value.strip()) for key, value in
                            (line.split(':', 1) for line in lines[1:]))
    parsed = json.loads(body) if body else None
    record = {'name': name, 'app': app.name, 'argv': argv, 'method': method, 'path': path,
              'raw': stem + '.http', 'status': status, 'headers': response_headers, 'json': parsed}
    REQUESTS.append(record)
    save(stem, record)
    check(name + ' status', status == expected, status)
    check(name + ' request id', bool(response_headers.get('x-request-id')))
    if method == 'HEAD':
        check(name + ' HEAD has no body', body == '')
    elif expected >= 400:
        check(name + ' problem', response_headers.get('content-type') == 'application/problem+json'
              and parsed.get('status') == expected, parsed)
        check(name + ' problem correlation', parsed.get('instance') == 'urn:uuid:'
              + response_headers['x-request-id'], parsed)
    return record


def create(app, name, headers=()):
    return http(app, name, '/api/links', 201, headers, 'POST',
                {'url': 'https://example.com/qa06/client-group'})


def stats(app, name, code, count, unique):
    deadline = time.monotonic() + 8
    attempts = 0
    while True:
        attempts += 1
        response = http(app, name + '-poll-' + str(attempts), '/api/links/' + code + '/stats', 200)
        if response['json']['totalClicks'] == count:
            break
        if time.monotonic() > deadline:
            raise TimeoutError(name + ' click writes')
        time.sleep(.05)
    body = response['json']
    day = body['clicksPerDay']
    check(name + ' exact grouped counts', len(day) == 1 and day[0]['clicks'] == count
          and day[0]['uniqueVisitors'] == unique and day[0]['botClicks'] == 0, body)
    check(name + ' exact response fields', set(body) == {'code', 'totalClicks', 'clicksPerDay', 'topReferrers'}, body)
    return response


def audit_cases(app, prefix):
    http(app, prefix + '-audit-admitted', '/api/audit', 200)
    for key, values in [('X-Forwarded-For', ['198.51.100.9', '127.0.0.2', '']),
                        ('Forwarded', ['for=198.51.100.9', 'for=127.0.0.2', ''])]:
        for index, value in enumerate(values):
            http(app, prefix + '-audit-refused-' + key + '-' + str(index), '/api/audit', 403,
                 [(key, value)])


try:
    check('baseline product tree still current', tree() == SOURCE['srcMainTree'], tree())
    save('artifact', {'mainSha': SOURCE['mainSha'], 'srcMainTree': SOURCE['srcMainTree'],
                     'jarSha256': hashlib.sha256(JAR.read_bytes()).hexdigest(),
                     'jar': str(JAR), 'scratch': str(SCRATCH), 'startedAt': dt.datetime.now(dt.timezone.utc).isoformat()})
    plain = App('direct')
    created = create(plain, 'direct-create')
    code = created['json']['code']
    audit_cases(plain, 'direct')
    browser = [('User-Agent', 'Mozilla/5.0 QA06-BROWSER-CANARY'),
               ('Referer', 'https://ref.example/private-QA06-path?q=QA06-query#QA06-fragment')]
    http(plain, 'direct-click-A', '/' + code, 302, browser, peer='127.0.0.1')
    http(plain, 'direct-click-A-forged-forwarding', '/' + code, 302,
         browser + [('X-Forwarded-For', '203.0.113.7')], peer='127.0.0.1')
    http(plain, 'direct-click-B', '/' + code, 302, browser, peer='127.0.0.2')
    stats(plain, 'direct-stats', code, 3, 2)
    live = http(plain, 'live-openapi', '/v3/api-docs', 200)
    save('live-openapi', live['json'])
    check('live and committed API documents semantically equal',
          live['json'] == json.loads((OUT / 'committed-openapi.json').read_text()))
    plain.stop()

    proxy = App('trusted', ['--urlshort.rate-limit.trusted-proxies=127.0.0.1'])
    code = create(proxy, 'trusted-create')['json']['code']
    audit_cases(proxy, 'trusted')
    for suffix, forwarded in [('A', '203.0.113.7'), ('A-chain', '198.51.100.1, 203.0.113.7'),
                              ('B', '203.0.113.8')]:
        http(proxy, 'trusted-click-' + suffix, '/' + code, 302,
             browser + [('X-Forwarded-For', forwarded)], peer='127.0.0.1')
    stats(proxy, 'trusted-stats', code, 3, 2)
    proxy.stop()

    for name, extra in [('untrusted-budget', []),
                        ('trusted-budget', ['--urlshort.rate-limit.trusted-proxies=127.0.0.1'])]:
        app = App(name, ['--urlshort.rate-limit.create-per-minute=2'] + extra)
        def headers(index):
            forwarded = '203.0.113.7' if extra else '203.0.113.' + str(index + 7)
            return [('X-Forwarded-For', forwarded), ('Forwarded', 'for=203.0.113.88'),
                    ('X-Real-IP', '203.0.113.99')]
        for index in range(2):
            http(app, name + '-spent-' + str(index), '/api/links/zzzzzzzz', 404, headers(index))
        refused = http(app, name + '-429', '/api/links/zzzzzzzz', 429, headers(2))
        check(name + ' retry after positive integer', refused['headers'].get('retry-after', '').isdigit()
              and int(refused['headers']['retry-after']) >= 1, refused)
        if extra:
            http(app, name + '-unrelated-client', '/api/links/zzzzzzzz', 404,
                 [('X-Forwarded-For', '203.0.113.8')])
        app.stop()

    settings = [
        ('remote-ip', ['--server.tomcat.remoteip.remote-ip-header=x-forwarded-for']),
        ('protocol', ['--server.tomcat.remoteip.protocol-header=x-forwarded-proto']),
        ('both-remoteip', ['--server.tomcat.remoteip.remote-ip-header=x-forwarded-for',
                           '--server.tomcat.remoteip.protocol-header=x-forwarded-proto']),
        ('native', ['--server.forward-headers-strategy=native']),
        ('framework', ['--server.forward-headers-strategy=framework'])]
    for name, overrides in settings:
        app = App(name, overrides)
        create(app, name + '-create-canary')
        for method in ['GET', 'HEAD']:
            for suffix, headers in [('plain', []), ('forged', [('X-Forwarded-For', '127.0.0.2')])]:
                http(app, name + '-audit-' + method + '-' + suffix, '/api/audit', 403, headers, method)
        app.stop()
    cloud = App('cloud', ['--spring.main.cloud-platform=kubernetes'])
    http(cloud, 'cloud-pinned-audit-admitted', '/api/audit', 200)
    http(cloud, 'cloud-pinned-audit-refused', '/api/audit', 403, [('X-Forwarded-For', '127.0.0.2')])
    cloud.stop()

    joins = []
    for request in REQUESTS:
        events = []
        for line in (OUT / (request['app'] + '.jsonl')).read_text().splitlines():
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            if event.get('requestId') == request['headers']['x-request-id']:
                events.append(event)
        completed = [event for event in events if event.get('message') == 'request completed']
        check(request['name'] + ' one matching request log', len(completed) == 1
              and completed[0]['status'] == request['status'], events)
        joins.append({'name': request['name'], 'requestId': request['headers']['x-request-id'], 'events': events})
    save('request-log-joins', joins)
    for app in APPS:
        raw = app.logpath.read_text()
        check(app.name + ' log identity/browsing canaries private', not any(canary in raw for canary in
              ['203.0.113.7', '203.0.113.8', '198.51.100.9', '127.0.0.2',
               'QA06-BROWSER-CANARY', 'private-QA06-path', 'QA06-query', 'QA06-fragment']), None)
    check('no intervening main product change', tree() == SOURCE['srcMainTree'], tree())
    save('summary', {'pass': True, 'mainSha': SOURCE['mainSha'], 'jarSha256': hashlib.sha256(JAR.read_bytes()).hexdigest(),
                     'currentMainSha': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                     'srcMainTree': tree(), 'requests': len(REQUESTS), 'assertions': len(CHECKS),
                     'appsStopped': len(APPS), 'finishedAt': dt.datetime.now(dt.timezone.utc).isoformat()})
    print(json.dumps(json.loads((OUT / 'summary.json').read_text())))
finally:
    for app in APPS:
        if app.process.poll() is None:
            app.stop()
