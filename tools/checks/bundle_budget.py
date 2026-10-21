#!/usr/bin/env python3
"""F5: initial static import graph gzip size; never counts lazy route imports as initial."""
import argparse,gzip,importlib.util,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('browser',ROOT/'tools/frontend/browser.py');browser=importlib.util.module_from_spec(spec);spec.loader.exec_module(browser)
BASELINE=ROOT/'tools/checks/bundle-budget.json'
def imported(source):
 # Vite/Rollup emits import/export ... from or side-effect import; dynamic import(...) stays lazy.
 return re.findall(r'(?:\b(?:import|export)\s*(?:[^;]*?\bfrom\s*)?)["\']([^"\']+\.js)["\']',source)
def size(entries,load):
 seen=set();pending=list(entries);total=0
 while pending:
  path=pending.pop()
  if path in seen:continue
  seen.add(path);data=load(path);total+=len(gzip.compress(data,mtime=0))
  from urllib.parse import urljoin
  for child in imported(data.decode()):pending.append(urljoin(path,child))
 return total,len(seen)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--write-admin-baseline',action='store_true');args=parser.parse_args();results=[]
 with browser.server(['node','.output/server/index.mjs'],ROOT/'frontend/apps/website') as (url,client):
  for route in ('/zh/','/zh/gallery'):
   html=client.open(url+route).read().decode();entries=re.findall(r'<script[^>]+src="([^"]+\.js)"',html)+re.findall(r'<link[^>]+rel="modulepreload"[^>]+href="([^"]+\.js)"',html)
   if not entries:raise RuntimeError('No website entry scripts found')
   count,chunks=size(entries,lambda path:client.open(url+path).read());results.append({'target':'website','route':route,'gzip_bytes':count,'chunks':chunks,'budget_bytes':150*1024,'passed':count<=150*1024})
 dist=ROOT/'frontend/apps/admin/dist';html=(dist/'index.html').read_text();entries=re.findall(r'<script[^>]+src="([^"]+\.js)"',html)
 if not entries:raise RuntimeError('No admin entry scripts found')
 count,chunks=size(entries,lambda path:(dist/path.lstrip('/').removeprefix('admin/')).read_bytes())
 if args.write_admin_baseline:BASELINE.write_text(json.dumps({'admin_initial_gzip_bytes':count,'review_status':'candidate; requires review with this change'},indent=2)+'\n')
 baseline=json.loads(BASELINE.read_text())['admin_initial_gzip_bytes'];results.append({'target':'admin','gzip_bytes':count,'chunks':chunks,'budget_bytes':int(baseline*1.1),'passed':count<=baseline*1.1})
 out=ROOT/'.reports';out.mkdir(exist_ok=True);report={'results':results,'scope':'current implemented routes; release Lighthouse budgets remain separate'};(out/'bundle-budget.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return int(any(not item['passed'] for item in results))
if __name__=='__main__':raise SystemExit(main())
