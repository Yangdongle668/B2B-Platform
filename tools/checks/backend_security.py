#!/usr/bin/env python3
"""Scan the resolved Maven SBOM with OSV. Unreachable databases are an error, never a pass."""
import argparse,json,sys,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--bom',type=Path,default=ROOT/'.reports/security/bom.json');args=parser.parse_args()
 out=ROOT/'.reports/security';out.mkdir(parents=True,exist_ok=True)
 result={'scanner':'OSV querybatch','automatic_waivers':False,'status':'error','packages':0,'findings':[]}
 try:
  bom=json.loads(args.bom.read_text());packages={}
  for component in bom.get('components',[]):
   if component.get('purl','').startswith('pkg:maven/'):
    name=component.get('group','')+':'+component['name'];version=component['version'];packages[(name,version)]={'package':{'ecosystem':'Maven','name':name},'version':version}
  queries=list(packages.values());result['packages']=len(queries)
  if not queries:raise ValueError('Resolved Maven SBOM contains no packages')
  for offset in range(0,len(queries),100):
   batch=queries[offset:offset+100]
   request=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=json.dumps({'queries':batch}).encode(),headers={'Content-Type':'application/json'},method='POST')
   with urllib.request.urlopen(request,timeout=30) as response:data=json.load(response)
   if not isinstance(data.get('results'),list) or len(data['results'])!=len(batch):raise ValueError('Invalid OSV batch response')
   for query,item in zip(batch,data['results']):
    for advisory in item.get('vulns',[]):result['findings'].append({'package':query['package']['name'],'version':query['version'],'id':advisory['id']})
  # Conservatively block every advisory until severity and applicability have been reviewed.
  result['status']='failed' if result['findings'] else 'passed'
 except urllib.error.HTTPError as error:result['error']='Vulnerability database returned HTTP '+str(error.code)
 except Exception as error:result['error']=type(error).__name__+': '+str(error)
 (out/'backend-osv.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
 return 0 if result['status']=='passed' else 1
if __name__=='__main__':sys.exit(main())
