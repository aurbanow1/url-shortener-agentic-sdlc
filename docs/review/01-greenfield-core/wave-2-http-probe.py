"""Independent integrated journey on the unmodified 8e9c065 product tree."""
import hashlib
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import tempfile
import time
import zipfile

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
JAVA = Path(os.environ.get('URLSHORT_JAVA_HOME', '/opt/homebrew/opt/openjdk@21')) / 'bin/java'
work = Path(tempfile.mkdtemp(prefix='urlshort-wave2-'))
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0))
    port = sock.getsockname()[1]
base = f'http://127.0.0.1:{port}'
database = f'jdbc:h2:file:{work}/data;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE'
jar = ROOT / 'build/libs/urlshort.jar'
records = []

def request(label, path, method='GET', client='198.51.100.100', body=None, extra=None, head=False):
    headers = work / 'headers.txt'
    response = work / 'body.txt'
    args = [str(ROOT / 'scripts/http'), '-sS', '--max-time', '10', '--path-as-is',
            '-D', str(headers), '-o', str(response), '-w', '%{http_code}',
            '-H', 'X-Forwarded-For: ' + client,
            '-H', 'User-Agent: wave2-private-UA-canary',
            '-H', 'Referer: https://source.example/private-path?private-query-canary',
            '-H', 'X-Request-Id: wave2-inbound-id-canary']
    args += ['-I'] if head else ['-X', method]
    if body is not None:
        args += ['-H', 'Content-Type: application/json', '--data-binary', body]
    for key, value in (extra or {}).items():
        args += ['-H', key + ': ' + value]
    result = subprocess.run(args + [base + path], capture_output=True, text=True)
    status = int(result.stdout or '0')
    text = response.read_text() if response.exists() else ''
    h = {}
    for line in headers.read_text().splitlines() if headers.exists() else []:
        if ':' in line:
            k, v = line.split(':', 1)
            h[k.lower()] = v.strip()
    record = dict(label=label, path=path, method='HEAD' if head else method, client=client,
                  status=status, exit=result.returncode, headers=h, body=text)
    records.append(record)
    if result.returncode:
        raise AssertionError(record)
    assert h.get('x-request-id') and h['x-request-id'] != 'wave2-inbound-id-canary'
    return record

