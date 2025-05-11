local key = KEYS[1]
local noteId = ARGV[1]

--使用exists检查布隆过滤器是否存在
local exists = redis.call('EXISTS',key)
if exists == 0 then
    return -1
end

--校验该篇笔记是否被收藏过（1表示已经收藏，0表示未收藏）
return redis.call('BF.EXISTS',key,noteId)