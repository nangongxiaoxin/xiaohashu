package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.slilio.xiaohashu.count.biz.model.dto.AggregationCountLikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
  @Resource private UserCountDOMapper userCountDOMapper;
  @Resource private TransactionTemplate transactionTemplate;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  @Override
  public void onMessage(String body) {
    // 流量削峰
    rateLimiter.acquire();

    log.info("## 消费到了 MQ 【计数: 笔记点赞数入库】, {}...", body);

    List<AggregationCountLikeUnlikeNoteMqDTO> countList = null;
    try {
      countList = JsonUtils.parseList(body, AggregationCountLikeUnlikeNoteMqDTO.class);
    } catch (Exception e) {
      log.error("## 解析 JSON 字符串异常", e);
    }

    if (CollUtil.isNotEmpty(countList)) {
      // 判断数据库中t_user_count和t_note_count表，若笔记计数记录不存在，则插入；若记录已存在，则直接更新
      countList.forEach(
          item -> {
            Long creatorId = item.getCreatorId();
            Long noteId = item.getNoteId();
            Integer count = item.getCount();

            // 编程式事务,保证两条语句的原子性
            transactionTemplate.execute(
                status -> {
                  try {
                    noteCountDOMapper.insertOrUpdateLikeTotalByNoteId(count, noteId);
                    userCountDOMapper.insertOrUpdateLikeTotalByUserId(count, creatorId);
                    return true;
                  } catch (Exception ex) {
                    status.setRollbackOnly(); // 标记事务为回滚
                    log.error("", ex);
                  }
                  return false;
                });
          });
    }
  }
}
