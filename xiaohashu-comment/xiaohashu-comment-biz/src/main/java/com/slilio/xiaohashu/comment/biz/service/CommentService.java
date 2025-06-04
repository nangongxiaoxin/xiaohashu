package com.slilio.xiaohashu.comment.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.comment.biz.model.vo.PublishCommentReqVO;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
public interface CommentService {

  /**
   * 发布评论
   *
   * @param publishCommentReqVO
   * @return
   */
  Response<?> publishComment(PublishCommentReqVO publishCommentReqVO);
}
