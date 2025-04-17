package com.slilio.xiaohashu.user.relation.biz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @Author: slilio @CreateTime: 2025-04-18 @Description: Redis模板配置类 @Version: 1.0
 */
@Configuration
public class RedisTemplateConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    // 设置RedisTemplate的连接工厂
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    // 使用StringRedisSerializer来序列化和反序列化redis的 key 值，确保key是可读的字符串
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());

    // 使用Jackson2JsonRedisSerializer 来序列化和反序列化redis的 value 值，确保存储的是json格式
    Jackson2JsonRedisSerializer<Object> serializer =
        new Jackson2JsonRedisSerializer<>(Object.class);
    redisTemplate.setValueSerializer(serializer);
    redisTemplate.setHashValueSerializer(serializer);

    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }
}
