local threshold = tonumber(ARGV[1]);
local now = tonumber(ARGV[2]);

if threshold <= 0 then
    redis.call('SET', KEYS[1], now);
    return true;
end;

local lastOpenedAt = tonumber(redis.call('GET', KEYS[1]));
if nil == lastOpenedAt or now >= (lastOpenedAt + threshold) then
    redis.call('SET', KEYS[1], now, 'PX', threshold);
    return true;
end;

return false;
