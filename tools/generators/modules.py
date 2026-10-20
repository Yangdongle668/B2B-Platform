#!/usr/bin/env python3
"""Generate initial modules from the approved layout and document-owned DDL.
Existing migrations are immutable: a mismatch is an error, never an overwrite.
"""
from pathlib import Path
import re,json,argparse
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'backend/modules'
PARENT='../../..'
def pom(name,parent,dependencies=''):
 return f'<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>com.b2bweb</groupId><artifactId>b2b-platform</artifactId><version>0.1.0-SNAPSHOT</version><relativePath>{parent}/pom.xml</relativePath></parent><artifactId>{name}</artifactId><dependencies>{dependencies}</dependencies></project>\n'
def put(path,body,immutable=False):
 path.parent.mkdir(parents=True,exist_ok=True)
 if immutable and path.exists() and path.read_text()!=body:raise SystemExit('Immutable migration changed; add new migration: '+str(path))
 path.write_text(body)
def schema(text):
 blocks=re.findall(r'```sql\n(.*?)\n```',text,re.S);creates={};alters=[]
 for block in blocks:
  found=re.findall(r'^CREATE TABLE\s+(\w+)',block,re.M)
  if found:creates[found[0]]=block
  elif re.search(r'^ALTER TABLE\s',block,re.M):alters.append(block)
 pending=set(creates);ordered=[]
 while pending:
  ready=sorted(t for t in pending if not(set(re.findall(r'REFERENCES\s+(\w+)',creates[t],re.I))-{t})&pending)
  if not ready:raise ValueError('FK cycle')
  ordered.extend(creates[t] for t in ready);pending-=set(ready)
 return ordered,alters
if __name__=='__main__':
 framework=(ROOT/'docs/framework/platform-framework.md').read_text()
 modules=[]
 for doc in sorted((ROOT/'docs/modules').glob('*.md')):
  if doc.stem=='README':continue
  name=doc.stem;text=doc.read_text();descriptor=json.loads(text.split('### 2.1 ')[1].split('```json\n')[1].split('\n```')[0]);modules.append(name)
  base=BASE/name
  put(base/'pom.xml',pom('module-'+name,'../../..',('<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jdbc</artifactId></dependency>' if name in ('media','search') else '')+'<dependency><groupId>com.b2bweb</groupId><artifactId>platform-api</artifactId><version>${project.version}</version></dependency><dependency><groupId>org.springframework.modulith</groupId><artifactId>spring-modulith-api</artifactId></dependency>'))
  put(base/f'src/main/resources/META-INF/b2bweb/modules/{name}/module.json',json.dumps(descriptor,ensure_ascii=False,indent=2)+'\n')
  put(base/f'src/main/java/com/b2bweb/{name}/package-info.java','/** '+descriptor['name']+'：M0 skeleton; business slice not implemented. */\n@org.springframework.modulith.ApplicationModule(allowedDependencies={"api","core :: *"})\n@org.jspecify.annotations.NullMarked\npackage com.b2bweb.'+name+';\n')
  module_class=''.join(part.title() for part in name.split('-'))+'Module'
  put(base/f'src/main/java/com/b2bweb/{name}/{module_class}.java',f"""package com.b2bweb.{name};
/** Generated M0 lifecycle entry; business behavior is implemented in its milestone slice. */
@org.springframework.stereotype.Component("b2bwebModule_{name}")
public class {module_class} implements com.b2bweb.api.extension.PlatformModule {{
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public {module_class}(@org.springframework.beans.factory.annotation.Qualifier("{name}ModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor){{this.descriptor=descriptor;}}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor(){{return descriptor;}}
}}
""")
  creates,alters=schema(text)
  for version,items in [('001',creates),('002',alters)]:
   if items:put(base/('src/main/resources/db/migration/'+name+'/V'+version+'__'+('init.sql' if version=='001' else 'm0_remediation.sql')),'\n\n'.join(items)+'\n',True)
 core=ROOT/'backend/platform/platform-core'
 creates,alters=schema(framework)
 assert not alters
 initial=core/'src/main/resources/db/migration/core/V001__init.sql'
 if initial.exists():
  existing=re.findall(r'CREATE TABLE\s+(\w+)',initial.read_text())
  known={re.search(r'CREATE TABLE\s+(\w+)',sql).group(1):sql for sql in creates}
  for old in re.findall(r'(CREATE TABLE\s+.*?;)\s*(?=CREATE TABLE|$)',initial.read_text(),re.S):
   name=re.search(r'CREATE TABLE\s+(\w+)',old).group(1)
   if old.strip()!=known[name].strip():raise SystemExit('Existing core migration differs: '+name)
  extra={name:sql for name,sql in known.items() if name not in existing}
  for name,sql in extra.items():
   version='V002__processed_events.sql' if name=='sys_processed_event' else 'V003__event_publications.sql'
   put(core/'src/main/resources/db/migration/core'/version,sql+'\n',True)
 else:put(initial,'\n\n'.join(creates)+'\n',True)
 put(core/'src/main/resources/META-INF/b2bweb/module.schema.json',framework.split('### A.2 JSON Schema')[1].split('```json\n')[1].split('\n```')[0]+'\n')
 print(json.dumps({'module_skeletons':len(modules),'core_tables':len(creates),'module_names':modules}))
