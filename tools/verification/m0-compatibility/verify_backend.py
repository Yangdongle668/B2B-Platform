#!/usr/bin/env python3
"""Isolated, disposable Docker verification. Never connects to a user database."""
import argparse
import json
import io
import tarfile
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import urllib.parse
import uuid
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
MYSQL = 'mysql:8.4@sha256:0f51e63e537a1f176423fa03ef559aabe6fa4372a53160a44bc7e75da7d15364'
MAVEN = 'maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976'
ENV = os.environ.copy()
for key in ('DOCKER_HOST', 'DOCKER_CONTEXT', 'DOCKER_TLS', 'DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH'):
    ENV.pop(key, None)


def docker(*args, input=None, check=True):
    result = subprocess.run(['docker', '--host=unix:///var/run/docker.sock', *args],
                            env=ENV, input=input, text=True, capture_output=True, timeout=900)
    if check and result.returncode:
        raise RuntimeError(f'Docker {args[0]} failed: {result.stderr}')
    return result


def document_ddl():
    paths = [REPO / 'docs/framework/platform-framework.md'] + sorted((REPO / 'docs/modules').glob('*.md'))
    creates, alters = {}, []
    for path in paths:
        for index, block in enumerate(re.findall(r'```sql\n(.*?)\n```', path.read_text(), re.S), 1):
            tables = re.findall(r'^CREATE TABLE\s+`?(\w+)', block, re.M | re.I)
            item = dict(path=str(path.relative_to(REPO)), block=index, sql=block)
            if tables:
                if len(tables) != 1 or tables[0] in creates:
                    raise ValueError('Expected one unique authoritative CREATE per block')
                creates[tables[0]] = item
            elif re.search(r'^ALTER TABLE\s', block, re.M | re.I):
                alters.append(item)
    pending, ordered = set(creates), []
    while pending:
        ready = sorted(t for t in pending if not
                       (set(re.findall(r'REFERENCES\s+`?(\w+)', creates[t]['sql'], re.I)) - {t}) & pending)
        if not ready:
            raise ValueError('Foreign-key dependency cycle')
        ordered.extend(creates[t] | {'table': t} for t in ready)
        pending.difference_update(ready)
    return ordered, alters


