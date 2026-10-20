#!/usr/bin/env python3
"""Run the repository Maven reactor with the managed proxy and CA preserved."""
import importlib.util,os,subprocess,tempfile,urllib.parse,uuid,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('runner',ROOT/'tools/verification/m0-compatibility/verify_backend.py');runner=importlib.util.module_from_spec(spec);spec.loader.exec_module(runner)
def main():
 import sys
 cache=Path(os.environ.get('B2B_MAVEN_CACHE','/tmp/b2b-m0-compat/m2'));cache.mkdir(parents=True,exist_ok=True)
 network=os.environ.get('B2B_DEV_NETWORK')
 proxy=urllib.parse.urlsplit(runner.docker('run','--rm',runner.MAVEN,'sh','-c','printf "%s" "${HTTPS_PROXY:-$HTTP_PROXY}"').stdout)
 with tempfile.TemporaryDirectory(prefix='b2b-maven-') as directory:
  settings=ET.Element('settings')
  if proxy.hostname:
   item=ET.SubElement(ET.SubElement(settings,'proxies'),'proxy')
   values={'id':'session','active':'true','protocol':'http','host':proxy.hostname,'port':str(proxy.port or 80),'nonProxyHosts':'localhost|127.*|mysql|redis|meilisearch|minio|storage|mailpit'}
   if proxy.username:values['username']=urllib.parse.unquote(proxy.username)
   if proxy.password:values['password']=urllib.parse.unquote(proxy.password)
   for k,v in values.items():ET.SubElement(item,k).text=v
  path=Path(directory)/'settings.xml';path.touch(mode=0o600);ET.ElementTree(settings).write(path,encoding='unicode')
  args=['docker','--host=unix:///var/run/docker.sock','run','--rm','--name','b2b-build-'+uuid.uuid4().hex[:10],'--memory','3g','--mount',f'type=bind,src={ROOT},dst=/workspace','--mount',f'type=bind,src={cache.resolve()},dst=/m2','--mount',f'type=bind,src={path},dst=/run/settings.xml,readonly','-w','/workspace']
  if network:args+=['--network',network]
  for key in ('B2BWEB_S3_SIGNING_ENDPOINT','B2BWEB_S3_ENDPOINT','B2BWEB_S3_ACCESS_KEY','B2BWEB_S3_SECRET_KEY','B2BWEB_MAILPIT_URL','B2BWEB_DB_URL','B2BWEB_DB_USER','B2BWEB_DB_PASSWORD','B2BWEB_REDIS_HOST','B2BWEB_MEILI_URL','B2BWEB_MEILI_MASTER_KEY','B2BWEB_SECRET_KEYS','B2BWEB_RECOVERY_CODE_KEY','B2BWEB_DB_MIGRATION_USER','B2BWEB_DB_MIGRATION_PASSWORD','B2BWEB_REDIS_URL'):
   if key in os.environ:args+=['-e',key]
  script='set -eu\n'
  cert=os.environ.get('CODEX_PROXY_CERT')
  if cert:
   args+=['--mount',f'type=bind,src={Path(cert).resolve()},dst=/run/proxy-ca.pem,readonly']
   script+='cp "$JAVA_HOME/lib/security/cacerts" /tmp/cacerts\nkeytool -importcert -noprompt -alias session-proxy -file /run/proxy-ca.pem -keystore /tmp/cacerts -storepass changeit >/dev/null 2>&1\nexport MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/tmp/cacerts -Djavax.net.ssl.trustStorePassword=changeit"\n'
  script+='exec mvn -B --no-transfer-progress -s /run/settings.xml -Dmaven.repo.local=/m2 "$@"\n'
  return subprocess.run(args+[runner.MAVEN,'sh','-c',script,'maven',*sys.argv[1:]],env=runner.ENV).returncode
if __name__=='__main__':raise SystemExit(main())
