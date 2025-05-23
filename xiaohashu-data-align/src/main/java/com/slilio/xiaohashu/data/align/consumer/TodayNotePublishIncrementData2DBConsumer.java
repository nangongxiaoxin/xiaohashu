package com.slilio.xiaohashu.data.align.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.data.align.constant.MQConstants;
import com.slilio.xiaohashu.data.align.constant.RedisKeyConstants;
import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.InsertMapper;
import com.slilio.xiaohashu.data.align.model.dto.NoteOperateMqDTO;
import jakarta.annotation.Resource;
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
    consumerGroup = "xiaohashu_group_data_align_" + MQConstants.TOPIC_NOTE_OPERATE, // group组
    topic = MQConstants.TOPIC_NOTE_OPERATE)
@Slf4j
public class TodayNotePublishIncrementData2DBConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private InsertMapper insertMapper;

  @Value("${table.shards}")
  private int tableShards;

  @Override
  public void onMessage(String body) {
    log.info("## TodayNotePublishIncrementData2DBConsumer 消费到了 MQ: {}", body);

    // 消息体json转DTO对象
    NoteOperateMqDTO noteOperateMqDTO = JsonUtils.parseObject(body, NoteOperateMqDTO.class);

    if (Objects.isNull(noteOperateMqDTO)) {
      return;
    }

    // 发布、被删除笔记发布者ID
    Long noteCreatorId = noteOperateMqDTO.getCreatorId();
    // 今日日期
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串

    String bloomKey = RedisKeyConstants.buildBloomUserNoteOperateListKey(date);

    // 1.布隆过滤器判断该日增量数据是否已经记录
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // Lua脚本
    script.setScriptSource(
        new ResourceScriptSource(
            new ClassPathResource("/lua/bloom_today_user_note_publish_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteCreatorId);

    // 若布隆过滤器不存在（绝对正确）
    if (Objects.equals(result, 0L)) {
      //  2.若无，才会落库，减轻数据库压力

      // 根据分片总数，取模，分别获取对应的分片序号
      long userIdHashKey = noteCreatorId % tableShards;

      // 将日增量变更数据，写入日增量表中
      // - t_data_align_note_publish_count_temp_日期_分片序号
      insertMapper.insert2DataAlignUserNotePublishCountTempTable(
          TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);

      // 3。数据库写入成功后，再添加布隆过滤器中
      RedisScript<Long> bloomAddScript =
          RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
      redisTemplate.execute(bloomAddScript, Collections.singletonList(bloomKey), noteCreatorId);
    }
  }
}
