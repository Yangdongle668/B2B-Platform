#!/usr/bin/env python3
"""One command, loopback-only local stack. Secrets and state stay in ignored .dev/stack/."""
from pathlib import Path
import argparse,base64,contextlib,hashlib,http.client,http.server,importlib.util,json,os,secrets,signal,socket,ssl,subprocess,sys,time,urllib.request,uuid
ROOT=Path(__file__).resolve().parents[2];STATE=ROOT/'.dev/stack';CONFIG=STATE/'state.json'
spec=importlib.util.spec_from_file_location('verify',ROOT/'tools/dev/verify_backend.py');verify=importlib.util.module_from_spec(spec);spec.loader.exec_module(verify);r=verify.r
PREFIX='b2b-local-'+hashlib.sha256(str(ROOT).encode()).hexdigest()[:8]
RESOURCES=['backend','mysql','redis','search','storage','mail']
def docker(*args,**kwargs):return r.docker(*args,**kwargs)
def save(state):
 STATE.mkdir(parents=True,exist_ok=True);CONFIG.touch(mode=0o600);CONFIG.write_text(json.dumps(state,indent=2)+'\n');CONFIG.chmod(0o600)
def config():
 if CONFIG.exists():
  value=json.loads(CONFIG.read_text());
  if 'internalTokens' not in value:value['internalTokens']=secrets.token_urlsafe(32);save(value)
  return value
 value={'owner':str(ROOT),'prefix':PREFIX,'internalTokens':secrets.token_urlsafe(32),'rootPassword':secrets.token_urlsafe(32),'appPassword':secrets.token_urlsafe(32),'migrationPassword':secrets.token_urlsafe(32),'storageKey':secrets.token_urlsafe(32),'searchKey':secrets.token_urlsafe(32),'bootstrapPassword':secrets.token_urlsafe(24),'secretKeys':'local:'+base64.b64encode(secrets.token_bytes(32)).decode(),'recoveryKey':base64.b64encode(secrets.token_bytes(32)).decode()};save(value);return value

def env(value):
 subnets=json.loads(docker('network','inspect',PREFIX).stdout)[0]['IPAM']['Config'];cidrs=','.join(['127.0.0.1/32','::1/128']+[entry['Subnet'] for entry in subnets])
 return {'B2BWEB_INTERNAL_TOKENS':value['internalTokens'],'B2BWEB_CORE_INTERNAL_ALLOWED_CIDRS':cidrs,'SPRING_PROFILES_ACTIVE':'local','B2BWEB_DB_URL':'jdbc:mysql://mysql:3306/b2b?allowPublicKeyRetrieval=true&useSSL=false','B2BWEB_DB_USER':'app','B2BWEB_DB_PASSWORD':value['appPassword'],'B2BWEB_DB_MIGRATION_USER':'migrator','B2BWEB_DB_MIGRATION_PASSWORD':value['migrationPassword'],'B2BWEB_REDIS_URL':'redis://redis:6379','B2BWEB_SECRET_KEYS':value['secretKeys'],'B2BWEB_RECOVERY_CODE_KEY':value['recoveryKey'],'B2BWEB_NODE_ID':'0','B2BWEB_S3_ENDPOINT':'http://storage:9090','B2BWEB_S3_SIGNING_ENDPOINT':'http://localhost:9090','B2BWEB_S3_ACCESS_KEY':'local-access','B2BWEB_S3_SECRET_KEY':value['storageKey'],'B2BWEB_MEILI_URL':'http://meilisearch:7700','B2BWEB_MEILI_MASTER_KEY':value['searchKey'],'B2BWEB_BOOTSTRAP_PASSWORD':value['bootstrapPassword'],'B2BWEB_DEV_SEED_ENABLED':'true'}
def owned(name):
 result=docker('inspect','--format','{{index .Config.Labels "b2bweb.local.owner"}}',name,check=False)
 if result.returncode==0 and result.stdout.strip()!=str(ROOT):raise RuntimeError('Refusing resource owned by another checkout: '+name)
 return result.returncode==0

def run_service(role,image,*args):
 name=PREFIX+'-'+role
 if owned(name):docker('start',name);return
 docker('run','-d','--name',name,'--label','b2bweb.local.owner='+str(ROOT),'--network',PREFIX,*args,image)
