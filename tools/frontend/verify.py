#!/usr/bin/env python3
"""Run each mandatory frontend command and fail on its actual exit status."""
import os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 env=os.environ.copy();env['PNPM_HOME']=str(ROOT/'.dev/pnpm-home')
 if env.get('CODEX_PROXY_CERT'):env['NODE_EXTRA_CA_CERTS']=env['CODEX_PROXY_CERT']
 for command in (['pnpm','install','--frozen-lockfile'],['pnpm','lint'],['pnpm','typecheck'],['pnpm','test'],['pnpm','build']):subprocess.run(command,env=env,cwd=ROOT,check=True)
if __name__=='__main__':main()
