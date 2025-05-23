local key = KEYS[1]
local userId = ARGV[1]

local exists = redis.call("EXISTS",key)
--不存在该布隆过滤器
if exists == 0 then
    redis.call("BF.ADD",key,"")
    redis.call("EXPIRE",key,20*60*60)
end

-- 校验该变更数据是否已经存在(1 表示已存在，0 表示不存在)
return redis.call("BF.EXISTS",key,userId)
