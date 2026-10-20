#!/usr/bin/env python3
"""Generate and verify an eighteenth module in an isolated, disposable checkout copy."""
import importlib.util,json,os,shutil,subprocess,tempfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 started=time.monotonic();directory=Path(tempfile.mkdtemp(prefix='b2b-new-module-',dir='/tmp'));copy=directory/'checkout';copy.mkdir()
 ignore=shutil.ignore_patterns('target','node_modules','.dev','.reports','.output','.nuxt','dist','__pycache__','.git')
 for name in ('backend','frontend','tools'):shutil.copytree(ROOT/name,copy/name,ignore=ignore)
 for name in ('pom.xml','mvnw'):shutil.copy2(ROOT/name,copy/name)
 try:
  subprocess.run(['python3','tools/generators/new_module.py','--id','sandbox','--name','模块接入演练','--root',str(copy)],cwd=copy,check=True)
  # The generated module participates in the real runtime catalog, independent migration histories,
  # lifecycle startup, full Modulith checks, and the entire integration suite.
  with (directory/'verify.log').open('w') as log:subprocess.run(['python3','tools/dev/verify_backend.py','-q','-Db2b.expected-module-count=18','verify'],cwd=copy,check=True,stdout=log,stderr=subprocess.STDOUT)
  report=json.loads((copy/'.reports/backend.json').read_text());elapsed=time.monotonic()-started
  assert elapsed<3600
  result={'module':'sandbox','isolated_copy':True,'elapsed_seconds':round(elapsed,3),'budget_seconds':3600,'tests':report['tests'],'failures':report['failures'],'business_implementation':False,'human_merge_review':False}
  out=ROOT/'.reports';out.mkdir(exist_ok=True);(out/'new-module.json').write_text(json.dumps(result,indent=2)+'\n');shutil.copy2(directory/'verify.log',out/'new-module.log');print(json.dumps(result))
 finally:
  # Maven outputs may use the mapped container UID: only this exact temporary directory is removed.
  spec=importlib.util.spec_from_file_location('runner',ROOT/'tools/verification/m0-compatibility/verify_backend.py');runner=importlib.util.module_from_spec(spec);spec.loader.exec_module(runner)
  runner.docker('run','--rm','--mount','type=bind,src='+str(directory)+',dst=/exercise',runner.MAVEN,'sh','-c','find /exercise -mindepth 1 -delete',check=False)
  shutil.rmtree(directory,ignore_errors=True)
if __name__=='__main__':main()
