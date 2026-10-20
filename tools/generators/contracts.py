#!/usr/bin/env python3
"""Materialize chapter 4's canonical public Java declarations; never invent DTOs."""
from pathlib import Path
import re,json,argparse
ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs/framework/platform-framework.md'
DEST=ROOT/'backend/platform/platform-api/src/main/java'
EXTRA={'ReferenceWriteApi':'common','MediaReferenceOwner':'common','ReferenceStage':'common','MediaReferenceSink':'media','SiteResourceChangedEvent':'event','CacheWriteFence':'port','ProjectionChangeApi':'common','SearchChangeCapture':'extension','TransactionMutexApi':'common'}
PACKAGES='common site security content url media taxonomy form notification translation seo delivery extension event port'.split()
IMPORTS='\n'.join(['import java.util.*;','import java.time.*;','import java.net.*;','import java.io.*;','import java.util.function.*;','import java.lang.annotation.*;','import org.jspecify.annotations.Nullable;','import jakarta.validation.Valid;','import jakarta.validation.constraints.*;','import tools.jackson.databind.*;','import tools.jackson.databind.node.*;']+['import com.b2bweb.api.'+p+'.*;' for p in PACKAGES])
def mask(s):
 return re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda m:' '*(len(m[0])),s,flags=re.S)
def generate():
 section=DOC.read_text().split('## 4. platform-api 契约')[1].split('## 5. 后端框架')[0]
 result={}
 for block in re.finditer(r'```java\n(.*?)\n```',section,re.S):
  code=block[1];package=re.search(r'^package (com.b2bweb.api.\w+);',code,re.M)
  if not package and not any(re.search(r'\b'+n+r'\b',code) for n in EXTRA):continue
  if 'package com.b2bweb.testkit' in code:continue
  clean=mask(code);last=0
  for m in re.finditer(r'^public (?:(?:abstract|final|sealed|non-sealed) )?(?:interface|class|record|enum|@interface) (\w+)',code,re.M):
   start=m.start();opening=clean.index('{',m.end());depth=1;end=opening+1
   while depth:
    depth+=(clean[end]=='{')-(clean[end]=='}');end+=1
   name=m[1];pkg=package[1] if package else 'com.b2bweb.api.'+EXTRA[name]
   prefix=code[last:start];prefix=re.sub(r'^package [^;]+;|^import [^;]+;','',prefix,flags=re.M).strip()
   # Only immediately attached comments/annotations belong to the declaration.
   attached=''
   if prefix:
    pos=prefix.rfind('/**');attached=prefix[pos:]+'\n' if pos>=0 else ''
    if not attached:
     lines=prefix.splitlines();annotations=[x for x in lines if x.startswith('@')];attached='\n'.join(annotations)+('\n' if annotations else '')
   body=attached+code[start:end]+'\n';last=end
   key=pkg+'.'+name
   if key in result:raise ValueError('Duplicate canonical type: '+key)
   result[key]=(pkg,name,body)
 return result
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
 definitions=generate();bad=[]
 for key,(pkg,name,body) in definitions.items():
  path=DEST/Path(*pkg.split('.'))/(name+'.java')
  value='// Generated from platform-framework.md chapter 4; edit the canonical document.\npackage '+pkg+';\n'+IMPORTS+'\n\n'+body
  if args.check:
   if not path.exists() or path.read_text()!=value:bad.append(str(path.relative_to(ROOT)))
  else:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(value)
 for pkg in PACKAGES:
  p=DEST/Path('com/b2bweb/api')/pkg/'package-info.java'
  value='@org.jspecify.annotations.NullMarked\npackage com.b2bweb.api.'+pkg+';\n'
  if not args.check:p.write_text(value)
 if bad:raise SystemExit('Canonical contracts differ: '+', '.join(bad))
 print(json.dumps({'canonical_public_types':len(definitions),'mode':'check' if args.check else 'write'}))