def wait_mysql(value):
 for _ in range(120):
  result=docker('exec','-e','MYSQL_PWD='+value['rootPassword'],PREFIX+'-mysql','mysql','-uroot','-N','-e','SELECT 1',check=False)
  if result.returncode==0:return
  time.sleep(.5)
 raise TimeoutError('Local MySQL not ready; inspect docker logs '+PREFIX+'-mysql')
def stop_web(value):
 pid=value.get('supervisor')
 if pid:
  command=Path('/proc/'+str(pid)+'/cmdline')
  if command.exists() and str(Path(__file__).resolve()).encode() in command.read_bytes() and b'serve' in command.read_bytes():
   os.kill(pid,signal.SIGTERM)
   for _ in range(50):
    try:
     with socket.create_connection(('127.0.0.1',8443),timeout=.1):pass
    except OSError:break
    time.sleep(.1)
  value.pop('supervisor',None);save(value)

def up(no_build=False):
 value=config()
 if value.get('owner')!=str(ROOT):raise RuntimeError('Local state belongs to another checkout')
 stop_web(value)
 if not no_build:
  subprocess.run([str(ROOT/'mvnw'),'-q','-Pdevelopment','-DskipTests','package'],cwd=ROOT,check=True)
  subprocess.run([sys.executable,'tools/frontend/verify.py'],cwd=ROOT,check=True)
 # Never consume or delete unrelated containers, volumes, or networks.
 net=docker('network','inspect',PREFIX,check=False)
 if net.returncode!=0:docker('network','create','--label','b2bweb.local.owner='+str(ROOT),PREFIX)
 else:
  metadata=json.loads(net.stdout)[0]
  if metadata.get('Labels',{}).get('b2bweb.local.owner')!=str(ROOT):raise RuntimeError('Network owner mismatch')
 run_service('mysql',r.MYSQL,'--network-alias','mysql','--memory','768m','-e','MYSQL_DATABASE=b2b','-e','MYSQL_ROOT_PASSWORD='+value['rootPassword'],'--mount','type=volume,src='+PREFIX+'-mysql,dst=/var/lib/mysql')
 run_service('redis',r.REDIS,'--network-alias','redis','--memory','256m','--mount','type=volume,src='+PREFIX+'-redis,dst=/data')
 run_service('search',r.MEILI,'--network-alias','meilisearch','--memory','1536m','-e','MEILI_MASTER_KEY='+value['searchKey'],'-e','MEILI_NO_ANALYTICS=true','--mount','type=volume,src='+PREFIX+'-search,dst=/meili_data')
 run_service('storage',verify.S3MOCK,'--network-alias','storage','--memory','768m','-p','127.0.0.1:9090:9090','-e','COM_ADOBE_TESTING_S3MOCK_STORE_ROOT=/data','-e','COM_ADOBE_TESTING_S3MOCK_STORE_RETAIN_FILES_ON_EXIT=true','-e','COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS=public-assets,private-assets','--mount','type=volume,src='+PREFIX+'-storage,dst=/data')
 run_service('mail',verify.MAILPIT,'--network-alias','mailpit','--memory','128m','-p','127.0.0.1:8025:8025')
 wait_mysql(value)
 sql="CREATE USER IF NOT EXISTS 'app'@'%' IDENTIFIED BY '"+value['appPassword']+"';GRANT SELECT,INSERT,UPDATE,DELETE ON b2b.* TO 'app'@'%';CREATE USER IF NOT EXISTS 'migrator'@'%' IDENTIFIED BY '"+value['migrationPassword']+"';GRANT ALL PRIVILEGES ON b2b.* TO 'migrator'@'%';"
 docker('exec','-i','-e','MYSQL_PWD='+value['rootPassword'],PREFIX+'-mysql','mysql','-uroot',input=sql)
 backend=PREFIX+'-backend'
 if owned(backend):docker('rm','-f',backend)
 extra=[]
 for key,item in env(value).items():extra+=['-e',key+'='+item]
 jar=ROOT/'backend/application/target/application-0.1.0-SNAPSHOT.jar'
 docker('run','-d','--name',backend,'--label','b2bweb.local.owner='+str(ROOT),'--network',PREFIX,'--memory','1536m','-p','127.0.0.1:8080:8080','--mount','type=bind,src='+str(jar)+',dst=/app.jar,readonly',*extra,r.MAVEN,'java','-jar','/app.jar','--b2bweb.dev.seed.enabled=true')
 client=urllib.request.build_opener(urllib.request.ProxyHandler({}))
 for _ in range(180):
  try:
   with client.open('http://localhost:8080/api/admin/v1/system/auth/csrf',timeout=1) as response:
    if response.status==200:break
  except OSError:time.sleep(.5)
 else:raise TimeoutError('Backend not ready. Use docker logs '+backend)
 if not (STATE/'key.pem').exists():
  subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','30','-keyout',str(STATE/'key.pem'),'-out',str(STATE/'cert.pem'),'-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost,IP:127.0.0.1'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=True);(STATE/'key.pem').chmod(0o600)
 with (STATE/'web.log').open('a') as log:
  proc=subprocess.Popen([sys.executable,str(Path(__file__).resolve()),'serve'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True);value['supervisor']=proc.pid;save(value)
 print('Local stack: https://localhost:8443/ and https://localhost:8443/admin/ (local self-signed certificate).')
 print('Login admin@local.test; initial password is in .dev/stack/state.json (bootstrapPassword). MFA enrollment and password change are required.')
 print('Mailpit: http://localhost:8025. Stop with python tools/dev/stack.py down; data is retained.')

def serve():
 processes=[]
 class Proxy(http.server.BaseHTTPRequestHandler):
  protocol_version='HTTP/1.1'
  def handle_request(self):
   if self.path=='/__local_health':
    self.send_response(200);self.send_header('Content-Length','2');self.end_headers();self.wfile.write(b'ok');return
   port=8080 if self.path.startswith('/api/') else 5173 if self.path=='/admin' or self.path.startswith('/admin/') else 3000
   body=self.rfile.read(int(self.headers.get('Content-Length','0')))
   headers={k:v for k,v in self.headers.items() if k.lower() not in ('connection','host','transfer-encoding','upgrade','forwarded','x-forwarded-host','x-forwarded-proto')};headers['Host']='localhost'
   connection=http.client.HTTPConnection('127.0.0.1',port,timeout=120)
   try:
    connection.request(self.command,self.path,body=body,headers=headers);response=connection.getresponse();data=response.read();self.send_response(response.status)
    for key,item in response.getheaders():
     if key.lower() not in ('connection','content-length','transfer-encoding'):self.send_header(key,item)
    self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
   except (OSError,http.client.HTTPException):self.send_error(502,'Local application unavailable')
   finally:connection.close()
  do_GET=do_POST=do_PUT=do_PATCH=do_DELETE=do_OPTIONS=do_HEAD=handle_request
  def log_message(self,*args):pass
 try:
  environment=os.environ.copy();environment.update(PNPM_HOME=str(ROOT/'.dev/pnpm-home'),NITRO_HOST='127.0.0.1',NITRO_PORT='3000',NUXT_TELEMETRY_DISABLED='1')
  for cmd,cwd in [(['node','.output/server/index.mjs'],ROOT/'frontend/apps/website'),(['pnpm','exec','vite','preview','--host','127.0.0.1','--port','5173','--strictPort'],ROOT/'frontend/apps/admin')]:processes.append(subprocess.Popen(cmd,cwd=cwd,env=environment))
  server=http.server.ThreadingHTTPServer(('127.0.0.1',8443),Proxy);tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.load_cert_chain(STATE/'cert.pem',STATE/'key.pem');server.socket=tls.wrap_socket(server.socket,server_side=True)
  def stop(*args):raise KeyboardInterrupt()
  signal.signal(signal.SIGTERM,stop);server.serve_forever(poll_interval=.2)
 except KeyboardInterrupt:pass
 finally:
  for proc in processes:proc.terminate()
  for proc in processes:
   try:proc.wait(timeout=10)
   except subprocess.TimeoutExpired:proc.kill();proc.wait()

def down():
 value=config();stop_web(value)
 for role in RESOURCES:
  name=PREFIX+'-'+role
  if owned(name):docker('rm','-f',name)
 # No -v, volume rm, or broad prune. The next up reuses persistent local data.
 print('Stopped this checkout local stack; data retained.')
def main():
 parser=argparse.ArgumentParser();parser.add_argument('action',choices=['up','down','serve','status']);parser.add_argument('--no-build',action='store_true');args=parser.parse_args()
 if args.action=='up':up(args.no_build)
 elif args.action=='serve':serve()
 elif args.action=='down':down()
 else:
  for role in RESOURCES:
   name=PREFIX+'-'+role
   print(name+': '+('present' if owned(name) else 'absent'))
if __name__=='__main__':main()
