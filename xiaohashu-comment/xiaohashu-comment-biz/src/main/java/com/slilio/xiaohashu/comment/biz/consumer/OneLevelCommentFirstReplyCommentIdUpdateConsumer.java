package com.slilio.xiaohashu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.enums.CommentLevelEnum;
import com.slilio.xiaohashu.comment.biz.model.dto.CountPublishCommentMqDTO;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存大小
          .batchSize(10000) // 批量处理大小
          .linger(Duration.ofSeconds(1)) // 等待时间
          .setConsumerEx(this::consumeMessage) // 消费方法
          .build();

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

    //todo
  }
}
