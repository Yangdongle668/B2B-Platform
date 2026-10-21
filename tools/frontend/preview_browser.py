#!/usr/bin/env python3
"""Real HTTPS iframe transport/auth smoke using an explicitly development-only content fixture."""
import importlib.util,json,os,shutil,secrets
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('stack',ROOT/'tools/dev/stack.py');stack=importlib.util.module_from_spec(spec);spec.loader.exec_module(stack)
BASE='https://localhost:8443'
def main():
 state=json.loads(stack.CONFIG.read_text());identifier=8_100_000_000_000+secrets.randbelow(1_000_000_000);suffix=secrets.token_hex(12);email='preview-'+suffix+'@local.test';role='preview-'+suffix
 def sql(statement):return stack.docker('exec','-i','-e','MYSQL_PWD='+state['appPassword'],stack.PREFIX+'-mysql','mysql','-uapp','-N','b2b',input=statement)
 result={'transport':'real local HTTPS iframe','injected_responses':False,'content_provider':'explicit development-only in-memory fixture; not production content','ephemeral_editor':True}
 sql(f"START TRANSACTION;INSERT INTO sys_role(id,code,name) VALUES({identifier},'{role}','画布验收角色');INSERT INTO sys_user(id,email,display_name,password_hash,status,system_admin,must_change_password) SELECT {identifier},'{email}','画布验收用户',password_hash,'ACTIVE',0,0 FROM sys_user WHERE email='admin@local.test' AND deleted_at IS NULL;INSERT INTO sys_user_site_role(id,site_id,user_id,role_id) VALUES({identifier},1,{identifier},{identifier});INSERT INTO sys_role_permission(role_id,permission_id) SELECT {identifier},id FROM sys_permission WHERE code='content:page:edit' AND removed_at IS NULL;COMMIT;")
 try:
  with sync_playwright() as p:
   browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or shutil.which('chromium'))
   try:
    context=browser.new_context(ignore_https_errors=True);page=context.new_page();page.goto(BASE+'/admin/',wait_until='networkidle')
    page.get_by_label('邮箱',exact=True).fill(email);page.get_by_label('密码',exact=True).fill(state.get('smokePassword',state['bootstrapPassword']));page.get_by_role('button',name='继续',exact=True).click();expect(page.get_by_role('heading',name='工作台',exact=True)).to_be_visible()
    csrf=next(c['value'] for c in context.cookies() if c['name']=='XSRF-TOKEN');headers={'X-Site-Id':'1','X-XSRF-TOKEN':csrf}
    def issue(purpose):
     response=context.request.post(BASE+'/api/admin/v1/system/m0/preview-fixture?purpose='+purpose,headers=headers);assert response.status==200,(response.status,response.text());return response.json()
    editor=issue('EDITOR');share=issue('SHARE')
    for token in ['invalid',share['token']]:
     response=context.request.get(BASE+'/__editor?token='+token);assert response.status==404,(response.status,response.text()[:200]);assert response.headers['cache-control']=='no-store'
    result['sharing_cannot_enter_editor']=True
    response=context.request.get(BASE+'/__editor?token='+editor['token']);assert response.status==200,(response.status,response.text()[:200]);assert response.headers['cache-control']=='no-store';assert response.headers['cdn-cache-control']=='no-store';assert response.headers['content-security-policy']=='frame-ancestors '+BASE;assert response.headers['x-robots-tag']=='noindex, nofollow';assert 'internalToken' not in response.text();result['server_gate_csp_and_private_headers']=True
    response=context.request.get(BASE+'/__preview/'+share['token']);assert response.status==200,(response.status,response.text()[:200]);assert 'M0预览夹具' in response.text();assert response.headers['cache-control']=='no-store';result['share_preview_ssr']=True
    # Parent is a real authenticated admin document. Messages go through browser postMessage;
    # only the local fixture data is supplied, with no mocked HTTP responses.
    page.evaluate('''url=>{window.previewMessages=[];window.addEventListener('message',e=>{if(e.origin===location.origin&&e.data?.source==='b2bweb-editor')window.previewMessages.push(e.data)});const f=document.createElement('iframe');f.id='m0-canvas';f.src=url;f.title='M0画布验收';document.body.appendChild(f)}''',BASE+'/__editor?token='+editor['token'])
    page.wait_for_function("window.previewMessages.some(m=>m.type==='preview:ready')")
    frame=page.frame_locator('#m0-canvas');expect(frame.get_by_role('heading',name='M0预览夹具',exact=True)).to_be_visible()
    def send(message):page.evaluate("m=>document.querySelector('#m0-canvas').contentWindow.postMessage(m,location.origin)",message)
    document=editor['layout'];document['sections'][0]['props']['title']='真实画布更新'
    send({'source':'b2bweb-editor','version':1,'type':'editor:init','payload':{'document':document,'locale':'zh-CN','device':'desktop'}})
    expect(frame.get_by_role('heading',name='真实画布更新',exact=True)).to_be_visible()
    send({'source':'b2bweb-editor','version':1,'type':'editor:patch','payload':{'ops':[{'op':'replace','path':'/sections/0/props/title','value':'增量更新已显示'}]}})
    expect(frame.get_by_role('heading',name='增量更新已显示',exact=True)).to_be_visible();result['real_iframe_init_and_patch']=True
    frame.get_by_role('heading',name='增量更新已显示',exact=True).click();page.wait_for_function("window.previewMessages.some(m=>m.type==='preview:select'&&m.payload.sectionId==='hero-one')");result['selection_roundtrip']=True
    send({'source':'b2bweb-editor','version':1,'type':'editor:patch','payload':{'ops':[{'op':'add','path':'/__proto__/polluted','value':True}]}});page.wait_for_function("window.previewMessages.some(m=>m.type==='preview:error')");assert frame.locator('body').evaluate("()=>({}).polluted===undefined");result['unsafe_patch_rejected']=True
    # Synthetic data from a same-origin sibling window is ignored because source must be parent.
    child=page.frames[1];child.evaluate("()=>window.dispatchEvent(new MessageEvent('message',{origin:location.origin,source:window,data:{source:'b2bweb-editor',version:1,type:'editor:patch',payload:{ops:[{op:'replace',path:'/sections/0/props/title',value:'不应显示'}]}}}))")
    expect(frame.get_by_role('heading',name='增量更新已显示',exact=True)).to_be_visible();result['wrong_window_source_rejected']=True
    # Revocation is tested through the real current user directory on each HTTP resolution.
    try:
     sql(f"UPDATE sys_user SET status='DISABLED' WHERE id={identifier} AND email='{email}';")
     response=context.request.get(BASE+'/__editor?token='+editor['token']);assert response.status==404
     response=context.request.post(BASE+'/__editor/resolve',data={'token':editor['token'],'partOverrides':{}});assert response.status==404
     result['live_issuer_revocation']=True
    finally:sql(f"UPDATE sys_user SET status='ACTIVE' WHERE id={identifier} AND email='{email}';")
    page.screenshot(path=str(ROOT/'.reports/frontend/preview-iframe.png'),full_page=True)
   finally:browser.close()
 finally:
  sql(f"DELETE FROM sys_role_permission WHERE role_id={identifier};DELETE FROM sys_user_site_role WHERE user_id={identifier};DELETE FROM sys_user WHERE id={identifier} AND email='{email}';DELETE FROM sys_role WHERE id={identifier} AND code='{role}';")
  assert sql(f"SELECT (SELECT COUNT(*) FROM sys_user WHERE id={identifier})+(SELECT COUNT(*) FROM sys_role WHERE id={identifier});").stdout.strip()=='0';result['fixtures_removed']=True

 out=ROOT/'.reports/frontend/preview-browser.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':
 try:main()
 except Exception as error:
  # Playwright's accessibility snapshot may echo even password field values.
  # Keep exit status and diagnostics while redacting checkout-owned credentials.
  message=str(error)
  if stack.CONFIG.exists():
   state=json.loads(stack.CONFIG.read_text())
   for key,value in state.items():
    if isinstance(value,str) and any(word in key.lower() for word in ('password','token','key')) and value:message=message.replace(value,'[REDACTED]')
  raise SystemExit(type(error).__name__+': '+message)
