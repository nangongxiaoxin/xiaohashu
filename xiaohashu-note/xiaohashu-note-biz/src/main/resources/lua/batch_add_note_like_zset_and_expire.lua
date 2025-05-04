--操作的Key
local key  = KEYS[1]

--准备批量添加数据的参照表
local zaddArgs = {}

--遍历ARGV参数，将分数和值按顺序插入到zaddArgs变量中
for i = 1, #ARGV - 1,2 do
    table.insert(zaddArgs,ARGV[i]) --分数（点赞时间）
    table.insert(zaddArgs,ARGV[i+1]) --值（笔记ID）
end

--调用ZADD批量插入数据
redis.call('ZADD',key,unpack(zaddArgs))

--设置ZSet的过期时间
local expireTime = ARGV[#ARGV] --最后一参数为过期时间
redis.call('EXPIRE',key,expireTime)

return 0