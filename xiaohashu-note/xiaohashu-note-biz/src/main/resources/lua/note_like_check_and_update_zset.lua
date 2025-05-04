local key = KEYS[1] --RedisKey
local noteId = ARGV[1] --笔记ID
local timestamp = ARGV[2] --时间戳

--使用EXISTS命令检查ZSET是否已经存在
local exists = redis.call('EXISTS',key)
if exists == 0 then
    return -1
end

--获取笔记点赞列表的大小
local size = redis.call('ZCARD',key)

--若已经点赞100篇了，则移除最早点赞的那篇
if size >= 100 then
    redis.call('ZPOPMIN',key)
end

--添加新的笔记点赞关系
redis.call('ZADD',key,timestamp,noteId)
return 0