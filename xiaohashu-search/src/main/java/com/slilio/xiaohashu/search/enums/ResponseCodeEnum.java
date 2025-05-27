package com.slilio.xiaohashu.search.enums;

import com.slilio.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
  // ----------- 通用异常状态码 -----------
  SYSTEM_ERROR("SEARCH-10000", "出错了，后台小哥正在努力修复..."),
  PARAM_NOT_VALID("SEARCH-10001", "参数错误"),
// ----------- 通用异常状态码 -----------
;

  // 错误码
  private final String errorCode;
  // 错误信息
  private final String errorMessage;
}
