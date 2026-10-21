#!/usr/bin/env python3
"""Run real schema-gate mutations in a disposable source copy; never edit the checkout or index."""
import json,os,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 git=subprocess.run(['git','rev-parse','--absolute-git-dir'],cwd=ROOT,text=True,capture_output=True,check=True).stdout.strip()
 with tempfile.TemporaryDirectory(prefix='b2b-schema-gate-',dir='/tmp') as temporary:
  copy=Path(temporary);package=copy/'frontend/packages/component-schemas';package.parent.mkdir(parents=True)
  shutil.copytree(ROOT/'frontend/packages/component-schemas',package,ignore=shutil.ignore_patterns('node_modules'))
  (package/'node_modules').symlink_to(ROOT/'frontend/packages/component-schemas/node_modules',target_is_directory=True)
  (package.parent/'design-tokens').symlink_to(ROOT/'frontend/packages/design-tokens',target_is_directory=True)
  (copy/'backend').symlink_to(ROOT/'backend',target_is_directory=True)
  env=os.environ.copy();env['GIT_DIR']=git;env['GIT_WORK_TREE']=str(ROOT)
  file=package/'components/page/hero.json';original=json.loads(file.read_text());results=[]
  def run(name,definition,expected,reason=''):
   file.write_text(json.dumps(definition,ensure_ascii=False)+'\n')
   result=subprocess.run(['node',str(package/'scripts/check.mjs'),'--base','HEAD'],cwd=copy,env=env,text=True,capture_output=True)
   if (result.returncode==0)!=expected or reason and reason not in result.stderr:raise AssertionError(name+': '+result.stdout+result.stderr)
   results.append({'case':name,'expected_pass':expected,'matched':True})
  def fresh():return json.loads(json.dumps(original))
  run('unchanged',fresh(),True)
  value=fresh();value['propsSchema']['properties']['title']['maxLength']=110
  run('tightened_limit_without_version',value,False,'breaking schema change requires version +1')
  value=fresh();value['propsSchema']['properties']['title'].update({'enum':['框架示例标题'],'x-translatable':False})
  run('new_enum_without_version',value,False,'breaking schema change requires version +1')
  value=fresh();value['propsSchema']['properties']['title']['maxLength']=200
  run('relaxed_limit',value,True)
  value=fresh();value['propsSchema']['properties']['optionalFlag']={'title':'可选标记','type':'boolean','x-translatable':False}
  run('optional_property',value,True)
 out=ROOT/'.reports/schema-selftest.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps({'isolated_copy':True,'cases':results},indent=2)+'\n');print(json.dumps({'isolated_copy':True,'gate_cases':len(results),'matched':True}))
if __name__=='__main__':main()
