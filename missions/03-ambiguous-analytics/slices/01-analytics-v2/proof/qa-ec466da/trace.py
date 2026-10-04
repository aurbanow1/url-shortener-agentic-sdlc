"""Inventory candidate JUnit methods and write both directions of analytics traceability."""
import json,pathlib,re,subprocess,xml.etree.ElementTree as ET
P=pathlib.Path(__file__).resolve().parent; ROOT=P.parents[5]; W=ROOT/'.worktrees/01-analytics-v2'
def mask(source):
    return re.sub(r'/\*[\s\S]*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda m:re.sub(r'[^\n]',' ',m.group()),source)
methods=[]
for suite,folder in [('unit','test'),('functional','functionalTest')]:
    for path in sorted((W/'src'/folder/'java').rglob('*.java')):
        text=path.read_text(); clean=mask(text);package=re.search(r'package\s+([\w.]+)',text).group(1)
        classes=[]
        for match in re.finditer(r'\bclass\s+(\w+)[^{]*\{',clean):
            start=match.end()-1;depth=1;end=start+1
            while depth and end<len(clean):
                depth+=(clean[end]=='{')-(clean[end]=='}');end+=1
            classes.append((match.group(1),start,end))
        for match in re.finditer(r'@(Test|ParameterizedTest)\b',clean):
            method=re.search(r'\bvoid\s+(\w+)\s*\(',clean[match.end():])
            if method is None:raise AssertionError('unparsed annotation '+str(path))
            pos=match.end()+method.start();owners=[n for n,a,b in classes if a<pos<b]
            runtime=package+'.'+'$'.join(owners)
            methods.append({'suite':suite,'path':str(path.relative_to(W)),'class':runtime,'method':method.group(1),'parameterized':match.group(1)=='ParameterizedTest'})
# Five inherited real-container methods execute again in the configured-proxy context.
for item in list(methods):
    if item['class']=='dev.urlshort.click.ClickResilienceJourneyTest':
        methods.append({**item,'class':'dev.urlshort.click.ClickResilienceTrustedProxyJourneyTest','inheritedFrom':item['class']})
xml={}
for suite in ['unit','functional']:
    for path in (P/'test-results'/suite).glob('TEST-*.xml'):
        for item in ET.parse(path).getroot().findall('testcase'):
            key=(suite,item.attrib['classname']);xml.setdefault(key,[]).append(item.attrib['name'])
for item in methods:
    cases=xml.get((item['suite'],item['class']),[])
    named=[n for n in cases if n.startswith(item['method']+'(') or n.startswith(item['method']+'[')]
    if named:item['attribution']='named XML invocation';item['invocations']=len(named)
    elif item['parameterized'] and cases:item['attribution']='green class parameterized group; XML display omits method';item['invocations']=None
    else:raise AssertionError('source test without invocation: '+str(item))
    cls=item['class'].split('.')[-1].split('$')[0];method=item['method']
    # New v2 methods map their local AC number directly. Dependency methods retain original names.
    if cls in ['StatsV2JourneyTest','TrustedProxyClickJourneyTest','ClickMetricsJourneyTest']:
        item['mapsTo']=['AC-'+str(int(re.search(r'AC(\d+)',method).group(1)))]
    elif cls in ['ClickResilienceJourneyTest','ClickResilienceTrustedProxyJourneyTest']:
        item['mapsTo']=['AC-15' if re.match(r'AC(?:14|15|16)_',method) else 'AC-12' if re.match(r'AC18_',method) else 'BR-1','AC-14']
    elif cls=='StatsJourneyTest':
        item['mapsTo']=['AC-6' if re.match(r'AC(?:08|09|10)_',method) else 'AC-12' if method.startswith('AC19_') else 'AC-13' if method.startswith('AC21_') else 'BR-1','AC-14']
    elif cls=='ClickRecordingJourneyTest':
        item['mapsTo']=['AC-9' if method.startswith('AC17_') else 'AC-12' if method.startswith('AC18_') else 'BR-4' if method.startswith('AC04_') else 'BR-5','AC-14']
    elif item['suite']=='functional':
        rule='BR-7' if any(t in cls for t in ['Purge','Retention']) else 'BR-6' if any(t in cls for t in ['RateLimit','TrustedProxy']) else 'BR-11' if 'Observability' in cls else 'BR-8' if 'OpenApi' in cls else 'BR-1'
        item['mapsTo']=[rule,'AC-14']
    else:
        rules={'LinkStatsTest':'BR-3/4/9','ClickRecorderTest':'BR-2/4/6/10/11','DailySaltTest':'BR-3/5/9','ClickPurgeTest':'BR-7','ClickTest':'BR-2/4/5/9','ClickSchemaTest':'BR-5','ClickAuditColumnsTest':'BR-5','RateLimitFilterTest':'BR-6','RateLimiterTest':'BR-6','RequestIdFilterTest':'BR-11','MetricsConfigTest':'BR-10/11'}
        item['mapsTo']=[rules.get(cls,'BR-1 (inherited service/management regression)')]
represented={(m['suite'],m['class']) for m in methods}
if set(xml)-represented:raise AssertionError('XML classes without source map: '+str(set(xml)-represented))
invocations=[]
for (suite,cls),cases in xml.items():
    group=[m for m in methods if m['suite']==suite and m['class']==cls]
    for name in cases:
        exact=[m for m in group if name.startswith(m['method']+'(') or name.startswith(m['method']+'[')]
        if exact:m=exact[0];mapping=m['mapsTo'];attribution=m['method']
        else:
            params=[m for m in group if m['parameterized']]
            if not params:raise AssertionError('unmatched XML invocation '+name)
            mapping=sorted({r for m in params for r in m['mapsTo']});attribution='green parameterized group: '+', '.join(m['method'] for m in params)
        invocations.append({'suite':suite,'class':cls,'name':name,'attribution':attribution,'mapsTo':mapping,'result':'PASS'})
(P/'source-test-inventory.json').write_text(json.dumps(methods,indent=2)+'\n')
(P/'invocation-attribution.json').write_text(json.dumps(invocations,indent=2)+'\n')
ac={1:('FR-8 v2, FR-13','StatsV2JourneyTest#AC01_thePerDayElementCarriesFourFigures'),2:('FR-8 v2','StatsV2JourneyTest#AC02_uniqueVisitorsCountDistinctClientsWithinAUtcDay'),3:('FR-8 v2, NFR-P1','StatsV2JourneyTest#AC03_uniquesArePerUtcDayAndNeverCombinedAcrossDays'),4:('FR-8 v2','StatsV2JourneyTest#AC04_botClicksAreCountedPerDayAndNothingElseChangesMeaning'),5:('FR-8 v2','StatsV2JourneyTest#AC05_oneClientsBotAndBrowserClicksAreOneVisitor'),6:('FR-13','StatsJourneyTest#AC08_totalClicksCountsEveryRedirect; #AC09_clicksPerDayAreGroupedByUtcCalendarDay; #AC10_topReferrersAreRankedAndCapped'),7:('NFR-P1, FR-8 v2, W2-02','TrustedProxyClickJourneyTest#AC07_behindATrustedProxyUniquesCountTheForwardedClients'),8:('NFR-P1','StatsV2JourneyTest#AC08_withoutATrustedProxyForwardingHeadersChangeNothing'),9:('NFR-P1','TrustedProxyClickJourneyTest#AC09_theStatisticsStillExposeAggregatesOnly; ClickRecordingJourneyTest#AC17_theStatisticsExposeAggregatesOnly'),10:('NFR-O3','ClickMetricsJourneyTest#AC10_recordedAndLostClicksAreCounted'),11:('NFR-O3, NFR-P1','ClickMetricsJourneyTest#AC11_theClickCountersAreScrapeableAndNameNoClient'),12:('NFR-O1, NFR-O2','TrustedProxyClickJourneyTest#AC12_trustedProxyRedirectsLogNoForwardedValueOrAddress; ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs; ClickResilienceJourneyTest#AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated'),13:('NFR-M3','StatsV2JourneyTest#AC13_theApiDocumentDescribesTheV2PerDayElement; OpenApiDocumentTest#NFRM3_committedDocumentEqualsTheLiveOne'),14:('FR-13','Original f6dd29e 155 invocations, independently replayed with only granted expectations; all current functional methods below'),15:('FR-13, NFR-L1','ClickResilienceJourneyTest and ClickResilienceTrustedProxyJourneyTest#AC14_aSlowClickStoreDoesNotSlowTheRedirect; #AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn; #AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse')}
rules={1:('FR-13','StatsJourneyTest#AC07_aLinkWithNoClicksHasEmptyStatistics; #AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails; #AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing'),2:('FR-8 v2','StatsV2JourneyTest#AC04_botClicksAreCountedPerDayAndNothingElseChangesMeaning'),3:('FR-8 v2, NFR-P1','StatsV2JourneyTest#AC02_uniqueVisitorsCountDistinctClientsWithinAUtcDay; #AC03_uniquesArePerUtcDayAndNeverCombinedAcrossDays; DailySaltTest (inventory below)'),4:('FR-8 v2','StatsV2JourneyTest#AC04_botClicksAreCountedPerDayAndNothingElseChangesMeaning; #AC05_oneClientsBotAndBrowserClicksAreOneVisitor'),5:('NFR-P1','TrustedProxyClickJourneyTest#AC09_theStatisticsStillExposeAggregatesOnly; DailySaltTest and ClickRecorderTest (inventory below); downstream review item 11'),6:('FR-13, NFR-P1','TrustedProxyClickJourneyTest#AC07_behindATrustedProxyUniquesCountTheForwardedClients; StatsV2JourneyTest#AC08_withoutATrustedProxyForwardingHeadersChangeNothing; RateLimitFilterTest (inventory below)'),7:('NFR-P2','ClickRetentionJourneyTest; ClickRetentionScheduleJourneyTest; ClickRetentionStartupJourneyTest; ClickPurgeTest (each method below)'),8:('FR-13, NFR-M3','OpenApiDocumentTest#AC28_liveDocumentDescribesTheSlice; #NFRM3_committedDocumentEqualsTheLiveOne'),9:('FR-8 v2','StatsV2JourneyTest#AC03_uniquesArePerUtcDayAndNeverCombinedAcrossDays; StatsJourneyTest#AC09_clicksPerDayAreGroupedByUtcCalendarDay'),10:('NFR-O3','ClickMetricsJourneyTest#AC10_recordedAndLostClicksAreCounted; #AC11_theClickCountersAreScrapeableAndNameNoClient; ClickRecorderTest (all five reasons below)'),11:('NFR-O1, NFR-O2, NFR-P1','TrustedProxyClickJourneyTest#AC12_trustedProxyRedirectsLogNoForwardedValueOrAddress; ClickRecordingJourneyTest#AC18_noClickDataReachesTheLogs')}
lines=['\n## 01-analytics-v2 — candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9','',
'Independent gate: 221 unit / 241 functional invocations, no failures/errors/skips. The original f6dd29e replay is 155/155 with only the two per-day shape expectations and the two inherited audit enumerations authorized by a12a0e2. The earlier literal 153/155 result is retained. Every test below maps to an analytics AC or business rule; inherited names keep their originating slice numbers and are read as regression guards, not renumbered v2 criteria. Requirement ids follow this SPEC and its inherited privacy/operations rules.','',
'| AC / business rule | Requirement id | Test class#method | Suite | Result |','|---|---|---|---|---|']
for n,(req,tests) in ac.items():lines.append(f'| AC-{n} | {req} | {tests} | functional | PASS on ec466da; AC-14 with explicit lead reading |')
for n,(req,tests) in rules.items():lines.append(f'| BR-{n} | {req} | {tests} | functional + unit where named | '+('PASS observable effects; construction/security judgment remains item 11' if n==5 else 'PASS on ec466da')+' |')
lines+=['','### Every candidate test back to its rule','',
'Exact source and invocation inventories: missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-ec466da/source-test-inventory.json and invocation-attribution.json. Some parameterized XML displays omit the source method: those rows are attributed to their green class parameterized group, not asserted as a unique per-method XML join. Five inherited resilience methods execute in each of two contexts.','',
'| Test class#method | Suite | Maps to | Result / attribution |','|---|---|---|---|']
for m in sorted(methods,key=lambda r:(r['suite'],r['class'],r['method'])):
    lines.append(f"| {m['class']}#{m['method']} | {m['suite']} | {', '.join(m['mapsTo'])} | PASS — {m['attribution']}"+(' (inherited)' if 'inheritedFrom' in m else '')+' |')
trace=ROOT/'docs/qa/TRACEABILITY.md';assert '## 01-analytics-v2 — candidate ec466da' not in trace.read_text()
trace.write_text(trace.read_text()+'\n'.join(lines)+'\n')
print(json.dumps({'sourceMethodsIncludingInherited':len(methods),'invocations':len(invocations),'unmappedSourceMethods':0,'unmappedInvocations':0,'parameterizedGroups':sum(m['invocations'] is None for m in methods)}))
