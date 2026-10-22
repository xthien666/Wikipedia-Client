"""TV3 real-WebView acceptance. Requires installed debug + androidTest APKs; restores app data."""
import argparse, base64, datetime, hashlib, json, pathlib, re, subprocess, sys, time, xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding='utf-8')
p=argparse.ArgumentParser()
p.add_argument('--adb',default='adb'); p.add_argument('--serial',required=True)
p.add_argument('--output',default='build/tv3-device'); a=p.parse_args()
out=pathlib.Path(a.output); out.mkdir(parents=True,exist_ok=True)
pkg='com.example.wikipediaclient'
def adb(*args,data=None,check=True,timeout=60):
    r=subprocess.run([a.adb,'-s',a.serial,*args],input=data,capture_output=True,timeout=timeout)
    if check and r.returncode: raise RuntimeError(r.stderr.decode('utf8','replace')+r.stdout.decode('utf8','replace'))
    return r

def shell(*args): return adb('shell',*args).stdout.decode('utf8','replace').strip()
def files():
    text=shell('run-as',pkg,'find','.', '-type','f')
    text='\n'.join(line.removeprefix('./') for line in text.splitlines())
    return [s for s in text.splitlines() if re.fullmatch(r'(databases|shared_prefs)/[\w.\-]+',s)]
def stop(): shell('am','force-stop',pkg)
def tree():
    raw=adb('exec-out','uiautomator','dump','/dev/tty').stdout.decode('utf8','replace')
    start=raw.find('<?xml'); end=raw.rfind('</hierarchy>')
    if start<0 or end<0: raise RuntimeError(raw)
    return raw[start:end+12]
backup={}; rotation={}; report={}; failure=None; backup_complete=False
try:
    stop()
    for name in files(): backup[name]=adb('exec-out','run-as',pkg,'cat',name).stdout
    (out/'data-backup.json').write_text(json.dumps({n:base64.b64encode(b).decode() for n,b in backup.items()}))
    for key in ('accelerometer_rotation','user_rotation'): rotation[key]=shell('settings','get','system',key)
    backup_complete=True
    crash_before=adb('logcat','-b','crash','-d').stdout
    apk=shell('pm','path',pkg).splitlines()[0].removeprefix('package:')
    apk_sha=hashlib.sha256(adb('exec-out','cat',apk).stdout).hexdigest().upper()
    r=adb('shell','am','instrument','-w',pkg+'.test/com.example.wikipediaclient.ReaderInstrumentation',timeout=260)
    (out/'instrumentation.txt').write_bytes(r.stdout+r.stderr)
    raw=adb('exec-out','run-as',pkg,'cat','files/tv3-qa/report.json').stdout
    report=json.loads(raw)
    report.update(serial=a.serial,apkSha256=apk_sha,started=datetime.datetime.now().astimezone().isoformat())
    for name in shell('run-as',pkg,'ls','files/tv3-qa').splitlines():
        if re.fullmatch(r'[\w\-]+\.png',name): (out/name).write_bytes(adb('exec-out','run-as',pkg,'cat','files/tv3-qa/'+name).stdout)
    if report.get('result')!='PASS': raise AssertionError(report.get('failure',r.stdout.decode('utf8','replace')))
    for check in report['checks']: print('PASS:',check,flush=True)
    # The runner leaves A visible. Background + actual am kill must remove its process.
    shell('am','start','-W','-n',pkg+'/.MainActivity'); time.sleep(3)
    # Instrumentation.finish ends its activity. Open a fresh real article through visible UI.
    before=tree(); root=ET.fromstring(before)
    sample=next((n for n in root.iter('node') if n.get('resource-id')==pkg+':id/open_sample'),None)
    if sample is not None:
        x1,y1,x2,y2=map(int,re.findall(r'\d+',sample.get('bounds')))
        shell('input','tap',str((x1+x2)//2),str((y1+y2)//2))
    for attempt in range(20):
        before=tree(); root=ET.fromstring(before)
        article=next((n for n in root.iter('node') if n.get('resource-id')==pkg+':id/article_title'),None)
        share=next((n for n in root.iter('node') if n.get('resource-id')==pkg+':id/action_share'),None)
        if article is not None and share is not None and share.get('enabled')=='true': break
        time.sleep(1)
    else: raise AssertionError('Real article not ready before process-death check')
    expected_title=article.get('text')
    (out/'before-process-death.xml').write_text(before,encoding='utf8')
    shell('input','keyevent','3'); time.sleep(1)
    for attempt in range(15):
        shell('am','kill',pkg); time.sleep(1)
        if not adb('shell','pidof',pkg,check=False).stdout.strip(): break
    else: raise AssertionError('am kill did not terminate app')
    shell('am','start','-W','-n',pkg+'/.MainActivity')
    for attempt in range(20):
        after=tree(); root=ET.fromstring(after)
        article=next((n for n in root.iter('node') if n.get('resource-id')==pkg+':id/article_title'),None)
        share=next((n for n in root.iter('node') if n.get('resource-id')==pkg+':id/action_share'),None)
        if article is not None and article.get('text')==expected_title and share is not None and share.get('enabled')=='true': break
        time.sleep(1)
    else: raise AssertionError('Article did not restore and render after process death')
    (out/'after-process-death.xml').write_text(after,encoding='utf8')
    (out/'reader-process-restored.png').write_bytes(adb('exec-out','screencap','-p').stdout)
    report['checks'].append('T15 real process death restores article and reloads HTML')
    crash=adb('logcat','-b','crash','-d').stdout
    (out/'crash-log.txt').write_bytes(crash)
    if crash!=crash_before: raise AssertionError('Crash buffer changed during reader QA')
    report['checks'].append('No crash during reader acceptance')
except BaseException as exc:
    failure=exc; report.update(result='FAIL',hostFailure=str(exc))
finally:
    try:
        if backup_complete:
            stop()
            for name in files(): shell('run-as',pkg,'rm','-f',name)
            for name,data in backup.items():
                shell('run-as',pkg,'mkdir','-p',name.split('/')[0])
                adb('shell','run-as',pkg,'tee',name,data=data)
            restored={n:adb('exec-out','run-as',pkg,'cat',n).stdout for n in files()}
            report['sqliteAndPreferencesRestored']=restored==backup
            if restored!=backup: raise AssertionError('App data restore mismatch')
            for key,value in rotation.items():
                if value=='null': shell('settings','delete','system',key)
                else: shell('settings','put','system',key,value)
            report['historyAndRotationRestored']=True
            shell('am','start','-W','-n',pkg+'/.MainActivity')
    except Exception as exc:
        failure=exc; report.update(result='FAIL',restoreFailure=str(exc))
    report['checks']=[{'name':c,'result':'PASS'} if isinstance(c,str) else c for c in report.get('checks',[])]
    (out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
if failure: raise failure
print('PASS:',len(report['checks']),'checks; app data restored; APK',report['apkSha256'])
