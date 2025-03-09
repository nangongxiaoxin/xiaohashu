package com.slilio.xiaohashu.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@Slf4j
class RedisTests {

  @Resource private RedisTemplate<String, Object> redisTemplate;

  // 添加
  @Test
  void testSetKeyValue() {
    // 添加一个key为name，value值为 啊啊啊
    redisTemplate.opsForValue().set("name", "啊啊啊");
  }

  // 判断是否存在
  @Test
  void testHashKey() {
    log.info("key 是否存在：{}", Boolean.TRUE.equals(redisTemplate.hasKey("name")));
  }

  // 值获取
  @Test
  void testGetValue() {
    log.info("name 的值为：{}", redisTemplate.opsForValue().get("name"));
  }

  // 删除key
  @Test
  void testDeleteKey() {
    redisTemplate.delete("name");
  }
}
