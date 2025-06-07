package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.count.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-07 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_NOTE_COMMENT, // group
    topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT // 主题
    )
@Slf4j
public class CountNoteCommentConsumer implements RocketMQListener<String> {
  @Resource private NoteCountDOMapper noteCountDOMapper;

  // 聚合
  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存队列的最大容量
          .batchSize(1000) // 一批次最多聚合1000条数据
          .linger(Duration.ofSeconds(1)) // 1s聚合一次
          .setConsumerEx(this::consumeMessage) // 设置消费者方法
          .build();

  @Override
  public void onMessage(String body) {
    // 往bufferTrigger中添加数据
    bufferTrigger.enqueue(body);
  }

  private void consumeMessage(List<String> bodys) {
    log.info("==> 【笔记评论数】聚合消息, size: {}", bodys.size());
    log.info("==> 【笔记评论数】聚合消息, {}", JsonUtils.toJsonString(bodys));

    // 将聚合后的消息体Json转List<CountPublishCommentMqDTO>
    List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();

    bodys.forEach(
        body -> {
          try {
            List<CountPublishCommentMqDTO> list =
                JsonUtils.parseList(body, CountPublishCommentMqDTO.class);
            countPublishCommentMqDTOList.addAll(list);
          } catch (Exception e) {
            log.error("", e);
          }
        });

    // 按照笔记ID进行分组
    Map<Long, List<CountPublishCommentMqDTO>> groupMap =
        countPublishCommentMqDTOList.stream()
            .collect(Collectors.groupingBy(CountPublishCommentMqDTO::getNoteId));

    // 循环分组字典
    for (Map.Entry<Long, List<CountPublishCommentMqDTO>> entry : groupMap.entrySet()) {
      // 笔记ID
      Long noteId = entry.getKey();
      // 评论数
      int count = CollUtil.size(entry.getValue());

      // 若评论数大于0，则执行更新操作：累计评论总数
      if (count > 0) {
        noteCountDOMapper.insertOrUpdateCommentTotalByNoteId(count, noteId);
      }
    }
  }
}
