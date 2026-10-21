#!/usr/bin/env python3
"""Emit method-level inventory so a single smoke test cannot masquerade as complete semantics."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[2]
def main():
 api=ROOT/'backend/platform/platform-api/src/main/java';interfaces=[]
 catalog=json.loads((ROOT/'backend/platform/platform-testkit/src/main/resources/META-INF/b2bweb/testkit/contract-catalog.json').read_text())
 for name in sorted(catalog['services']+catalog['ports']):
  path=api/('/'.join(name.split('.'))+'.java')
  text=path.read_text()
  if not re.search(r'public interface ',text):raise ValueError('Canonical contract is not an interface: '+name)
  methods=re.findall(r'^    (?:default )?(?:[\w<>?,.]+\s+)+([a-z][A-Za-z0-9]*)\(([^;{}]*?)\)(?:;| \{)',text,re.M|re.S)
  if methods:interfaces.append({'interface':'.'.join(path.relative_to(api).with_suffix('').parts),'methods':[{'name':name,'signature':' '.join(args.split())} for name,args in methods]})
 out=ROOT/'.reports';out.mkdir(exist_ok=True);result={'interfaces':interfaces,'semantic_acceptance':'Method inventory only; shared-suite presence does not certify the full behavioral matrix.'};(out/'contract-methods.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'inventoried_interfaces':len(interfaces),'declared_methods':sum(len(i['methods']) for i in interfaces)}))
if __name__=='__main__':main()
