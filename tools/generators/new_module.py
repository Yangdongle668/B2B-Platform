#!/usr/bin/env python3
"""Create a compile-time module. Existing modules are never overwritten; no business CRUD is invented."""
import argparse,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def generate(root,module,name,layer):
 if not re.fullmatch('[a-z][a-z0-9]{1,31}',module) or module in ('core','api','application','infrastructure'):raise ValueError('Invalid/reserved module id')
 if not re.search('[\u3400-\u9fff]',name) or not 1<=len(name)<=32:raise ValueError('Name must be a Chinese label (1–32 characters)')
 base=root/'backend/modules'/module
 if base.exists():raise FileExistsError('Module already exists: '+module)
 descriptor=dict(id=module,name=name,version='1.0.0',layer=layer,platform='>=1.0.0 <2.0.0',requires={},optional={},disableable=True,tablePrefix=None,apiPrefix=module,permissions=[],menus=[],settings=None,components=[],events={'publishes':[],'subscribes':[]})
 files={
 'pom.xml':'<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>com.b2bweb</groupId><artifactId>b2b-platform</artifactId><version>0.1.0-SNAPSHOT</version><relativePath>../../../pom.xml</relativePath></parent><artifactId>module-'+module+'</artifactId><dependencies><dependency><groupId>com.b2bweb</groupId><artifactId>platform-api</artifactId><version>${project.version}</version></dependency><dependency><groupId>org.springframework.modulith</groupId><artifactId>spring-modulith-api</artifactId></dependency></dependencies></project>\n',
 f'src/main/resources/META-INF/b2bweb/modules/{module}/module.json':json.dumps(descriptor,ensure_ascii=False,indent=2)+'\n',
 f'src/main/java/com/b2bweb/{module}/package-info.java':'@org.springframework.modulith.ApplicationModule(allowedDependencies={"api","core :: *"})\n@org.jspecify.annotations.NullMarked\npackage com.b2bweb.'+module+';\n',
 f'src/main/java/com/b2bweb/{module}/{module.title()}Module.java':f'''package com.b2bweb.{module};
@org.springframework.stereotype.Component("b2bwebModule_{module}")
public class {module.title()}Module implements com.b2bweb.api.extension.PlatformModule {{
 private final com.b2bweb.api.extension.ModuleDescriptor descriptor;
 public {module.title()}Module(@org.springframework.beans.factory.annotation.Qualifier("{module}ModuleDescriptor") com.b2bweb.api.extension.ModuleDescriptor descriptor) {{this.descriptor=descriptor;}}
 public com.b2bweb.api.extension.ModuleDescriptor descriptor() {{return descriptor;}}
}}
'''}
 # Validate both insertion points before creating any files.
 reactor=root/'pom.xml';application=root/'backend/application/pom.xml';reactor_text=reactor.read_text();application_text=application.read_text()
 if reactor_text.count('</modules>')!=1 or application_text.count('<dependencies>')!=1:raise ValueError('Unexpected Maven layout')
 for relative,body in files.items():path=base/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body)
 reactor.write_text(reactor_text.replace('</modules>',f'<module>backend/modules/{module}</module></modules>'))
 dependency=f'<dependency><groupId>com.b2bweb</groupId><artifactId>module-{module}</artifactId><version>${{project.version}}</version></dependency>'
 application.write_text(application_text.replace('<dependencies>','<dependencies>'+dependency,1))
 frontend=root/f'frontend/apps/admin/src/modules/{module}/index.ts';frontend.parent.mkdir(parents=True,exist_ok=True);frontend.write_text("import { defineAdminModule } from '@b2bweb/admin-kit'\nexport default defineAdminModule({ id: '"+module+"', routes: [] })\n")
 return {'module':module,'files':len(files)+3,'business_features':0}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--id',required=True);parser.add_argument('--name',required=True);parser.add_argument('--layer',choices=['L2','L3','L4'],default='L3');parser.add_argument('--root',type=Path,default=ROOT);args=parser.parse_args();print(json.dumps(generate(args.root.resolve(),args.id,args.name,args.layer),ensure_ascii=False))
if __name__=='__main__':main()
