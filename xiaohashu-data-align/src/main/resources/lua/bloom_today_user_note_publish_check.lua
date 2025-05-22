local key = KEYS[1]
local userId = ARGV[1]

--检查布隆过滤器是否存在
local exists = redis.call("EXISTS",key)
if exists == 0 then
    redis.call("BF.ADD",key,"")
    --过期时间
    redis.call("EXPIRE",key,20*60*60)
end

--检验该变更数据是否已经存在（1 表示已经存在；0 表示不存在）
return redis.call("BF.EXISTS",key,userId)