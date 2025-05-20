local key = KEYS[1]
local noteIdAndNoteCreatorId = ARGV[1]

local exists = redis.call("EXISTS",key)
if exists == 0 then
    redis.call("BF.ADD",key,"") --创建布隆过滤器
    redis.call("EXPIRE",key,20*60*60) --设置过期时间
end

--校验变更数模是否已经存在（1表示已存在，0表示不存在）
return redis.call("BF.EXISTS", key, noteIdAndNoteCreatorId)