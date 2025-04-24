--Lua脚本 ：校验并移除关注关系

local key = KEYS[1] --操作的redis key
local unfollowUserId = ARGV[1] --关注的用户ID

--使用EXISTS命令检查ZSET是否存在
local exists = redis.call('EXISTS',key)
if exists == 0 then
    return -1
end


--校验目标用户是否被关注
local score = redis.call('ZSCORE',key,unfollowUserId)
if score == false or score == nil then
    return -4
end

--ZREM 删除关注关系
redis.call('ZREM',key,unfollowUserId)
return 0