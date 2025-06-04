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
;

  private final String errorCode;
  private final String errorMessage;
};