def verify_schema(mysql):
    def sql(statement):
        return docker('exec', '-i', mysql, 'mysql', '-uroot', '-N', input=statement).stdout.strip()
    sql('CREATE DATABASE remediation CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;')
    creates, alters = document_ddl()
    results = []
    for item in creates + alters:
        sql('USE remediation; SET FOREIGN_KEY_CHECKS=1;\n' + item['sql'])
        results.append({k: v for k, v in item.items() if k != 'sql'} | {'success': True})
    count = int(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='remediation'"))
    columns = sql("SELECT CONCAT(table_name,'.',column_name) FROM information_schema.columns WHERE table_schema='remediation' AND column_name IN ('translation_review_required','expected_target_version','download_grant','gated_form_key','download_gate_version','purge_job_id') ORDER BY table_name,column_name").splitlines()
    expected = {'cnt_content.translation_review_required', 'trn_job_item.expected_target_version',
                'frm_submission.download_grant', 'mda_media.gated_form_key',
                'mda_media.download_gate_version', 'mda_media.purge_job_id', 'prd_product_document.gated_form_key'}
    assert set(columns) == expected, columns
    assert count == len(creates)
    return dict(mysql=sql('SELECT VERSION()'), foreign_key_checks=1, create_blocks=len(creates),
                alter_blocks=len(alters), tables=count, columns=columns, statements=results)


def verify_backend(network, mysql, maven, scratch, cache, output):
    # Read configured proxy only inside memory; never print it or persist it in evidence.
    result = docker('run', '--rm', '--network', network, MAVEN, 'sh', '-c',
                    'printf "%s" "${HTTPS_PROXY:-$HTTP_PROXY}"')
    proxy = urllib.parse.urlsplit(result.stdout)
    settings = ET.Element('settings')
    if proxy.hostname:
        p = ET.SubElement(ET.SubElement(settings, 'proxies'), 'proxy')
        values = [('id', 'session'), ('active', 'true'), ('protocol', 'http'),
                  ('host', proxy.hostname), ('port', str(proxy.port or 80)),
                  ('nonProxyHosts', 'localhost|127.*|mysql')]
        if proxy.username:
            values.append(('username', urllib.parse.unquote(proxy.username)))
        if proxy.password:
            values.append(('password', urllib.parse.unquote(proxy.password)))
        for key, value in values:
            ET.SubElement(p, key).text = value
    settings_path = scratch / 'settings.xml'
    settings_path.touch(mode=0o600)
    ET.ElementTree(settings).write(settings_path, encoding='unicode')
    args = ['run', '--name', maven, '--network', network, '--memory', '2g',
            '--mount', f'type=bind,src={HERE / "backend"},dst=/source,readonly',
            '--mount', f'type=bind,src={cache},dst=/cache/m2',
            '--mount', f'type=bind,src={settings_path},dst=/run/maven-settings.xml,readonly',
            '-e', 'PROBE_JDBC_URL=jdbc:mysql://mysql:3306/compat?allowPublicKeyRetrieval=true&useSSL=false',
            '-e', 'PROBE_PROTOCOL_JDBC_URL=jdbc:mysql://mysql:3306/remediation?allowPublicKeyRetrieval=true&useSSL=false',
            '-w', '/tmp']
    script = 'set -eu\nmkdir /tmp/probe\ncp -R /source/src /source/pom.xml /tmp/probe/\ncd /tmp/probe\n'
    cert = os.environ.get('CODEX_PROXY_CERT')
    if cert:
        if not Path(cert).is_file():
            raise ValueError('CODEX_PROXY_CERT must point to a readable public CA certificate')
        args.extend(['--mount', f'type=bind,src={Path(cert).resolve()},dst=/run/proxy-ca.pem,readonly'])
        script += '''cp "$JAVA_HOME/lib/security/cacerts" /tmp/probe-cacerts
keytool -importcert -noprompt -alias session-proxy -file /run/proxy-ca.pem -keystore /tmp/probe-cacerts -storepass changeit >/dev/null 2>&1
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/tmp/probe-cacerts -Djavax.net.ssl.trustStorePassword=changeit"
'''
    script += 'java -version\nmvn --version\nmvn -B -q --no-transfer-progress -s /run/maven-settings.xml -Dmaven.repo.local=/cache/m2 test\n'
    result = docker(*args, MAVEN, 'sh', '-c', script, check=False)
    (output / 'backend.log').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(f'Maven verification failed; see {output / "backend.log"}')
    copied = subprocess.run(['docker', '--host=unix:///var/run/docker.sock', 'cp',
                             maven + ':/tmp/probe/target/surefire-reports', '-'],
                            env=ENV, capture_output=True, check=True)
    suites = []
    with tarfile.open(fileobj=io.BytesIO(copied.stdout)) as archive:
        for member in archive.getmembers():
            if not Path(member.name).name.startswith('TEST-') or not member.name.endswith('.xml'):
                continue
            payload = archive.extractfile(member).read()
            (output / Path(member.name).name).write_bytes(payload)
            report = ET.fromstring(payload)
            assert all(report.attrib[k] == '0' for k in ('failures', 'errors', 'skipped'))
            suites.append(dict(name=report.attrib['name'], tests=int(report.attrib['tests']),
                               failures=0, errors=0, skipped=0, seconds=float(report.attrib['time']),
                               cases=[c.attrib['name'] for c in report.findall('testcase')]))
            if report.attrib['name'] == 'com.b2bplatform.compat.CompatibilityTest':
                root = report
    assert len(suites) == 4, 'Expected compatibility, media, translation and download suites'
    props = {p.attrib['name']: p.attrib['value'] for p in root.find('properties')}
    jars = [Path(x).name for x in props['java.class.path'].split(':')
            if re.search(r'(spring-boot-4|modulith-core|flyway-core|mybatis-plus-core)', x)]
    table = docker('exec', mysql, 'mysql', '-uroot', '-N', 'compat', '-e',
                   "SHOW TABLES LIKE 'EVENT_PUBLICATION'").stdout.strip()
    assert table == 'EVENT_PUBLICATION'
    return dict(tests=sum(s['tests'] for s in suites), failures=0, errors=0, skipped=0, suites=suites,
                java=props['java.version'], dependencies=jars, registry_table=table,
                cases=[c.attrib['name'] for c in root.findall('testcase')])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, default=HERE / '.results')
    parser.add_argument('--cache', type=Path, default=Path(tempfile.gettempdir()) / 'b2b-m0-maven-cache')
    parser.add_argument('--schema-only', action='store_true')
    args = parser.parse_args()
    output = args.output.resolve(); output.mkdir(parents=True, exist_ok=True)
    (output / 'backend-schema.json').unlink(missing_ok=True)
    cache = args.cache.resolve(); cache.mkdir(parents=True, exist_ok=True)
    stem = 'b2b-m0-' + uuid.uuid4().hex[:10]
    network, mysql, maven = stem, stem + '-mysql', stem + '-maven'
    result = dict(images={'mysql': MYSQL, 'maven': MAVEN})
    with tempfile.TemporaryDirectory(prefix='b2b-m0-settings-') as temp:
        try:
            docker('network', 'create', network)
            docker('run', '-d', '--name', mysql, '--network', network, '--network-alias', 'mysql',
                   '--memory', '768m', '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', '-e', 'MYSQL_ROOT_HOST=%',
                   '-e', 'MYSQL_DATABASE=compat', MYSQL, '--skip-log-bin', '--innodb-buffer-pool-size=128M')
            print('Waiting for isolated MySQL (no host port exposed)', flush=True)
            for _ in range(60):
                if docker('exec', mysql, 'mysql', '-h127.0.0.1', '-uroot', '-N', '-e', 'SELECT 1', check=False).returncode == 0:
                    break
                time.sleep(1)
            else:
                raise TimeoutError('MySQL TCP readiness timed out')
            result['schema'] = verify_schema(mysql)
            print(f"DDL: {result['schema']['create_blocks']} CREATE + {result['schema']['alter_blocks']} ALTER passed", flush=True)
            if not args.schema_only:
                result['backend'] = verify_backend(network, mysql, maven, Path(temp), cache, output)
                print(f"Backend: {result['backend']['tests']} tests passed", flush=True)
            (output / 'backend-schema.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        finally:
            # Only unique resources created by this invocation; anonymous DB volumes too.
            docker('rm', '-f', '-v', maven, mysql, check=False)
            docker('network', 'rm', network, check=False)


if __name__ == '__main__':
    main()
