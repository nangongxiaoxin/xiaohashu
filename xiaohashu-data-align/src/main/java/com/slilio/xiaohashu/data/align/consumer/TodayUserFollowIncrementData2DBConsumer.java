package com.slilio.xiaohashu.data.align.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.data.align.constant.MQConstants;
import com.slilio.xiaohashu.data.align.constant.RedisKeyConstants;
import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.InsertMapper;
import com.slilio.xiaohashu.data.align.model.dto.FollowUnfollowMqDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-23 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_data_align_" + MQConstants.TOPIC_COUNT_FOLLOWING, // group组
    topic = MQConstants.TOPIC_COUNT_FOLLOWING // 主题 topic
    )
@Slf4j
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {

  private final RedisTemplate<String, Object> redisTemplate;
  private final InsertMapper insertMapper;

  @Value("${table.shards}")
  private int tableShards;

  public TodayUserFollowIncrementData2DBConsumer(
      RedisTemplate<String, Object> redisTemplate, InsertMapper insertMapper) {
    this.redisTemplate = redisTemplate;
    this.insertMapper = insertMapper;
  }

  @Override
  public void onMessage(String body) {
    log.info("## TodayUserFollowIncrementData2DBConsumer 消费到了 MQ: {}", body);

    // 消息体字符串转DTO对象
    FollowUnfollowMqDTO followUnfollowMqDTO =
        JsonUtils.parseObject(body, FollowUnfollowMqDTO.class);

    if (Objects.isNull(followUnfollowMqDTO)) {
      return;
    }

    // 关注、取关操作
    // 源用户
    Long userId = followUnfollowMqDTO.getUserId();
    // 目标用户
    Long targetUserId = followUnfollowMqDTO.getTargetUserId();

    // 今日日期
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串

    // --源用户--
    String userBloomKey = RedisKeyConstants.buildBloomUserFollowListKey(date);
    // 1。布隆过滤器判断该日增量数据是否已经记录
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_user_follow_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), userId);

    // Lua脚本：添加到布隆过滤器
    RedisScript<Long> bloomAddScript =
        RedisScript.of("return redis.call('BF.ADD',KEYS[1],ARGV[1])", Long.class);
    // 若布隆过滤器不存在（绝对正确）
    if (Objects.equals(result, 0L)) {
      // 若无，才会落库，减轻数据库压力
      // 根据分片总数，取模，分别获取对应的分片序号
      long userIdHashKey = userId % tableShards;

      try {
        // 将日增量变更数据，分别写入两张表 - t_data_align_following_count_temp_日期_分片序号
        insertMapper.insert2DataAlignUserFollowingCountTempTable(
            TableConstants.buildTableNameSuffix(date, userIdHashKey), userId);
      } catch (Exception e) {
        log.error("", e);
      }

      //  数据库写入成功后，再添加布隆过滤器中
      redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), userId);
    }

    // --目标用户的粉丝数--
    String targetUserBloomKey = RedisKeyConstants.buildBloomUserFansListKey(date);
    //  布隆过滤器判断该日增量数据是否已经记录
    result =
        redisTemplate.execute(script, Collections.singletonList(targetUserBloomKey), targetUserId);

    // 若布隆过滤器不存在（绝对正确）
    if (Objects.equals(result, 0L)) {
      // 若无，才会落库，减轻数据库压力
      // 根据分片总数，取模，分别获取对应的分片序号
      long targetUserIdHashKey = targetUserId % tableShards;

      try {
        // 将日增量变更数据，写入表 t_data_align_fans_count_temp_日期_分片序号
        insertMapper.insert2DataAlignUserFansCountTempTable(
            TableConstants.buildTableNameSuffix(date, targetUserIdHashKey), targetUserId);
      } catch (Exception e) {
        log.error("", e);
      }

      //  数据库写入成功后，再添加布隆过滤器中
      redisTemplate.execute(
          bloomAddScript, Collections.singletonList(targetUserBloomKey), targetUserId);
    }
  }
}
