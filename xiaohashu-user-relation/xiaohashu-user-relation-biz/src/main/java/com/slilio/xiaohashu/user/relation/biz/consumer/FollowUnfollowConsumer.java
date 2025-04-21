package com.slilio.xiaohashu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.user.relation.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.slilio.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: slilio @CreateTime: 2025-04-21 @Description: 消费者类 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu-group", // group组
    topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW // 消费者的Topic主题，
    )
@Slf4j
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
  @Resource private FollowingDOMapper followingDOMapper;
  @Resource private FansDOMapper fansDOMapper;
  @Resource private TransactionTemplate transactionTemplate;

  @Resource private RateLimiter rateLimiter;

  @Override
  public void onMessage(Message message) {
    // 流量消峰
    rateLimiter.acquire();

    // 消息体
    String bodyJsonStr = new String(message.getBody());
    // 标签
    String tags = message.getTags();

    log.info("===》 FollowUnfollowConsumer 消费了消息{}，tags：{}", bodyJsonStr, tags);

    // 根据MQ标签，判断操作类型
    if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) {
      // 关注
      handleFollTagMessage(bodyJsonStr);
    } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) {
      // 取关
      // todo
    }
  }

  /**
   * 关注
   *
   * @param bodyJsonStr
   */
  private void handleFollTagMessage(String bodyJsonStr) {
    // 将消息体 Json字符串转为DTO对象
    FollowUserMqDTO followUserMqDTO = JsonUtils.parseObject(bodyJsonStr, FollowUserMqDTO.class);

    // 判空
    if (Objects.isNull(followUserMqDTO)) {
      return;
    }

    // 幂等性：通过联合唯一索引保证
    Long userId = followUserMqDTO.getUserId();
    Long followUserId = followUserMqDTO.getFollowUserId();
    LocalDateTime createTime = followUserMqDTO.getCreateTime();

    // 编程式事务提交
    boolean isSuccess =
        Boolean.TRUE.equals(
            transactionTemplate.execute(
                status -> {
                  try {
                    // 关注成功需往数据库添加两条记录

                    // 关注表：一条记录
                    int count =
                        followingDOMapper.insert(
                            FollowingDO.builder()
                                .userId(userId)
                                .followingUserId(followUserId)
                                .createTime(createTime)
                                .build());

                    // 粉丝表：一条记录
                    if (count > 0) {
                      fansDOMapper.insert(
                          FansDO.builder()
                              .userId(followUserId)
                              .fansUserId(userId)
                              .createTime(createTime)
                              .build());
                    }
                    return true;
                  } catch (Exception ex) {
                    status.setRollbackOnly(); // 标记为事务回滚
                    log.error("", ex);
                  }
                  return false;
                }));
    log.info("## 数据库添加记录结果：{}", isSuccess);
    // TODO ：更新Redis中被关注用户的ZSET粉丝列表
  }
}
