#!/usr/bin/env python3
"""F1: all external versions are catalog-owned; internal dependencies use workspace:*."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[2]
def check(root=ROOT):
 errors=[]
 for path in [root/'package.json',*root.glob('frontend/*/*/package.json')]:
  data=json.loads(path.read_text())
  for section in ('dependencies','devDependencies','peerDependencies'):
   for name,value in data.get(section,{}).items():
    if name.startswith('@b2bweb/'):
     if value!='workspace:*':errors.append(f'{path.relative_to(root)}: internal {name} must use workspace:*')
    elif value!='catalog:':errors.append(f'{path.relative_to(root)}: external {name} must use catalog:')
    if re.search(r'(^|/)(?:openai|anthropic|vue-i18n|@intlify)(?:/|$)',name):errors.append(f'{path.relative_to(root)}: forbidden dependency {name}')
    if name=='element-plus' and '/admin' not in str(path):errors.append(f'{path.relative_to(root)}: Element Plus is admin only')
 return errors
if __name__=='__main__':
 errors=check();print(json.dumps({'gate':'F1','errors':errors},ensure_ascii=False));raise SystemExit(bool(errors))
