package com.slilio.xiaohashu.comment.biz.enums;

import com.slilio.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
  // ----------- 通用异常状态码 -----------
  SYSTEM_ERROR("COMMENT-10000", "出错了，后台小哥正在修复中..."),
  PARAM_NOT_VALID("COMMENT-10001", "参数错误"),

  // ----------- 业务异常状态码 -----------
  COMMENT_NOT_FOUND("COMMENT-20001", "此评论不存在"),
  PARENT_COMMENT_NOT_FOUNT("COMMENT-20000", "此父评论不存在"),
  COMMENT_ALREADY_LIKED("COMMENT_20002", "您已经点赞过该评论"),
  COMMENT_NOT_LIKED("COMMENT-20003", "您未点赞该评论，无法取消点赞"),
  ;

  private final String errorCode;
  private final String errorMessage;
};
