#!/usr/bin/env python3
"""Mutation tests run against isolated in-memory API docs and disposable Git repositories."""
import copy,importlib.util,json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def module(name):
 spec=importlib.util.spec_from_file_location(name,ROOT/'tools/checks'/f'{name}.py');value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
def main():
 api=module('api_compatibility');migration=module('migration_immutable');cases=[]
 old={'paths':{'/api/public/v1/sample':{'get':{'responses':{'200':{'content':{'application/json':{'schema':{'type':'object','properties':{'title':{'type':'string'}},'required':['title']}}}}}}}}}
 assert not api.compare(old,copy.deepcopy(old));cases.append('compatible-api')
 for mutation in ('endpoint','field','type'):
  new=copy.deepcopy(old)
  if mutation=='endpoint':new['paths'].clear()
  elif mutation=='field':new['paths']['/api/public/v1/sample']['get']['responses']['200']['content']['application/json']['schema']['properties'].clear()
  else:new['paths']['/api/public/v1/sample']['get']['responses']['200']['content']['application/json']['schema']['properties']['title']['type']='integer'
  assert api.compare(old,new),mutation;cases.append('reject-api-'+mutation)
 with tempfile.TemporaryDirectory() as tmp:
  root=Path(tmp);path=root/'backend/modules/sample/src/main/resources/db/migration/sample/V001__init.sql';path.parent.mkdir(parents=True);path.write_text('CREATE TABLE sam_item(id BIGINT);\n')
  def git(*args):subprocess.run(['git',*args],cwd=root,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  git('init');git('add','.');git('-c','user.name=Gate Fixture','-c','user.email=gate@example.test','commit','-m','test: initial fixture')
  assert not migration.check(root,'HEAD');cases.append('unchanged-migration')
  initial=path.read_text();path.write_text(initial+'-- illegal edit\n');assert migration.check(root,'HEAD');cases.append('reject-migration-edit')
  path.unlink();assert migration.check(root,'HEAD');cases.append('reject-migration-delete');path.write_text(initial)
  addition=path.with_name('V002__append.sql');addition.write_text('ALTER TABLE sam_item ADD value INT;');assert not migration.check(root,'HEAD');cases.append('allow-append')
  addition.rename(path.with_name('V001__duplicate.sql'));assert migration.check(root,'HEAD');cases.append('reject-duplicate-sequence')
 out=ROOT/'.reports';out.mkdir(exist_ok=True);report={'cases':cases,'passed':len(cases),'failed':0};(out/'gate-selftest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