log_path = OUT / 'wave-2-http-log-8e9c065.jsonl'
log = log_path.open('w')
process = subprocess.Popen([str(JAVA), '-jar', str(jar), '--server.address=127.0.0.1',
    f'--server.port={port}', '--spring.datasource.url=' + database,
    '--urlshort.rate-limit.create-per-minute=4', '--urlshort.rate-limit.redirect-per-minute=3',
    '--urlshort.rate-limit.trusted-proxies=127.0.0.1'], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
summary = {'target': '8e9c065', 'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
           'configuration': {'create': 4, 'redirect': 3, 'trustedProxy': '127.0.0.1'},
           'work': str(work), 'port': port}
try:
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        ready = subprocess.run([str(ROOT / 'scripts/http'), '-fsS', base + '/actuator/health/readiness'],
                               capture_output=True, text=True)
        if ready.returncode == 0:
            break
        assert process.poll() is None, 'app exited before readiness'
        time.sleep(.1)
    else:
        raise AssertionError('readiness timeout')
    target = 'https://example.com/wave2?private-target-canary=1'
    body = json.dumps({'url': target})
    key = {'Idempotency-Key': 'wave2-idempotency-canary', 'Host': 'forged.invalid'}
    created = request('create', '/api/links', 'POST', body=body, extra=key)
    assert created['status'] == 201
    link = json.loads(created['body'])
    code = link['code']
    assert link['shortUrl'] == 'http://localhost:8080/' + code
    replay = request('replay', '/api/links', 'POST', body=body, extra=key)
    assert replay['status'] == 201 and replay['body'] == created['body']
    invalid = request('validation-errors', '/api/links', 'POST', body='{"url":"javascript:wave2"}')
    problem = json.loads(invalid['body'])
    assert invalid['status'] == 400 and problem['errors'][0]['rule'] == 'scheme'
    read = request('read', '/api/links/' + code)
    assert read['status'] == 200 and read['body'] == created['body']
    rejected = request('limit-before-body-cap', '/api/links', 'POST', body='x' * 17000)
    assert rejected['status'] == 429 and 'location' not in rejected['headers']
    assert int(rejected['headers']['retry-after']) >= 1 and 'errors' not in json.loads(rejected['body'])

    for n in range(2):
        r = request('redirect-a-' + str(n), '/' + code, client='198.51.100.21')
        assert r['status'] == 302 and r['headers']['location'] == target
        assert r['headers']['cache-control'] == 'no-store'
    r = request('head-does-not-click', '/' + code, client='198.51.100.21', head=True)
    assert r['status'] == 302
    r = request('redirect-over-limit', '/' + code, client='198.51.100.21')
    assert r['status'] == 429 and 'location' not in r['headers']
    r = request('other-forwarded-client-independent', '/' + code, client='198.51.100.22')
    assert r['status'] == 302
    for n in range(10):
        stats = request('stats-' + str(n), '/api/links/' + code + '/stats', client=f'192.0.2.{10+n}')
        value = json.loads(stats['body'])
        if value['totalClicks'] == 3:
            break
        time.sleep(.05)
    assert value['totalClicks'] == 3 and sum(x['clicks'] for x in value['clicksPerDay']) == 3
    assert value['topReferrers'] == [{'referrer': 'https://source.example', 'clicks': 3}]
    assert set(value) == {'code', 'totalClicks', 'clicksPerDay', 'topReferrers'}
    r = request('retire', '/api/links/' + code, 'DELETE', client='192.0.2.50')
    assert r['status'] == 204
    r = request('retired-redirect', '/' + code, client='192.0.2.51')
    assert r['status'] == 410
    r = request('retired-stats', '/api/links/' + code + '/stats', client='192.0.2.52')
    assert json.loads(r['body']) == value
    r = request('bad-resource-path-private', '/actuator/../wave2-path-203.0.113.77')
    assert r['status'] == 404
    r = request('prometheus', '/actuator/prometheus')
    assert r['status'] == 200 and code not in r['body'] and target not in r['body']
    assert 'urlshort_ratelimit_rejections_total{budget="create"} 1.0' in r['body']
    assert 'urlshort_ratelimit_rejections_total{budget="redirect"} 1.0' in r['body']
    schema = request('openapi', '/v3/api-docs')
    document = json.loads(schema['body'])
    assert document == json.loads((ROOT / 'docs/api/openapi.json').read_text())
    assert all('429' in operation['responses'] for path in document['paths'].values() for operation in path.values())
    summary['problemSchemaOmitsErrors'] = 'errors' not in document['components']['schemas']['ProblemDetail']['properties']
    summary['httpChecks'] = 'PASS'
finally:
    process.send_signal(signal.SIGTERM)
    process.wait(timeout=25)
    log.close()
    (OUT / 'wave-2-http-8e9c065.json').write_text(json.dumps(records, indent=2) + '\n')

lines = [json.loads(line) for line in log_path.read_text().splitlines() if line.startswith('{')]
for record in records:
    rid = record['headers']['x-request-id']
    completions = [event for event in lines if event.get('requestId') == rid and event.get('message') == 'request completed']
    assert len(completions) == 1 and completions[0]['status'] == record['status'], record['label']
for canary in ['wave2-private-UA-canary', 'private-query-canary', 'private-target-canary',
               'wave2-idempotency-canary', 'wave2-inbound-id-canary', '203.0.113.77',
               '198.51.100.21', '198.51.100.22', 'https://source.example']:
    assert canary not in log_path.read_text(), canary
summary['logCorrelationAndCanaries'] = 'PASS'
with zipfile.ZipFile(jar) as z:
    h2name = next(n for n in z.namelist() if n.startswith('BOOT-INF/lib/h2-'))
    h2 = work / 'h2.jar'
    h2.write_bytes(z.read(h2name))
sql = ('SELECT COUNT(*) AS clicks, COUNT(DISTINCT client_hash) AS distinct_hashes, '
       'COUNT(DISTINCT referrer) AS origins FROM click; '
       'SELECT action, COUNT(*) AS rows FROM audit_log GROUP BY action; '
       'SELECT code, retired_at IS NOT NULL AS retired FROM link;')
db = subprocess.run([str(JAVA), '-cp', str(h2), 'org.h2.tools.Shell', '-url', database,
                     '-user', 'sa', '-password', '', '-sql', sql], capture_output=True, text=True)
assert db.returncode == 0 and 'Error' not in db.stdout
(OUT / 'wave-2-storage-8e9c065.txt').write_text(db.stdout)
summary['storageOutput'] = db.stdout
summary['stopped'] = process.poll() is not None
summary['requestCount'] = len(records)
(OUT / 'wave-2-http-summary-8e9c065.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
