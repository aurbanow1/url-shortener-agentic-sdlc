// Release evidence only: every HTTP exchange uses the loopback-only scripts/http.
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import assert from 'node:assert/strict';
const [base, mode] = process.argv.slice(2);
assert(['trusted', 'default'].includes(mode));
assert(new URL(base).hostname === '127.0.0.1');
const root = 'missions/03-ambiguous-analytics/release';
const dir = `${root}/http-${mode}-50ad9c3`;
fs.mkdirSync(dir, {recursive:true});
const exchanges = [];
function request(label, method, path, headers = {}, body) {
  const args = ['-sS', '-i', '--max-time', '10', '-X', method];
  for (const [k,v] of Object.entries(headers)) args.push('-H', `${k}: ${v}`);
  if (body !== undefined) args.push('-H','Content-Type: application/json','-d',JSON.stringify(body));
  args.push(base+path);
  const wire = execFileSync('scripts/http',args,{encoding:'utf8',timeout:15000});
  fs.writeFileSync(`${dir}/${label}.txt`,wire);
  const boundary = wire.indexOf('\r\n\r\n');
  assert(boundary >= 0, 'HTTP header separator');
  const head = wire.slice(0,boundary), text = wire.slice(boundary+4);
  const status = Number(head.match(/^HTTP\/\S+ (\d+)/)[1]);
  const requestId = head.match(/^X-Request-Id: (.+)$/im)?.[1].trim();
  exchanges.push({label,method,path,status,requestId});
  return {status,text,json:() => JSON.parse(text)};
}
const health=request('readiness','GET','/actuator/health/readiness');
assert.equal(health.status,200); assert.equal(health.json().status,'UP');
const create=request('create','POST','/api/links',{}, {url:'https://example.com/installed-analytics'});
assert.equal(create.status,201); const {code}=create.json();
const meter=() => request(`recorded-${exchanges.length}`,'GET','/actuator/metrics/urlshort.clicks.recorded').json().measurements.find(x => x.statistic==='COUNT').value;
const before=meter();
const peers=['203.0.113.41','203.0.113.41','203.0.113.42','203.0.113.43'];
for(let i=0;i<peers.length;i++) {
  const r=request(`redirect-${i}`,'GET',`/${code}`,{'X-Forwarded-For':peers[i],'User-Agent':i===2?'Googlebot/2.1':'Mozilla/5.0','Referer':'https://marketing.example/private?q=canary'});
  assert.equal(r.status,302);
}
let stats;
for(let i=0;i<50;i++) {
  const r=request(`stats-${i}`,'GET',`/api/links/${code}/stats`); assert.equal(r.status,200); stats=r.json();
  if(stats.totalClicks===4) break;
  await new Promise(resolve=>setTimeout(resolve,100));
}
assert.deepEqual(Object.keys(stats).sort(),['clicksPerDay','code','topReferrers','totalClicks']);
assert.equal(stats.totalClicks,4); assert.equal(stats.clicksPerDay.length,1);
const day=stats.clicksPerDay[0];
assert.deepEqual(Object.keys(day).sort(),['botClicks','clicks','date','uniqueVisitors']);
assert.equal(day.clicks,4); assert.equal(day.uniqueVisitors,mode==='trusted'?3:1); assert.equal(day.botClicks,1);
assert.deepEqual(stats.topReferrers,[{referrer:'https://marketing.example',clicks:4}]);
assert(!/203\.0\.113|private|canary|clientHash|client_hash/.test(JSON.stringify(stats)));
assert.equal(meter()-before,4);
const scrape=request('prometheus','GET','/actuator/prometheus'); assert.equal(scrape.status,200);
const clickLines=scrape.text.split('\n').filter(x=>x.startsWith('urlshort_clicks_'));
assert.equal(clickLines.filter(x=>x.startsWith('urlshort_clicks_recorded_total ')).length,1);
const lost=clickLines.filter(x=>x.startsWith('urlshort_clicks_lost_total{'));
assert.equal(lost.length,5);
for(const line of lost) {assert.match(line,/^urlshort_clicks_lost_total\{reason="[^"]+"\} 0\.0$/);}
const api=request('openapi','GET','/v3/api-docs'); assert.equal(api.status,200);
assert.deepEqual(api.json(),JSON.parse(fs.readFileSync('docs/api/openapi.json','utf8')));
const result={candidate:'50ad9c3ab9e65baa4100ede1772b514322957fa5',base,mode,code,stats,recordedDelta:4,clickSeries:clickLines,openapiEqual:true,exchanges};
fs.writeFileSync(`${root}/installed-${mode}-50ad9c3.json`,JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify({mode,stats,recordedDelta:4,clickSeries:clickLines,openapiEqual:true,exchanges:exchanges.length},null,2));
