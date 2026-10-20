#!/usr/bin/env python3
"""Check immutable per-module migrations against document-owned SQL blocks."""
from pathlib import Path
import re,json,hashlib
ROOT=Path(__file__).resolve().parents[2]
errors=[];inventory=[]
for doc,owner,base in [(ROOT/'docs/framework/platform-framework.md','core',ROOT/'backend/platform/platform-core')]+[(p,p.stem,ROOT/'backend/modules'/p.stem) for p in sorted((ROOT/'docs/modules').glob('*.md')) if p.stem!='README']:
 canonical=[b.strip() for b in re.findall(r'```sql\n(.*?)\n```',doc.read_text(),re.S) if re.search(r'^(?:CREATE|ALTER) TABLE\s+\w+',b,re.M)]
 files=sorted((base/f'src/main/resources/db/migration/{owner}').glob('V*.sql'))
 actual=[]
 for path in files:
  # Authoritative CREATE / ALTER statements include comments and multiline bodies.
  payload=path.read_text().strip()
  statements=re.split(r'\n\s*\n(?=(?:CREATE|ALTER) TABLE)',payload)
  actual.extend(s.strip() for s in statements)
  inventory.append(dict(owner=owner,path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
 if sorted(actual)!=sorted(canonical):errors.append(f'{owner}: migration SQL differs from owner document')
print(json.dumps({'migration_files':len(inventory),'errors':errors},ensure_ascii=False,indent=2))
raise SystemExit(bool(errors))
