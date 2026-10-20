#!/usr/bin/env python3
"""Full reactor verification against disposable MySQL/Redis, zero host ports."""
import importlib.util,os,secrets,subprocess,time,uuid,base64,json,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
S3MOCK='adobe/s3mock:4.11.0@sha256:cd49108c0094bc3f420b24bff354073032a9ec1a6f4cc89f8e8f483a308cf99e'
MAILPIT='axllent/mailpit:v1.27.9@sha256:b004cc0672d0692f097ba8ab9e5956ae08eeafee3d0a6bf2ab239750101ac538'
spec=importlib.util.spec_from_file_location('runner',ROOT/'tools/verification/m0-compatibility/verify_backend.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
def main():
 network='b2b-g0-'+uuid.uuid4().hex[:10];mysql=network+'-mysql';redis=network+'-redis';search=network+'-search';storage=network+'-storage';mail=network+'-mail'
 # Clean inside Maven's container: target directories may be owned by its mapped UID.
 output=ROOT/'.reports';output.mkdir(exist_ok=True);(output/'backend.json').unlink(missing_ok=True)
 try:
  r.docker('network','create',network)
  r.docker('run','-d','--name',mysql,'--network',network,'--network-alias','mysql','--memory','768m','-e','MYSQL_ALLOW_EMPTY_PASSWORD=yes','-e','MYSQL_ROOT_HOST=%','-e','MYSQL_DATABASE=b2b',r.MYSQL,'--skip-log-bin','--innodb-buffer-pool-size=128M')
  r.docker('run','-d','--name',redis,'--network',network,'--network-alias','redis','--memory','256m',r.REDIS,'redis-server','--save','','--appendonly','no')
  r.docker('run','-d','--name',search,'--network',network,'--network-alias','meilisearch','--memory','1536m','-e','MEILI_NO_ANALYTICS=true','-e','MEILI_MASTER_KEY=local-test-search-key','-e','MEILI_MAX_INDEXING_MEMORY=512MiB','-e','MEILI_MAX_INDEXING_THREADS=2',r.MEILI)
  r.docker('run','-d','--name',storage,'--network',network,'--network-alias','storage','--memory','768m','-e','COM_ADOBE_TESTING_S3MOCK_STORE_RETAIN_FILES_ON_EXIT=false',S3MOCK)
  r.docker('run','-d','--name',mail,'--network',network,'--network-alias','mailpit','--memory','128m',MAILPIT)
  for _ in range(90):
   if r.docker('exec',mysql,'mysql','-h127.0.0.1','-uroot','-N','-e','SELECT 1',check=False).returncode==0:break
   time.sleep(.5)
  else:raise TimeoutError('MySQL not ready')
  app_password=secrets.token_urlsafe(24);migration_password=secrets.token_urlsafe(24)
  r.docker('exec','-i',mysql,'mysql','-uroot',input=f"CREATE USER 'app'@'%' IDENTIFIED BY '{app_password}'; GRANT SELECT,INSERT,UPDATE,DELETE ON b2b.* TO 'app'@'%'; CREATE USER 'migrator'@'%' IDENTIFIED BY '{migration_password}'; GRANT ALL PRIVILEGES ON b2b.* TO 'migrator'@'%';")
  env=os.environ.copy();env.update(B2BWEB_S3_ENDPOINT='http://storage:9090',B2BWEB_S3_ACCESS_KEY='local-test-access',B2BWEB_S3_SECRET_KEY=secrets.token_urlsafe(24),B2BWEB_MAILPIT_URL='http://mailpit:8025',B2BWEB_MEILI_URL='http://meilisearch:7700',B2BWEB_MEILI_MASTER_KEY='local-test-search-key',B2B_DEV_NETWORK=network,B2BWEB_DB_URL='jdbc:mysql://mysql:3306/b2b?allowPublicKeyRetrieval=true&useSSL=false',B2BWEB_DB_USER='app',B2BWEB_DB_PASSWORD=app_password,B2BWEB_DB_MIGRATION_USER='migrator',B2BWEB_DB_MIGRATION_PASSWORD=migration_password,B2BWEB_REDIS_URL='redis://redis:6379',B2BWEB_SECRET_KEYS='test:'+base64.b64encode(secrets.token_bytes(32)).decode(),B2BWEB_RECOVERY_CODE_KEY=base64.b64encode(secrets.token_bytes(32)).decode())
  command=['python3',str(ROOT/'tools/dev/maven.py'),'clean',*(__import__('sys').argv[1:] or ['verify'])]
  started=time.time_ns()
  subprocess.run(command,env=env,cwd=ROOT,check=True)
  suites=[]
  for path in sorted((ROOT/'backend').rglob('TEST-*.xml')):
   # Partial reactor runs must not inherit stale XML from unrelated modules.
   if path.stat().st_mtime_ns<started:continue
   report=ET.parse(path).getroot();item={k:int(report.attrib.get(k,0)) for k in ('tests','failures','errors','skipped')};item['name']=report.attrib['name'];suites.append(item)
  assert suites and all(not any(s[k] for k in ('failures','errors','skipped')) for s in suites)
  result=dict(reactor_arguments=__import__('sys').argv[1:] or ['verify'],suites=suites,tests=sum(s['tests'] for s in suites),failures=0,errors=0,skipped=0,images={'mysql':r.MYSQL,'redis':r.REDIS,'maven':r.MAVEN,'s3-compatible':S3MOCK,'search':r.MEILI,'mail':MAILPIT},storage_provider='S3Mock; MinIO not verified')
  (output/'backend.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
 finally:
  r.docker('rm','-f','-v',mysql,redis,search,storage,mail,check=False);r.docker('network','rm',network,check=False)
if __name__=='__main__':main()
