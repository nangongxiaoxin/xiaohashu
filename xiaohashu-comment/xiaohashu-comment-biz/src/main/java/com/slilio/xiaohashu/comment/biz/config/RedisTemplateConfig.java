package com.slilio.xiaohashu.comment.biz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @Author: slilio @CreateTime: 2025-06-11 @Description: @Version: 1.0
 */
@Configuration
public class RedisTemplateConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

    // 设置RedisTemplate的连接工厂
    redisTemplate.setConnectionFactory(connectionFactory);

    // 使用StringRedisSerializer来序列化和反序列化redis的key值，确保key是可读的字符串
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());

    // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值，确保value是可读的JSON
    Jackson2JsonRedisSerializer<Object> serializer =
        new Jackson2JsonRedisSerializer<>(Object.class);
    redisTemplate.setValueSerializer(serializer);
    redisTemplate.setHashValueSerializer(serializer);

    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }
}
