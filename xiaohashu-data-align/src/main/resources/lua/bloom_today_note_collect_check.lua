local key = KEYS[1]
local noteIdAndNoteCreatorId = ARGV[1]

--检查布隆过滤器是否存在
local exists = redis.call("EXISTS",key)
if exists == 0 then
    --创建布隆过滤器
    redis.call("BF.ADD",key,"")
    redis.call("EXPIRE",key,20*60*60)
end

--校验该变更数据是否已经存在（1表示已经存在，0表示不存在）
return redis.call("BF.EXISTS",key,noteIdAndNoteCreatorId)