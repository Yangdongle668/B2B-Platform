#!/usr/bin/env python3
"""Real local HTTPS image route and private management checks; only owned disposable fixtures."""
import base64,hashlib,hmac,importlib.util,json,ssl,struct,urllib.error,urllib.request,uuid,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('stack',ROOT/'tools/dev/stack.py');stack=importlib.util.module_from_spec(spec);spec.loader.exec_module(stack)
def png(width,height):
 def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
 pixels=b''.join(b'\0'+b'\xff\0\0'*width for _ in range(height))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b'')
def main():
 for role in ('backend','image','storage'):
  if not stack.owned(stack.PREFIX+'-'+role):raise RuntimeError('Local stack is missing '+role)
 value=stack.config();tls=ssl.create_default_context(cafile=str(stack.STATE/'cert.pem'))
 client=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPSHandler(context=tls))
 def status(url):
  try:
   with client.open(url,timeout=10) as response:return response.status
  except urllib.error.HTTPError as error:return error.code
 backend=stack.PREFIX+'-backend'
 def private(path):return stack.docker('exec',backend,'curl','--noproxy','127.0.0.1','-fsS','http://127.0.0.1:8081'+path).stdout
 assert json.loads(private('/actuator/health/readiness'))['status']=='UP'
 metrics=private('/actuator/prometheus')
 for metric in ('b2bweb_core_jobs_queue','b2bweb_core_events_incomplete_count','b2bweb_core_events_incomplete_oldest_age_seconds'):
  assert metric in metrics,metric
 assert status('http://localhost:8080/actuator/health/readiness')==403
 assert status('https://localhost:8443/actuator/health/readiness')!=200
 key='1/contract/m0-runtime-'+uuid.uuid4().hex+'.png';object_url='http://localhost:9090/public-assets/'+key
 try:
  client.open(urllib.request.Request(object_url,png(320,240),{'Content-Type':'image/png'},method='PUT'),timeout=10).close()
  path='/rs:fill:80:60/g:fp:0.5:0.5/q:80/plain/'+key+'@png'
  signature=base64.urlsafe_b64encode(hmac.new(bytes.fromhex(value['imageKey']),bytes.fromhex(value['imageSalt'])+path.encode(),hashlib.sha256).digest()).decode().rstrip('=')
  image_url='https://localhost:8443/__images/'+signature+path
  with client.open(image_url,timeout=10) as response:
   data=response.read();assert response.headers['Content-Type']=='image/png'
   assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(80,60)
  assert status(image_url.replace('/rs:fill:80:60/','/rs:fill:81:60/'))==403
 finally:client.open(urllib.request.Request(object_url,method='DELETE'),timeout=10).close()
 result={'private_readiness':'UP','management_publicly_accessible':False,'prometheus_backlog_metrics':True,'same_origin_https_image':True,'actual_s3_png_dimensions':[80,60],'tampered_signature_status':403,'fixture_removed':True,'production_storage_certification':False}
 out=ROOT/'.reports/runtime-stack.json';out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
