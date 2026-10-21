#!/usr/bin/env python3
"""Exercise the actual shared SchemaForm/Tiptap/reference controls in a dev-only browser lab."""
import importlib.util,json,os,shutil
from pathlib import Path
from playwright.sync_api import sync_playwright,expect
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('browser_helpers',ROOT/'tools/frontend/browser.py');helpers=importlib.util.module_from_spec(spec);spec.loader.exec_module(helpers)
def main():
 result={}
 with helpers.server(['pnpm','exec','vite','--host','127.0.0.1','--port','{port}','--strictPort'],ROOT/'frontend/apps/admin') as (url,_):
  with sync_playwright() as p:
   browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or shutil.which('chromium'))
   try:
    page=browser.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)));page.goto(url+'/admin/tests/lab/index.html',wait_until='networkidle');expect(page.get_by_role('heading',name='Schema 表单协议夹具')).to_be_visible()
    page.get_by_label('标题',exact=True).fill('有效标题');expect(page.locator('p[role=status]')).to_contain_text('校验通过');result['reactive_model_updates_and_validation']=True
    def submission():return json.loads(page.get_by_label('提交内容').inner_text())
    assert 'videoText' not in submission() and 'secret' not in submission();page.get_by_label('模式',exact=True).select_option('"video"');expect(page.get_by_label('视频文字',exact=True)).to_be_visible();assert 'imageText' not in submission();result['conditional_fields_and_secret_omission']=True
    page.get_by_role('button',name='添加',exact=True).click();assert len(submission()['items'])==2;page.locator('[data-field="/items/1"]').get_by_label('项目文字',exact=True).fill('second');assert submission()['items']==['one','second'];result['recursive_repeater']=True
    page.get_by_label('媒体',exact=True).select_option('11');assert submission()['media']=={'$media':'11'};result['typed_reference_selection']=True
    page.get_by_label('数据源',exact=True).select_option('sample.items');expect(page.get_by_label('数量',exact=True)).to_be_visible();page.get_by_label('数量',exact=True).fill('3');assert submission()['source']['$source']['params']['limit']==3;result['source_params_schema']=True
    page.locator('.tiptap').fill('编辑后的正文');assert submission()['richtext']['content'][0]['content'][0]['text']=='编辑后的正文';result['real_tiptap_json_editing']=True
    page.get_by_role('button',name='切换只读',exact=True).click();expect(page.get_by_label('标题',exact=True)).to_be_disabled();expect(page.locator('.tiptap')).to_have_attribute('contenteditable','false');page.get_by_role('button',name='切换只读',exact=True).click();page.get_by_role('button',name='切换自动语言',exact=True).click();expect(page.get_by_label('标题',exact=True)).to_be_disabled();result['readonly_and_automatic_language']=True
    assert not errors,errors
   finally:browser.close()
 out=ROOT/'.reports/frontend/form-browser.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
