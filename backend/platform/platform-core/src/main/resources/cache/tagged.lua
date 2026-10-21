-- Single-node Redis. Every operation checks the live process identity inside the atomic script.
local op, control, fence, entry = ARGV[1], KEYS[1], KEYS[2], KEYS[3]
local boot = string.match(redis.call('INFO', 'server'), 'run_id:([^\r\n]+)')
local sameBoot = redis.call('HGET', control, 'boot') == boot
local epoch = redis.call('HGET', control, 'epoch')
if op == 'state' then return {boot, sameBoot and (epoch or '') or '', sameBoot and (redis.call('HGET', control, 'ready') or '0') or '0'} end
if op == 'begin' then
  redis.call('HSET', control, 'boot', boot, 'epoch', ARGV[2], 'ready', '0')
  redis.call('PERSIST', control)
  return {boot, ARGV[2]}
end
if op == 'finish' then
  if sameBoot and boot == ARGV[3] and epoch == ARGV[2] then redis.call('HSET', control, 'ready', '1'); return {'1'} end
  return {'0'}
end
if op == 'advance' and not sameBoot then
  redis.call('HSET', control, 'boot', boot, 'epoch', ARGV[2], 'ready', '0')
  redis.call('PERSIST', control)
  epoch = ARGV[2]
end
if op ~= 'advance' and (not sameBoot or redis.call('HGET', control, 'ready') ~= '1') then return {} end
local function ensureFence()
  if redis.call('HGET', fence, 'epoch') ~= epoch then redis.call('HSET', fence, 'epoch', epoch, 'inc', ARGV[2], 'counter', '0') end
  redis.call('PERSIST', fence)
end
if op == 'capture' then ensureFence(); return {redis.call('HGET', fence, 'inc'), redis.call('HGET', fence, 'counter')} end
if op == 'advance' then
  ensureFence()
  if redis.call('HGET', fence, 'counter') == '9223372036854775807' then redis.call('HSET', fence, 'inc', ARGV[2], 'counter', '0')
  else redis.call('HINCRBY', fence, 'counter', 1) end
  return {'1'}
end
local inc, counter = redis.call('HGET', fence, 'inc'), redis.call('HGET', fence, 'counter')
local current = redis.call('HGET', fence, 'epoch') == epoch
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
if op == 'get' then
  if current and redis.call('HGET', entry, 'inc') == inc and redis.call('HGET', entry, 'counter') == counter and redis.call('HGET', entry, 'epoch') == epoch then
    local deadline = redis.call('HGET', entry, 'until')
    if deadline and tonumber(deadline) > now then return {redis.call('HGET', entry, 'value'), deadline} end
  end
  return {}
end
if op == 'put' then
  if not current or inc ~= ARGV[3] or counter ~= ARGV[4] then return {} end
  local deadline = tonumber(ARGV[5]); local ttl = deadline - now
  if ttl <= 0 then return {} end
  redis.call('HSET', entry, 'value', ARGV[6], 'until', ARGV[5], 'inc', inc, 'counter', counter, 'epoch', epoch)
  redis.call('PEXPIREAT', entry, deadline)
  for i = 7, #ARGV do
    redis.call('SADD', ARGV[i], entry)
    if redis.call('PTTL', ARGV[i]) < ttl then redis.call('PEXPIRE', ARGV[i], ttl) end
  end
  return {'1'}
end
return redis.error_reply('Unknown tagged cache operation')
