local key = KEYS[1] --操作的RedisKey
local noteId = ARGV[1] --笔记ID

--使用EXISTS命令检查布隆过滤器是否存在
local exists = redis.call('EXISTS',key)
if exists == 0 then
    return -1
end

 --校验该笔记是否被点赞过（1表示已经点赞，0表示未点赞）
 return redis.call('BF.EXISTS',key,noteId)