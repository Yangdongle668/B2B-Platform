#!/usr/bin/env python3
"""Built SSR/Element Plus shell checks; auth failures are deliberately injected in this UI suite."""
from pathlib import Path
import contextlib,json,os,re,socket,subprocess,time,urllib.request
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'.reports/frontend';OUT.mkdir(parents=True,exist_ok=True)
@contextlib.contextmanager
def server(command,cwd,extra=None):
 with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
 env=os.environ.copy();env.update(PNPM_HOME=str(ROOT/'.dev/pnpm-home'),NUXT_TELEMETRY_DISABLED='1',NITRO_HOST='127.0.0.1',NITRO_PORT=str(port));env.update(extra or {})
 command=[str(x).replace('{port}',str(port)) for x in command]
 with (OUT/(cwd.name+'-server.log')).open('w') as log:
  proc=subprocess.Popen(command,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT)
  try:
   client=urllib.request.build_opener(urllib.request.ProxyHandler({}));url='http://127.0.0.1:'+str(port)
   for _ in range(120):
    try:client.open(url,timeout=1);break
    except OSError:
     if proc.poll() is not None:raise RuntimeError('Server exited: '+cwd.name)
     time.sleep(.1)
   else:raise TimeoutError(cwd.name)
   yield url,client
  finally:
   proc.terminate()
   try:proc.wait(timeout=10)
   except subprocess.TimeoutExpired:proc.kill();proc.wait()
def main():
 result={'viewports':[],'auth_scope':'UI injected failure handling; real API/session/MFA is tested by backend integration suite'}
 texts=json.loads((ROOT/'frontend/packages/themes/industrial/texts/zh-CN.json').read_text())
 admin_texts=dict(re.findall(r"(\w+): '([^']*)'",(ROOT/'frontend/apps/admin/src/texts/index.ts').read_text()))
 with sync_playwright() as p:
  browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or __import__('shutil').which('chromium'))
  try:
   with server(['node','.output/server/index.mjs'],ROOT/'frontend/apps/website') as (url,client):
    response=client.open(url+'/zh/');html=response.read().decode();assert texts['hero.title'] in html and response.headers.get('Cache-Control')=='no-store';result['ssr_without_js']=True
    for width in (1440,768,390,360):
     page=browser.new_page(viewport={'width':width,'height':900});errors=[];page.on('pageerror',lambda e:errors.append(str(e)));page.goto(url+'/zh/',wait_until='networkidle');expect(page.get_by_role('heading',level=1)).to_have_text(texts['hero.title']);assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth');page.add_script_tag(path=str(ROOT/'node_modules/axe-core/axe.min.js'));audit=page.evaluate("async()=>{const r=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}});return r.violations.map(v=>({id:v.id,impact:v.impact}))}");assert not audit,audit;page.screenshot(path=str(OUT/f'website-{width}.png'),full_page=True);page.get_by_role('link',name=texts['hero.action']).click();expect(page.get_by_role('heading',level=1)).to_have_text(texts['gallery.title']);assert not errors,errors;result['viewports'].append({'width':width,'no_overflow':True,'axe_violations':audit,'gallery_navigation':True});page.close()
   with server(['pnpm','exec','vite','preview','--host','127.0.0.1','--port','{port}','--strictPort'],ROOT/'frontend/apps/admin') as (url,_):
    page=browser.new_page(viewport={'width':1280,'height':900});page.goto(url,wait_until='networkidle');expect(page.get_by_role('heading',level=1)).to_have_text(admin_texts['login']);page.add_script_tag(path=str(ROOT/'node_modules/axe-core/axe.min.js'));audit=page.evaluate("async()=>{const r=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}});return r.violations.map(v=>({id:v.id,impact:v.impact}))}");assert not audit,audit
    page.route('**/api/admin/v1/system/auth/csrf',lambda route:route.fulfill(status=200,content_type='application/json',body='{"token":"test-token"}'));page.route('**/api/admin/v1/system/auth/login',lambda route:route.fulfill(status=401,content_type='application/problem+json',body='{"code":"CORE_NOT_AUTHENTICATED","detail":"internal-sensitive-detail"}'));page.get_by_label(admin_texts['email'],exact=True).fill('editor@example.test');page.get_by_label(admin_texts['password'],exact=True).fill('incorrect-password');page.get_by_role('button',name=admin_texts['submit'],exact=True).click();expect(page.get_by_role('alert')).to_contain_text(admin_texts['failed']);assert 'internal-sensitive-detail' not in page.locator('body').inner_text();page.screenshot(path=str(OUT/'admin-error.png'),full_page=True);result['admin_failure_state']=True;result['admin_axe_violations']=audit;page.close()
  finally:browser.close()
 (OUT/'browser.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
