package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.count.biz.model.dto.AggregationCountLikeUnlikeCommentMqDTO;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-23 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_COMMENT_LIKE_2_DB,
    topic = MQConstants.TOPIC_COUNT_COMMENT_LIKE_2_DB)
@Slf4j
public class CountCommentLike2DBConsumer implements RocketMQListener<String> {
  @Resource private CommentDOMapper commentDOMapper;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  @Override
  public void onMessage(String body) {
    // 流量削峰：通过令牌获取，如果没有令牌将会阻塞，直到获得
    rateLimiter.acquire();

    log.info("## 消费到了 MQ 【计数: 评论点赞数入库】, {}...", body);

    List<AggregationCountLikeUnlikeCommentMqDTO> countList = null;

    try {
      countList = JsonUtils.parseList(body, AggregationCountLikeUnlikeCommentMqDTO.class);
    } catch (Exception e) {
      log.error("## 解析 JSON 字符串异常", e);
    }

    if (CollUtil.isNotEmpty(countList)) {
      // 更新评论点赞数
      countList.forEach(
          item -> {
            Long commentId = item.getCommentId();
            Integer count = item.getCount();

            commentDOMapper.updateLikeTotalByCommentId(count, commentId);
          });
    }
  }
}
