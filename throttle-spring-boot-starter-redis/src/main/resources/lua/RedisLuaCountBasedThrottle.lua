local result = false;
local threshold = tonumber(ARGV[1]);
if (threshold <= 0) then
    redis.call('set', KEYS[1], 0);
    result = true;
else
    local counter = redis.call('incrBy', KEYS[1], 1);
    if (counter > threshold) then
        redis.call('set', KEYS[1], 0);
        result = true;
    else
        result = false;
    end;
end;
return result
