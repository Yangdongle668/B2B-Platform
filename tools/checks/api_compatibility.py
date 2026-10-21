#!/usr/bin/env python3
"""B10 conservative structural OpenAPI check. Resolves local refs and rejects request narrowing / response removal."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[2]
SNAPSHOT='frontend/packages/api-client/openapi.json'
def compare(old,new):
 errors=[]
 def schema(a,b,path,request=False,visited=frozenset()):
  pair=(a.get('$ref'),b.get('$ref'),request)
  if a.get('$ref') or b.get('$ref'):
   if pair in visited:return
   def resolve(doc,value):
    if '$ref' not in value:return value
    if not value['$ref'].startswith('#/'):raise ValueError('External OpenAPI refs not supported')
    node=doc
    for key in value['$ref'][2:].split('/'):node=node[key.replace('~1','/').replace('~0','~')]
    return node
   return schema(resolve(old,a),resolve(new,b),path,request,visited|{pair})
  if a.get('type')!=b.get('type'):errors.append(path+': schema type changed')
  if a.get('format')!=b.get('format'):errors.append(path+': schema format changed')
  if request:
   if set(b.get('required',[]))-set(a.get('required',[])):errors.append(path+': required input added')
   if b.get('enum') and (not a.get('enum') or not set(a['enum'])<=set(b['enum'])):errors.append(path+': accepted enum narrowed')
   for key in ('maxLength','maximum','maxItems'):
    if key in b and (key not in a or b[key]<a[key]):errors.append(path+': input '+key+' narrowed')
   for key in ('minLength','minimum','minItems'):
    if key in b and (key not in a or b[key]>a[key]):errors.append(path+': input '+key+' narrowed')
   if a.get('pattern')!=b.get('pattern') and b.get('pattern'):errors.append(path+': input pattern changed')
  else:
   if set(a.get('required',[]))-set(b.get('required',[])):errors.append(path+': guaranteed output removed')
   if a.get('enum') and (not b.get('enum') or not set(b['enum'])<=set(a['enum'])):errors.append(path+': output enum widened')
  for key,value in a.get('properties',{}).items():
   if key not in b.get('properties',{}):errors.append(path+'/'+key+': property removed')
   else:schema(value,b['properties'][key],path+'/'+key,request,visited)
  if a.get('items'):schema(a['items'],b.get('items',{}),path+'[]',request,visited)
  for keyword in ('oneOf','anyOf','allOf','not'):
   if a.get(keyword)!=b.get(keyword):errors.append(path+': composition changed; review a versioned API')
 for path,methods in old.get('paths',{}).items():
  for verb,operation in methods.items():
   if verb not in ('get','post','put','patch','delete','head','options'):continue
   current=new.get('paths',{}).get(path,{}).get(verb)
   if current is None:errors.append(verb+' '+path+': endpoint removed');continue
   before={(p['in'],p['name']):p for p in methods.get('parameters',[])+operation.get('parameters',[])};after={(p['in'],p['name']):p for p in new['paths'][path].get('parameters',[])+current.get('parameters',[])}
   if operation.get('security',old.get('security',[]))!=current.get('security',new.get('security',[])):errors.append(path+': security requirements changed; review a versioned API')
   for key,value in after.items():
    if value.get('required') and not before.get(key,{}).get('required'):errors.append(path+': required parameter added')
   for key,value in before.items():
    if key not in after:errors.append(path+': parameter removed')
    else:schema(value.get('schema',{}),after[key].get('schema',{}),path+'/parameter/'+key[1],True)
   for mime,body in operation.get('requestBody',{}).get('content',{}).items():schema(body.get('schema',{}),current.get('requestBody',{}).get('content',{}).get(mime,{}).get('schema',{}),path+'/request',True)
   if current.get('requestBody',{}).get('required') and not operation.get('requestBody',{}).get('required'):errors.append(path+': request body became required')
   for status,response in operation.get('responses',{}).items():
    if status not in current.get('responses',{}):errors.append(path+': response status removed');continue
    for mime,body in response.get('content',{}).items():schema(body.get('schema',{}),current['responses'][status].get('content',{}).get(mime,{}).get('schema',{}),path+'/'+status+'/'+mime)
 return sorted(set(errors))
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--base',default='HEAD');args=parser.parse_args();args.base='HEAD' if args.base and set(args.base)=={'0'} else args.base;old=json.loads(subprocess.check_output(['git','show',args.base+':'+SNAPSHOT],cwd=ROOT));new=json.loads((ROOT/SNAPSHOT).read_text());errors=compare(old,new);print(json.dumps({'gate':'B10','base':args.base,'errors':errors},ensure_ascii=False));raise SystemExit(bool(errors))
