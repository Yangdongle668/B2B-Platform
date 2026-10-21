#!/usr/bin/env python3
"""Exercise the formal Java cache against this checkout's Redis pause/restart with an old RDB.

Only project-owned local containers are touched; credentials and fence values are never printed.
"""
import importlib.util,json,time,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('stack',ROOT/'tools/dev/stack.py');stack=importlib.util.module_from_spec(spec);spec.loader.exec_module(stack)
def main():
 state=stack.config();name=stack.PREFIX+'-redis';assert stack.owned(name)
 client=urllib.request.build_opener(urllib.request.ProxyHandler({}))
 def call(operation,**values):
  request=urllib.request.Request('http://localhost:8080/api/internal/v1/system/m0/cache-probe',method='POST',headers={'Content-Type':'application/json','X-Internal-Token':state['internalTokens'].split(',')[0],'X-Site-Id':'1'},data=json.dumps({'operation':operation,**values}).encode())
  with client.open(request,timeout=8) as response:return json.load(response)
 def ready():
  deadline=time.monotonic()+30
  while time.monotonic()<deadline:
   if call('recover')['ready']:return
   time.sleep(.2)
  raise TimeoutError('Cache recovery did not confirm its durable boundary')
 ready();before=call('state')['state'];fence=call('fence')['fence'];assert fence
 assert call('put',**fence,value='before-restart')['stored'];assert call('get')['value']=='before-restart'
 stack.docker('exec',name,'redis-cli','SAVE');stack.docker('restart',name)
 deadline=time.monotonic()+10
 while time.monotonic()<deadline:
  if stack.docker('exec',name,'redis-cli','PING',check=False).stdout.strip()=='PONG':break
  time.sleep(.1)
 assert call('state')['state'][0]!=before[0]
 assert call('get')['found'] is False
 assert call('put',**fence,value='late-origin')['stored'] is False
 ready();current=call('fence')['fence'];assert call('put',**current,value='after-recovery')['stored'];assert call('get')['value']=='after-recovery'
 result={'provider':'formal Core RedisTaggedCache + CacheRecovery','real_process_restart':True,'old_ready_rdb_rejected':True,'old_fence_cannot_refill':True,'recovery_reopens_after_confirmation':True}
 stack.docker('pause',name)
 try:
  start=time.monotonic();assert call('get')['found'] is False;elapsed=time.monotonic()-start;assert elapsed<5;result['outage_read_bypass_seconds']=round(elapsed,3)
  assert not call('fence')['fence']
  try:call('invalidate');raise AssertionError('Invalidation must fail, preserving its retry obligation')
  except urllib.error.HTTPError as error:assert error.code>=500
  result['outage_invalidation_fails_closed']=True
 finally:stack.docker('unpause',name)
 ready();out=ROOT/'.reports/cache-restart.json';out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
