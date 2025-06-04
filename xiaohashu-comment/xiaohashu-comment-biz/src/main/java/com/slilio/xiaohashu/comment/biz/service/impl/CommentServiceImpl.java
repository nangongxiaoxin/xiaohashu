package com.slilio.xiaohashu.comment.biz.service.impl;

import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.model.dto.PublishCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.model.vo.PublishCommentReqVO;
import com.slilio.xiaohashu.comment.biz.service.CommentService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

  @Resource private RocketMQTemplate rocketMQTemplate;

  /**
   * 发布评论
   *
   * @param publishCommentReqVO
   * @return
   */
  @Override
  public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
    // 评论正文
    String content = publishCommentReqVO.getContent();
    // 附近图片
    String imageUrl = publishCommentReqVO.getImgUrl();

    // 评论内容和图片不能同时为空
    Preconditions.checkArgument(
        StringUtils.isNotBlank(content) || StringUtils.isBlank(imageUrl), "评论内容和图片不能同时为空");

    // 发布者ID
    Long creatorId = LoginUserContextHolder.getUserId();

    // 发送MQ
    // 构建消息体DTO
    PublishCommentMqDTO publishCommentMqDTO =
        PublishCommentMqDTO.builder()
            .noteId(publishCommentReqVO.getNoteId())
            .content(content)
            .imageUrl(imageUrl)
            .replyCommentId(publishCommentReqVO.getReplyCommentId())
            .createTime(LocalDateTime.now())
            .creatorId(creatorId)
            .build();

    // 构建消息对象，将DTO转换为JSON字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(publishCommentMqDTO)).build();

    // 异步发送MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_PUBLISH_COMMENT,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【评论发布】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【评论发布】MQ 发送异常：", throwable);
          }
        });

    return Response.success();
  }
}
