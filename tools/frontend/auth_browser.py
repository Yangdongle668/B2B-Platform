#!/usr/bin/env python3
"""Real HTTPS UI -> API -> MySQL/Redis MFA smoke against this checkout's local stack."""
import base64,hashlib,hmac,json,os,secrets,shutil,struct,time
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[2];STATE=ROOT/'.dev/stack/state.json'
def totp(secret):
 key=base64.b32decode(secret+'='*((-len(secret))%8));message=struct.pack('>Q',int(time.time())//30);digest=hmac.new(key,message,hashlib.sha1).digest();offset=digest[-1]&15;return str((int.from_bytes(digest[offset:offset+4],'big')&0x7fffffff)%1000000).zfill(6)
def save(value):STATE.write_text(json.dumps(value,indent=2)+'\n');STATE.chmod(0o600)
def main():
 state=json.loads(STATE.read_text());result={'transport':'real local HTTPS','injected_responses':False}
 with sync_playwright() as p:
  browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or shutil.which('chromium'))
  try:
   context=browser.new_context(ignore_https_errors=True);page=context.new_page();page.goto('https://localhost:8443/admin/',wait_until='networkidle')
   page.get_by_label('邮箱',exact=True).fill('admin@local.test');page.get_by_label('密码',exact=True).fill(state.get('smokePassword',state['bootstrapPassword']));page.get_by_role('button',name='继续',exact=True).click();expect(page.get_by_role('heading',name='验证',exact=True)).to_be_visible()
   if page.get_by_role('button',name='绑定验证器',exact=True).is_visible():
    page.get_by_role('button',name='绑定验证器',exact=True).click();expect(page.get_by_role('img',name='用于绑定验证器的二维码')).to_be_visible();secret=page.locator('p code').inner_text();page.get_by_label('验证码',exact=True).fill(totp(secret));page.get_by_role('button',name='验证',exact=True).click();expect(page.get_by_role('heading',name='恢复码仅显示一次，请妥善保存')).to_be_visible();state['smokeRecoveryCodes']=page.locator('aside li code').all_inner_texts();save(state);result['qr_enrollment']=True
   else:
    code=state['smokeRecoveryCodes'].pop();save(state);page.get_by_label('验证码',exact=True).fill(code);page.get_by_role('button',name='验证',exact=True).click();result['single_use_recovery']=True
   if page.get_by_role('heading',name='修改密码',exact=True).is_visible():
    password=secrets.token_urlsafe(24);page.get_by_label('新密码',exact=True).fill(password);page.get_by_role('button',name='继续',exact=True).click();expect(page.get_by_role('heading',name='工作台',exact=True)).to_be_visible();state['smokePassword']=password;save(state);result['required_password_change']=True
   expect(page.get_by_role('heading',name='工作台',exact=True)).to_be_visible();cookie=next(c for c in context.cookies() if c['name']=='__Host-SESSION');assert cookie['secure'] and cookie['httpOnly'] and cookie['sameSite']=='Lax';result['secure_redis_session']=True
   page.get_by_role('button',name='退出登录',exact=True).click();expect(page.get_by_role('heading',name='登录',exact=True)).to_be_visible();response=context.request.get('https://localhost:8443/api/admin/v1/system/bootstrap');assert response.status==401;result['logout_revokes_access']=True
  finally:browser.close()
 out=ROOT/'.reports/frontend/auth-browser.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
