package com.slilio.xiaohashu.data.align.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.data.align.constant.MQConstants;
import com.slilio.xiaohashu.data.align.constant.RedisKeyConstants;
import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.InsertMapper;
import com.slilio.xiaohashu.data.align.model.dto.CollectUnCollectNoteMqDTO;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT, // group组
    topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT // 主题topic
    )
@Slf4j
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private TransactionTemplate transactionTemplate;
  @Resource private InsertMapper insertMapper;

  // 表总分片数
  @Value("${table.shards}")
  private int tableShards;

  @Override
  public void onMessage(String body) {
    log.info("## TodayNoteCollectIncrementData2DBConsumer 消费到了 MQ: {}", body);

    // 消息体json转dto对象
    CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO =
        JsonUtils.parseObject(body, CollectUnCollectNoteMqDTO.class);

    if (Objects.isNull(collectUnCollectNoteMqDTO)) {
      return;
    }

    // 被收藏、取消收藏的笔记ID
    Long noteId = collectUnCollectNoteMqDTO.getNoteId();
    // 笔记发布者ID
    Long noteCreatorId = collectUnCollectNoteMqDTO.getNoteCreatorId();

    // 今日日期
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串

    // ------------------------- 笔记的收藏数变更记录 -------------------------

    String noteBloomKey = RedisKeyConstants.buildBloomUserNoteCollectNoteIdListKey(date);

    // 1.布隆过滤器判断改日增量数据是否已经记录
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_collect_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result = redisTemplate.execute(script, Collections.singletonList(noteBloomKey), noteId);

    // Lua脚本：添加到布隆过滤器
    RedisScript<Long> bloomAddScript =
        RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    // 若布隆过滤器判断不存在（绝对正确）
    if (Objects.equals(result, 0L)) {
      //  2.若无，才会落库，减轻数据库压力

      // 根据分片总数，取模，分别获取对应的分片序号
      long noteIdHashKey = noteId % tableShards;

      try {
        // 将日增变更数据，分别写入表
        // - t_data_align_note_collect_count_temp_日期_分片序号
        insertMapper.insert2DataAlignNoteCollectCountTempTable(
            TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);

      } catch (Exception e) {
        log.error("", e);
      }

      // 3. 数据库写入成功后，再添加布隆过滤器中

      redisTemplate.execute(bloomAddScript, Collections.singletonList(noteBloomKey), noteId);
    }

    // ------------------------- 笔记发布者获得的收藏数变更记录 -------------------------

    // 笔记发布者对应的BloomKey
    String userBloomKey = RedisKeyConstants.buildBloomUserNoteCollectUserIdListKey(date);
    // 执行lua脚本
    result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), noteCreatorId);

    // 若布隆过滤器判断不存在（绝对正确）
    if (Objects.equals(result, 0L)) {
      //  2.若无，才会落库，减轻数据库压力

      // 根据分片总数，取模，分别获取对应的分片序号
      long userIdHashKey = noteCreatorId % tableShards;
      try {
        // 将日增变更数据，写入表
        // - t_data_align_user_collect_count_temp_日期_分片序号
        insertMapper.insert2DataAlignUserCollectCountTempTable(
            TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
      } catch (Exception e) {
        log.error("", e);
      }

      // 3. 数据库写入成功后，再添加布隆过滤器中

      redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), noteCreatorId);
    }
  }
}
