package com.slilio.xiaohashu.auth.filter;

import com.slilio.framework.common.constant.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 提取请求头中的用户 ID 保存到上下文中，以方便后续使用 */
@Component
@Slf4j
public class HeaderUserId2ContextFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // 从请求中获取用户ID
    String userId = request.getHeader(GlobalConstants.USER_ID);

    log.info("## HeaderUserId2ContextFilter过滤器中,用户ID：{}", userId);

    // 判断请求中是否存在用户ID
    if (StringUtils.isBlank(userId)) {
      // 若为空，则直接放行
      filterChain.doFilter(request, response);
      return;
    }

    // 如果header中存在userId,则设置到ThreadLocal中
    log.info("===》 设置userId到ThreadLocal中，用户ID：{}", userId);
    LoginUserContextHolder.setUserId(userId);

    try {
      // 将请求和响应传递给过滤器链中的下一个过滤器
      filterChain.doFilter(request, response);
    } finally {
      // 一定要删除ThreadLocal，防止内存泄漏
      LoginUserContextHolder.remove();
      log.info("===》 删除ThreadLocal，userId：{}", userId);
    }
  }
}
