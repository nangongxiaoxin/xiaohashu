package com.slilio.xiaohashu.count.biz.enums;

import com.slilio.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
  // ----------- 通用异常状态码 -----------
  SYSTEM_ERROR("COUNT-10000", "出错啦，后台小哥正在努力修复中..."),
  PARAM_NOT_VALID("COUNT-10001", "参数错误"),
  FLOW_LIMIT("COUNT-10002", "操作过于频繁"),
// ----------- 业务异常状态码 -----------
;

  private final String errorCode;
  private final String errorMessage;
}
