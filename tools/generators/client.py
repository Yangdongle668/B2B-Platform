#!/usr/bin/env python3
"""Compare the generated client input to OpenAPI exported by the real application test."""
import argparse,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--write',action='store_true');args=parser.parse_args()
 source=ROOT/'backend/application/target/openapi.json';target=ROOT/'frontend/packages/api-client/openapi.json'
 if not source.exists():raise SystemExit('Run ./mvnw verify first; missing actual OpenAPI export')
 actual=json.loads(source.read_text());actual.pop('servers',None)
 if args.write:target.write_text(json.dumps(actual,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
 elif json.loads(target.read_text())!=actual:raise SystemExit('OpenAPI drift: run python tools/generators/client.py --write and pnpm generate:client; commit both outputs')
 print(json.dumps({'actual_api_paths':len(actual['paths']),'mode':'write' if args.write else 'check'}))
if __name__=='__main__':main()
