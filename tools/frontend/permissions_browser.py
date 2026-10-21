#!/usr/bin/env python3
"""Real local UI authorization with an ephemeral, unprivileged site member; no HTTP injection."""
import importlib.util,json,os,secrets,shutil
from pathlib import Path
from playwright.sync_api import sync_playwright,expect

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('stack',ROOT/'tools/dev/stack.py');stack=importlib.util.module_from_spec(spec);spec.loader.exec_module(stack)

def main():
 state=json.loads((stack.CONFIG).read_text());identifier=8_000_000_000_000+secrets.randbelow(1_000_000_000);suffix=secrets.token_hex(12);email='permissions-'+suffix+'@local.test';role='permissions-'+suffix
 # Reuse the local smoke account's hash without exposing it to Python or logs. The temporary
 # user gets one empty role and no administrator flag, MFA enrollment or inherited permissions.
 def sql(statement):return stack.docker('exec','-i','-e','MYSQL_PWD='+state['appPassword'],stack.PREFIX+'-mysql','mysql','-uapp','-N','b2b',input=statement)
 result={'transport':'real local HTTPS','injected_responses':False,'ephemeral_member':True}
 try:
  sql(f"START TRANSACTION;INSERT INTO sys_role(id,code,name) VALUES({identifier},'{role}','权限验收角色');INSERT INTO sys_user(id,email,display_name,password_hash,status,system_admin,must_change_password) SELECT {identifier},'{email}','权限验收用户',password_hash,'ACTIVE',0,0 FROM sys_user WHERE email='admin@local.test' AND deleted_at IS NULL;INSERT INTO sys_user_site_role(id,site_id,user_id,role_id) VALUES({identifier},1,{identifier},{identifier});COMMIT;")
  with sync_playwright() as p:
   browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or shutil.which('chromium'))
   try:
    context=browser.new_context(ignore_https_errors=True);page=context.new_page();page.goto('https://localhost:8443/admin/',wait_until='networkidle')
    page.get_by_label('邮箱',exact=True).fill(email);page.get_by_label('密码',exact=True).fill(state.get('smokePassword',state['bootstrapPassword']));page.get_by_role('button',name='继续',exact=True).click();expect(page.get_by_role('heading',name='工作台',exact=True)).to_be_visible()
    page.goto('https://localhost:8443/admin/system/modules',wait_until='networkidle');expect(page.get_by_text('没有访问权限',exact=True)).to_be_visible();expect(page.get_by_role('link',name='模块管理',exact=True)).to_have_count(0);result['route_guard_forbidden']=True
    response=context.request.get('https://localhost:8443/api/admin/v1/system/sites/1/modules/tracking/disable-impact',headers={'X-Site-Id':'1'});assert response.status==403;result['server_denies_same_permission']=True
    response=context.request.get('https://localhost:8443/api/admin/v1/system/bootstrap',headers={'X-Site-Id':'1'});assert response.status==200 and response.json()['permissions']==[];result['empty_permission_snapshot']=True
    page.get_by_role('button',name='退出登录',exact=True).click();expect(page.get_by_role('heading',name='登录',exact=True)).to_be_visible()
   finally:browser.close()
 finally:
  sql(f"DELETE ur FROM sys_user_site_role ur JOIN sys_user u ON u.id=ur.user_id WHERE u.id={identifier} AND u.email='{email}';DELETE FROM sys_user WHERE id={identifier} AND email='{email}';DELETE FROM sys_role WHERE id={identifier} AND code='{role}';")
  remaining=sql(f"SELECT (SELECT COUNT(*) FROM sys_user WHERE email='{email}')+(SELECT COUNT(*) FROM sys_role WHERE code='{role}');").stdout.strip();assert remaining=='0';result['fixtures_removed']=True
 out=ROOT/'.reports/frontend/permissions-browser.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))

if __name__=='__main__':main()
