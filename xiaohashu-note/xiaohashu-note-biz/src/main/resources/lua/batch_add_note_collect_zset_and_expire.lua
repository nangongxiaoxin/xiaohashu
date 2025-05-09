local key = KEYS[1]

--准备批量添加数据的参数集
local zaddArgs = {}

--遍历ARGV参数，将分数和值按顺序插入到zaddArgs中
for i = 1,#ARGV - 1, 2 do
    table.insert(zaddArgs,ARGV[i]) -- 分数（收藏时间）
    table.insert(zaddArgs,ARGV[i+1]) --值（笔记ID）
end

redis.call('ZADD',key,unpack(zaddArgs))

local expireTime = ARGV[#ARGV]
redis.call('EXPIRE',key,expireTime)

return 0