local result = false;
local threshold = tonumber(ARGV[1]);
local now = tonumber(ARGV[2]);
if (threshold <= 0) then
    redis.call('set', KEYS[1], now);
    result = true;
else
    local lastOpenedAt = redis.call('GET', KEYS[1]);
    if (now >= (lastOpenedAt + threshold)) then
        redis.call('set', KEYS[1], now);
        result = true;
    else
        result = false;
    end;
end;
return result;
