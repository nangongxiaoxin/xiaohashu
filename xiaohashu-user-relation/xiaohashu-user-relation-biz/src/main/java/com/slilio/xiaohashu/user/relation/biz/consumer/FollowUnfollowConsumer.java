package com.slilio.xiaohashu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.user.relation.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.relation.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.slilio.xiaohashu.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import com.slilio.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.slilio.xiaohashu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: slilio @CreateTime: 2025-04-21 @Description: 消费者类 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu-group" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, // group组
    topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, // 消费者的Topic主题，
    consumeMode = ConsumeMode.ORDERLY // 设置为顺序消费模式
    )
@Slf4j
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
  @Resource private FollowingDOMapper followingDOMapper;
  @Resource private FansDOMapper fansDOMapper;
  @Resource private TransactionTemplate transactionTemplate;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private RocketMQTemplate rocketMQTemplate;

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
      handleUnfollowTagMessage(bodyJsonStr);
    }
  }

  /**
   * 取关
   *
   * @param bodyJsonStr
   */
  private void handleUnfollowTagMessage(String bodyJsonStr) {
    // 将消息体Json字符串转为DTO对象
    UnfollowUserMqDTO unfollowUserMqDTO =
        JsonUtils.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);

    // 判空
    if (Objects.isNull(unfollowUserMqDTO)) {
      return;
    }

    Long userId = unfollowUserMqDTO.getUserId();
    Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
    LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();

    // 编程式提交事务
    boolean isSuccess =
        Boolean.TRUE.equals(
            transactionTemplate.execute(
                status -> {
                  try {
                    // 取关成功需要删除数据库两条记录

                    // 关注表：一条记录
                    int count =
                        followingDOMapper.deleteByUserIdAndFollowingUserId(userId, unfollowUserId);

                    // 粉丝表： 一条记录
                    if (count > 0) {
                      fansDOMapper.deleteByUserIdAndFansUserId(unfollowUserId, userId);
                    }

                    return true;
                  } catch (Exception ex) {
                    status.setRollbackOnly(); // 标记事务为回滚
                    log.error("", ex);

                    return false;
                  }
                }));
    // 若数据库删除成功，更新 Redis，将自己从被取注用户的 ZSet 粉丝列表删除
    if (isSuccess) {
      // 被取关用户的粉丝列表Redis Key
      String fansRedisKey = RedisKeyConstants.buildUserFansKey(unfollowUserId);
      // 删除指定粉丝
      redisTemplate.opsForZSet().remove(fansRedisKey, userId);

      // todo 粉丝ZSET存在逻辑判断和再次新增粉丝Zset bug

      //  发送MQ通知计数服务，统计关注数
      // 构建消息体
      CountFollowUnfollowMqDTO countFollowUnfollowMqDTO =
          CountFollowUnfollowMqDTO.builder().userId(userId).targetUserId(unfollowUserId).build();
      // 发送MQ
      sendMQ(countFollowUnfollowMqDTO);
    }
  }

  /**
   * 发送MQ通知计数服务
   *
   * @param countFollowUnfollowMqDTO
   */
  private void sendMQ(CountFollowUnfollowMqDTO countFollowUnfollowMqDTO) {
    // 构建消息对象，并将DTO转成json字符串设置到消息体中
    org.springframework.messaging.Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(countFollowUnfollowMqDTO)).build();

    // 异步发送MQ消息：统计关注数
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_COUNT_FOLLOWING,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===> 【计数服务：关注数】MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===> 【计数服务：关注数】MQ发送异常：", throwable);
          }
        });

    // 发送MQ通知计数服务：统计粉丝数
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_COUNT_FANS,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===> 【计数服务：粉丝数】MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===> 【计数服务：粉丝数】MQ发送异常：", throwable);
          }
        });
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

    // 若数据库操作成功，更新Redis中被关注用户的ZSet粉丝列表
    if (isSuccess) {
      // Lua脚本
      DefaultRedisScript<Long> script = new DefaultRedisScript<>();
      script.setScriptSource(
          new ResourceScriptSource(
              new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
      script.setResultType(Long.class);

      // 时间戳
      long timestamp = DateUtils.localDateTime2Timestamp(createTime);
      // 构建被关注用户的粉丝列表Redis key
      String fansRedisKey = RedisKeyConstants.buildUserFansKey(followUserId);
      // 执行脚本
      redisTemplate.execute(script, Collections.singletonList(fansRedisKey), userId, timestamp);

      // todo 粉丝ZSET存在逻辑判断和再次新增粉丝Zset bug

      // 发送MQ通知计数服务，统计关注数
      // 构建消息体
      CountFollowUnfollowMqDTO countFollowUnfollowMqDTO =
          CountFollowUnfollowMqDTO.builder().userId(userId).targetUserId(followUserId).build();
      // 发送MQ
      sendMQ(countFollowUnfollowMqDTO);
    }
  }
}
