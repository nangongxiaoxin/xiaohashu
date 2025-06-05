package com.slilio.xiaohashu.kv.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.dto.req.BatchAddCommentContentReqDTO;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
public interface CommentContentService {
  /**
   * 批量添加评论内容
   *
   * @param batchAddCommentContentReqDTO
   * @return
   */
  Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);
}
