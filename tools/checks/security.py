#!/usr/bin/env python3
"""Mandatory scanners. Never silently waive findings or treat service failures as a pass."""
import importlib.util,json,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('runner',ROOT/'tools/verification/m0-compatibility/verify_backend.py');r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
IMAGE='zricethezav/gitleaks:v8.28.0@sha256:cdbb7c955abce02001a9f6c9f602fb195b7fadc1e812065883f695d1eeaba854'
def main():
 out=ROOT/'.reports/security';out.mkdir(parents=True,exist_ok=True)
 leaks=r.docker('run','--rm','--mount',f'type=bind,src={ROOT},dst=/repo,readonly',IMAGE,'dir','/repo','--config','/repo/.gitleaks.toml','--redact','--no-banner',check=False)
 (out/'gitleaks.log').write_text(leaks.stdout+leaks.stderr)
 env=os.environ.copy();env['PNPM_HOME']=str(ROOT/'.dev/pnpm-home')
 if env.get('CODEX_PROXY_CERT'):env['NODE_EXTRA_CA_CERTS']=env['CODEX_PROXY_CERT']
 audit=subprocess.run(['pnpm','audit','--prod','--audit-level','high','--json'],cwd=ROOT,env=env,text=True,capture_output=True)
 (out/'pnpm-audit.json').write_text(audit.stdout);(out/'pnpm-audit.stderr').write_text(audit.stderr)
 result={'gitleaks':leaks.returncode==0,'pnpm_high_gate':audit.returncode==0,'automatic_waivers':False}
 (out/'summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
 return int(not all(result[k] for k in ('gitleaks','pnpm_high_gate')))
if __name__=='__main__':raise SystemExit(main())
