#!/usr/bin/env python3
"""G4: compare migration bytes to the actual Git base, including deletion and renames."""
import argparse,hashlib,json,re,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def git(root,*args):return subprocess.check_output(['git',*args],cwd=root)
def check(root,base):
 git(root,'rev-parse','--verify',base+'^{commit}')
 old={p for p in git(root,'ls-tree','-r','--name-only',base).decode().splitlines() if '/src/main/resources/db/migration/' in p and p.endswith('.sql')}
 current={str(p.relative_to(root)) for p in (root/'backend').rglob('*.sql') if '/src/main/resources/db/migration/' in str(p)} | {p for p in old if (root/p).is_file()};errors=[];maximum={}
 for path in old:
  match=re.fullmatch(r'(.*/db/migration/([a-z][a-z0-9-]*))/V(\d{3})__[a-z0-9_]+\.sql',path)
  if match:maximum[match[1]]=max(maximum.get(match[1],0),int(match[3]))
  elif path.startswith('backend/'):errors.append(f'{path}: invalid base migration path')
  if path not in current or hashlib.sha256(git(root,'show',base+':'+path)).digest()!=hashlib.sha256((root/path).read_bytes()).digest():errors.append(f'{path}: migration changed/deleted/renamed')
 seen=set()
 for path in sorted(current):
  if not path.startswith('backend/'):continue
  match=re.fullmatch(r'(.*/db/migration/([a-z][a-z0-9-]*))/V(\d{3})__[a-z0-9_]+\.sql',path)
  if not match:errors.append(f'{path}: invalid migration name');continue
  key=(match[1],int(match[3]))
  if key in seen:errors.append(f'{path}: duplicate sequence')
  seen.add(key)
  if path not in old and int(match[3])<=maximum.get(match[1],0):errors.append(f'{path}: new migration must follow base maximum')
 return errors
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--base',default='HEAD');args=parser.parse_args();args.base='HEAD' if args.base and set(args.base)=={'0'} else args.base;errors=check(ROOT,args.base);print(json.dumps({'gate':'G4','base':args.base,'errors':errors},ensure_ascii=False));raise SystemExit(bool(errors))
