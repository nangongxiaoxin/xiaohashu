package com.slilio.framework.biz.context.interceptor;

import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.constant.GlobalConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate requestTemplate) {
    // 获取当前上下文中的用户ID
    Long userId = LoginUserContextHolder.getUserId();

    // 若不为空，则添加到请求头中
    if (Objects.nonNull(userId)) {
      requestTemplate.header(GlobalConstants.USER_ID, String.valueOf(userId));
      log.info("############ feign请求设置请求头 userId：{}", userId);
    }
  }
}
