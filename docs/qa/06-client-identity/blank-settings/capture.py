"""Actual unmodified jars: AC-9 empty/whitespace remote-IP settings."""
from pathlib import Path
import datetime as dt
import hashlib
import json

ROOT = Path(__file__).resolve().parents[4]
parent = Path(__file__).resolve().parent
OUT = parent / ('run-' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'))
OUT.mkdir()
source = ROOT / 'docs/qa/06-client-identity/after/capture.py'
namespace = {'__file__': str(source)}
exec(compile(source.read_text().split('\ntry:\n')[0], str(source), 'exec'), namespace)
namespace['OUT'] = OUT
App, http, create, check, save = [namespace[name] for name in ['App', 'http', 'create', 'check', 'save']]
jars = {'baseline': Path(json.loads((ROOT / 'docs/qa/06-client-identity/before/artifact.json').read_text())['jar']),
        'candidate': ROOT / '.worktrees/06-client-identity/build/libs/urlshort.jar'}
settings = [('remote-empty', ['--server.tomcat.remoteip.remote-ip-header=']),
            ('remote-space', ['--server.tomcat.remoteip.remote-ip-header=   ']),
            ('protocol-empty', ['--server.tomcat.remoteip.protocol-header=']),
            ('protocol-space', ['--server.tomcat.remoteip.protocol-header=   ']),
            ('both-empty', ['--server.tomcat.remoteip.remote-ip-header=', '--server.tomcat.remoteip.protocol-header=']),
            ('both-space', ['--server.tomcat.remoteip.remote-ip-header=   ', '--server.tomcat.remoteip.protocol-header=   '])]
try:
    save('custody', {'candidate': 'fb63a88a9b92c1fec97ba74686af1a2f30304160',
                    'jars': {lane: {'path': str(jar), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()} for lane, jar in jars.items()},
                    'helperSourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                    'settings': settings, 'fixture': 'No injected configuration, classes or servlet filter; java -jar only.'})
    for lane, jar in jars.items():
        namespace['JAR'] = jar
        for name, values in settings:
            app = App(lane + '-' + name, values)
            created = create(app, app.name + '-create')
            before = http(app, app.name + '-before', '/api/audit', 200)['json']
            check(app.name + ' actual create audit effect', len(before['items']) == 1
                  and before['items'][0]['entityId'] == created['json']['code']
                  and before['items'][0]['requestId'] == created['headers']['x-request-id'], before)
            http(app, app.name + '-HEAD-plain', '/api/audit', 200, method='HEAD')
            for method in ['GET', 'HEAD']:
                http(app, app.name + '-' + method + '-forged', '/api/audit', 403,
                     [('X-Forwarded-For', '127.0.0.2')], method)
            after = http(app, app.name + '-after', '/api/audit', 200)['json']
            check(app.name + ' no audit mutation', before == after, after)
            app.stop()
    joins = []
    for request in namespace['REQUESTS']:
        events = [json.loads(line) for line in (OUT / (request['app'] + '.jsonl')).read_text().splitlines() if line.startswith('{')]
        matching = [event for event in events if event.get('requestId') == request['headers']['x-request-id']]
        check(request['name'] + ' correlation', len(matching) == 1 and matching[0]['message'] == 'request completed'
              and matching[0]['status'] == request['status'], matching)
        joins.append({'app': request['app'], 'name': request['name'], 'events': matching})
    save('request-log-joins', joins)
    save('summary', {'pass': True, 'requests': len(namespace['REQUESTS']), 'assertions': len(namespace['CHECKS']),
                     'appsStopped': len(namespace['APPS']), 'output': str(OUT.relative_to(ROOT))})
    (parent / 'last-run.txt').write_text(str(OUT.relative_to(ROOT)) + '\n')
    print((OUT / 'summary.json').read_text())
finally:
    for app in namespace['APPS']:
        if app.process.poll() is None:
            app.stop()
