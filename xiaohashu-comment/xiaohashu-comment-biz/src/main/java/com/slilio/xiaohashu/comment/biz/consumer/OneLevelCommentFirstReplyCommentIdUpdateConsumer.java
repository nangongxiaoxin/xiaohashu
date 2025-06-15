package com.slilio.xiaohashu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.CommentLevelEnum;
import com.slilio.xiaohashu.comment.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-09 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup =
        "xiaohashu_group_first_reply_comment_id" + MQConstants.TOPIC_COUNT_NOTE_COMMENT, // group组
    topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT // 主题Topic
    )
@Slf4j
public class OneLevelCommentFirstReplyCommentIdUpdateConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;

  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存大小
          .batchSize(10000) // 批量处理大小
          .linger(Duration.ofSeconds(1)) // 等待时间
          .setConsumerEx(this::consumeMessage) // 消费方法
          .build();
  @Resource private CommentDOMapper commentDOMapper;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Override
  public void onMessage(String body) {
    // 往BufferTrigger中添加元素
    bufferTrigger.enqueue(body);
  }

  private void consumeMessage(List<String> bodys) {
    log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, size: {}", bodys.size());
    log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, {}", JsonUtils.toJsonString(bodys));

    // 将聚合后的json消息转为List<CountPublishCommentMqDTO>
    List<CountPublishCommentMqDTO> publishCommentMqDTOS = Lists.newArrayList();
    bodys.forEach(
        body -> {
          try {
            List<CountPublishCommentMqDTO> list =
                JsonUtils.parseList(body, CountPublishCommentMqDTO.class);
            publishCommentMqDTOS.addAll(list);
          } catch (Exception e) {
            log.error("", e);
          }
        });

    // 过滤出二级评论的parent_id(即一级评论ID)，并去重，需要更新对应一级评论的first_reply_comment_id
    List<Long> parentIds =
        publishCommentMqDTOS.stream()
            .filter(
                publishCommentMqDTO ->
                    Objects.equals(publishCommentMqDTO.getLevel(), CommentLevelEnum.TWO.getCode()))
            .map(CountPublishCommentMqDTO::getParentId)
            .distinct() // 去重
            .toList();

    if (CollUtil.isEmpty(parentIds)) {
      return;
    }

    // 构建RedisKey
    List<String> keys =
        parentIds.stream().map(RedisKeyConstants::buildHaveFirstReplyCommentKey).toList();

    // 批量查询Redis
    List<Object> values = redisTemplate.opsForValue().multiGet(keys);

    // 提取redis中不存在的评论ID
    List<Long> missingCommentIds = Lists.newArrayList();
    for (int i = 0; i < values.size(); i++) {
      if (Objects.isNull(values.get(i))) {
        missingCommentIds.add(parentIds.get(i));
      }
    }

    // 存在一级评论ID，说明表中对应记录的first_reply_comment_id已经有值
    if (CollUtil.isNotEmpty(missingCommentIds)) {
      // 不存在的，则需进一步查询数据库来确定，是否需要更新记录对应的first_reply_comment_id值
      // 批量去数据库中查询
      List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(missingCommentIds);

      // 异步将first_reply_comment_id不为0的一级评论ID，同步到redis中
      threadPoolTaskExecutor.submit(
          () -> {
            List<Long> needSyncCommentIds =
                commentDOS.stream()
                    .filter(commentDO -> commentDO.getFirstReplyCommentId() != 0)
                    .map(CommentDO::getId)
                    .toList();

            sync2Redis(needSyncCommentIds);
          });

      // 过滤出值为0的，都需要更新其first_reply_comment_id
      List<CommentDO> needUpdateCommentDOS =
          commentDOS.stream().filter(commentDO -> commentDO.getFirstReplyCommentId() == 0).toList();

      needUpdateCommentDOS.forEach(
          needUpdateCommentDO -> {
            // 一级评论
            Long needUpdateCommentId = needUpdateCommentDO.getId();

            // 查询数据库，拿到一级评论最早回复的那条评论
            CommentDO earliestCommentDO =
                commentDOMapper.selectEarliestCommentByParentId(needUpdateCommentId);

            if (Objects.nonNull(earliestCommentDO)) {
              // 最早回复的那条评论ID
              Long earliestCommentId = earliestCommentDO.getId();

              // 更新其第一级评论的first_reply_comment_id
              commentDOMapper.updateFirstReplyCommentIdByPrimaryKey(
                  earliestCommentId, needUpdateCommentId);

              // 异步同步到Redis
              threadPoolTaskExecutor.submit(
                  () -> sync2Redis(Lists.newArrayList(needUpdateCommentId)));
            }
          });
    }
  }

  /**
   * 同步到redis中
   *
   * @param needSyncCommentIds
   */
  private void sync2Redis(List<Long> needSyncCommentIds) {
    // 获取 valueOperations
    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

    // 使用redisTemplate的管道模式，允许在一个操作中批量发送多个命令，防止频繁操作redis
    redisTemplate.executePipelined(
        (RedisCallback<?>)
            (connection) -> {
              needSyncCommentIds.forEach(
                  needSyncCommentId -> {
                    // 构建redisKey
                    String key = RedisKeyConstants.buildHaveFirstReplyCommentKey(needSyncCommentId);

                    // 批量设置值并指定过期时间（5小时以上）
                    valueOperations.set(
                        key, 1, RandomUtil.randomInt(5 * 60 * 60), TimeUnit.SECONDS);
                  });
              return null;
            });
  }
}
