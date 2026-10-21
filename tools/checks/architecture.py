#!/usr/bin/env python3
"""Enforce package/build/table ownership; execute rejection fixtures as well."""
from pathlib import Path
import re,sys,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
PREFIX=dict(core='sys_',content='cnt_',url='url_',media='mda_',taxonomy='tx_',form='frm_',notification='ntf_',translation='trn_',product='prd_',article='art_',page='pg_',inquiry='inq_',navigation='nav_',seo='seo_',graph='grh_',search='sch_',tracking='trk_')
def violations(module,source):
 errors=[]
 for other in re.findall(r'import\s+com\.b2bweb\.(\w+)\.',source):
  if other in PREFIX and other!='core' and other!=module:errors.append('cross-module-import')
 if '${' in source:errors.append('unsafe-sql-substitution')
 for table in re.findall(r'\b(?:FROM|JOIN|UPDATE|INTO)\s+([a-z][a-z0-9_]+)',source,re.I):
  for owner,prefix in PREFIX.items():
   if table.startswith(prefix) and module!=owner:errors.append('cross-module-table')
 return errors
errors=[]
for module in (ROOT/'backend/modules').iterdir():
 if not module.is_dir():continue
 for path in (module/'src').rglob('*'):
  if 'target' in path.parts or path.suffix not in ('.java','.xml'):continue
  errors.extend(f'{path.relative_to(ROOT)}: {problem}' for problem in violations(module.name,path.read_text()))
 pom=ET.parse(module/'pom.xml');ns={'m':'http://maven.apache.org/POM/4.0.0'}
 for dep in pom.findall('.//m:dependencies/m:dependency',ns):
  artifact=dep.findtext('m:artifactId',namespaces=ns)
  if artifact.startswith('module-') or artifact in ('infrastructure','application'):errors.append(f'{module.name}: forbidden dependency {artifact}')
# Audit is append-only except for the one bounded retention task.
for path in (ROOT/'backend/platform/platform-core/src/main/java').rglob('*.java'):
 source=path.read_text()
 if re.search(r'\bUPDATE\s+sys_audit_log\b',source,re.I):errors.append(str(path)+': audit records are append-only')
 if re.search(r'\bDELETE\s+FROM\s+sys_audit_log\b',source,re.I) and path.name!='AuditRetention.java':errors.append(str(path)+': audit delete outside retention task')
fixtures=[('inquiry','import com.b2bweb.product.domain.Product;', 'cross-module-import'),('inquiry','SELECT * FROM prd_product','cross-module-table'),('product','SELECT * FROM prd_product WHERE id=${id}','unsafe-sql-substitution')]
for module,source,expected in fixtures:
 if expected not in violations(module,source):errors.append('negative boundary fixture failed: '+expected)
for path in (ROOT/'backend/platform/platform-api/src/main/java').rglob('*.java'):
 s=path.read_text()
 if re.search(r'import (?:com\.b2bweb\.(?!api)|org\.springframework\.(?!stereotype))',s):errors.append(str(path)+': api implementation dependency')
 if 'UnsupportedOperationException("示意' in s:errors.append(str(path)+': unimplemented canonical body')
print(json.dumps({'module_skeletons':17,'rejected_negative_fixtures':len(fixtures),'errors':errors},ensure_ascii=False,indent=2))
raise SystemExit(bool(errors))
