#!/usr/bin/env python3
"""Resolve every documented service/port/extension name to its canonical Java type."""
from pathlib import Path
import argparse,json,re
ROOT=Path(__file__).resolve().parents[2]
def inventory():
 document=(ROOT/'docs/framework/platform-framework.md').read_text();result={}
 for section,label in [('4.2.2','services'),('4.2.3','extensions'),('4.2.5','ports')]:
  body=document.split('#### '+section+' ')[1].split('\n#### ')[0].split('\n### ')[0];names=[]
  for row in body.splitlines():
   if not row.startswith('| `'):continue
   cell=row.split('|')[1];listed=re.findall(r'`([^`]+)`',cell)
   if label=='services':listed=listed[:1]
   for name in listed:
    if name.startswith('@') or name in ('DataScope','ContentTypeScoped','KeyedExtension'):continue
    paths=list((ROOT/'backend/platform/platform-api/src/main/java').rglob(name+'.java'))
    if len(paths)!=1:raise ValueError('Canonical type missing or ambiguous: '+name)
    source=paths[0].read_text();package=re.search(r'^package (.*);',source,re.M).group(1)
    if 'public interface '+name not in source:continue
    names.append(package+'.'+name)
  result[label]=sorted(set(names))
 return result
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
 result=inventory();output=ROOT/'backend/platform/platform-testkit/src/main/resources/META-INF/b2bweb/testkit/contract-catalog.json';body=json.dumps(result,indent=2)+'\n'
 if args.check:
  if not output.exists() or output.read_text()!=body:raise SystemExit('Contract catalog drift; run tools/generators/catalog.py')
 else:output.parent.mkdir(parents=True,exist_ok=True);output.write_text(body)
 print(json.dumps({k:len(v) for k,v in result.items()}))
if __name__=='__main__':main()
