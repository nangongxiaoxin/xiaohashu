package com.slilio.xiaohashu.comment.biz.service;

import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.comment.biz.model.vo.FindCommentItemRspVO;
import com.slilio.xiaohashu.comment.biz.model.vo.FindCommentPageListReqVO;
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

  /**
   * 评论列表分页查询
   *
   * @param findCommentPageListReqVO
   * @return
   */
  PageResponse<FindCommentItemRspVO> findCommentPageList(
      FindCommentPageListReqVO findCommentPageListReqVO);
}
