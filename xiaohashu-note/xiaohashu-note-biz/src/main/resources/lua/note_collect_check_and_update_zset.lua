local key = KEYS[1]
local noteId = ARGV[1]
local timestamp = ARGV[2]

--使用exists检查zset笔记收藏列表是否存在
local exists = redis.call('EXISTS',key)
if exists == 0 then
    return -1
end

--获取笔记收藏列表大小
local size = redis.call('ZCARD',key)
--若已经收藏了300篇，则移除最早的
if size >= 300 then
    redis.call('ZPOPMIN',key)
end

--添加新的笔记收藏关系
redis.call('ZADD',key,timestamp,noteId)
return 0