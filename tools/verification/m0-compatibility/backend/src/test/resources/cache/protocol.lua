-- Single-node Redis protocol fixture, not a production TaggedCache implementation.
local op = ARGV[1]
local control, fence, entry = KEYS[1], KEYS[2], KEYS[3]
local boot = string.match(redis.call('INFO', 'server'), 'run_id:([^\r\n]+)')
local function controlMatches()
    return redis.call('HGET', control, 'boot') == boot
end
local function ready()
    return controlMatches() and redis.call('HGET', control, 'ready') == '1'
end
if op == 'begin' then
    redis.call('HSET', control, 'boot', boot, 'epoch', ARGV[2], 'ready', '0')
    redis.call('PERSIST', control)
    return {boot, ARGV[2]}
end
if op == 'finish' then
    if controlMatches() and redis.call('HGET', control, 'epoch') == ARGV[2]
       and boot == ARGV[3] then
        redis.call('HSET', control, 'ready', '1')
        return 1
    end
    return 0
end
if op == 'invalidate' and not controlMatches() then
    -- Invalidation remains possible during recovery, but never implicitly opens the gate.
    redis.call('HSET', control, 'boot', boot, 'epoch', ARGV[3], 'ready', '0')
    redis.call('PERSIST', control)
end
if op ~= 'invalidate' and not ready() then
    if op == 'put' then return 0 end
    return {}
end
local epoch = redis.call('HGET', control, 'epoch')
local function ensureFence()
    if redis.call('HGET', fence, 'epoch') ~= epoch then
        redis.call('HSET', fence, 'epoch', epoch, 'inc', ARGV[2], 'counter', '0')
    end
    redis.call('PERSIST', fence)
end
if op == 'capture' then
    ensureFence()
    return {redis.call('HGET', fence, 'inc'), redis.call('HGET', fence, 'counter')}
end
if op == 'invalidate' then
    ensureFence()
    if redis.call('HGET', fence, 'counter') == '9223372036854775807' then
        redis.call('HSET', fence, 'inc', ARGV[2], 'counter', '0')
    else
        redis.call('HINCRBY', fence, 'counter', 1)
    end
    local deleted = 0
    if ARGV[4] == 'tags' then
        for i = 5, #ARGV do
            local members = redis.call('SMEMBERS', ARGV[i])
            for _, key in ipairs(members) do deleted = deleted + redis.call('UNLINK', key) end
            redis.call('UNLINK', ARGV[i])
        end
    elseif ARGV[4] == 'key' then
        deleted = redis.call('UNLINK', ARGV[5])
    elseif ARGV[4] == 'namespace' then
        -- Small-fixture cleanup only. Production needs bounded incremental cleanup.
        local cursor = '0'
        repeat
            local found = redis.call('SCAN', cursor, 'MATCH', ARGV[5], 'COUNT', 100)
            cursor = found[1]
            for _, key in ipairs(found[2]) do deleted = deleted + redis.call('UNLINK', key) end
        until cursor == '0'
    end
    return deleted
end
local current = redis.call('HGET', fence, 'epoch') == epoch
local inc, counter = redis.call('HGET', fence, 'inc'), redis.call('HGET', fence, 'counter')
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
if op == 'get' then
    if current and redis.call('HGET', entry, 'inc') == inc
       and redis.call('HGET', entry, 'counter') == counter
       and redis.call('HGET', entry, 'epoch') == epoch then
        local deadline = redis.call('HGET', entry, 'until')
        if deadline and tonumber(deadline) > now then
            return {redis.call('HGET', entry, 'value'), deadline}
        end
    end
    return {}
end
if op == 'put' then
    -- Counters are exact decimal strings: Lua doubles lose distinct values above 2^53.
    if not current or inc ~= ARGV[4] or counter ~= ARGV[5] then return 0 end
    local deadline = tonumber(ARGV[6])
    local ttl = deadline - now
    if ttl <= 0 then return 0 end
    redis.call('HSET', entry, 'value', ARGV[7], 'until', ARGV[6],
               'inc', inc, 'counter', counter, 'epoch', epoch)
    redis.call('PEXPIREAT', entry, deadline)
    for i = 8, #ARGV do
        redis.call('SADD', ARGV[i], entry)
        if redis.call('PTTL', ARGV[i]) < ttl then redis.call('PEXPIRE', ARGV[i], ttl) end
    end
    return 1
end
return redis.error_reply('Unknown cache protocol operation')
