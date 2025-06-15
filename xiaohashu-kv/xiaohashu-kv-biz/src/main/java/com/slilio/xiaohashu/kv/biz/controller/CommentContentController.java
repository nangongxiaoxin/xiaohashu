package com.slilio.xiaohashu.kv.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.biz.service.CommentContentService;
import com.slilio.xiaohashu.kv.dto.req.BatchAddCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.BatchFindCommentContentReqDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@RestController
@RequestMapping("/kv")
@Slf4j
public class CommentContentController {
  @Resource private CommentContentService commentContentService;

  @PostMapping(value = "/comment/content/batchAdd")
  @ApiOperationLog(description = "批量存储评论内容")
  public Response<?> batchAddCommentContent(
      @Valid @RequestBody BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {
    return commentContentService.batchAddCommentContent(batchAddCommentContentReqDTO);
  }

  @PostMapping(value = "/comment/content/batchFind")
  @ApiOperationLog(description = "批量查询评论内容")
  public Response<?> batchFindCommentContent(
      @Validated @RequestBody BatchFindCommentContentReqDTO batchFindCommentContentReqDTO) {
    return commentContentService.batchFindCommentContent(batchFindCommentContentReqDTO);
  }
}
