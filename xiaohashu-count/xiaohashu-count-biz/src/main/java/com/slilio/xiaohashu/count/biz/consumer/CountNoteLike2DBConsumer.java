package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.NoteCountDOMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-08 @Description: 笔记计数消费者类 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB, // 组
    topic = MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB // 主题 topic
    )
@Slf4j
public class CountNoteLike2DBConsumer implements RocketMQListener<String> {
  @Resource private NoteCountDOMapper noteCountDOMapper;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  @Override
  public void onMessage(String body) {
    // 流量削峰
    rateLimiter.acquire();

    log.info("## 消费到了MQ 【计数：将笔记点赞数入库】，{}", body);

    Map<Long, Integer> countMap = null;
    try {
      countMap = JsonUtils.parseMap(body, Long.class, Integer.class);
    } catch (Exception e) {
      log.error("## 解析Json字符串异常");
    }

    if (CollUtil.isNotEmpty(countMap)) {
      // 数据库，数据入库
      countMap.forEach(
          (k, v) -> {
            noteCountDOMapper.insertOrUpdateLikeTotalByNoteId(v, k);
          });
    }
  }
}
