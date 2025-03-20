package com.slilio.xiaohashu.gateway.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.gateway.enums.ResponseCodeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
  @Resource private ObjectMapper objectMapper;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    // 获取响应对象
    ServerHttpResponse response = exchange.getResponse();

    log.error("==》 全局异常捕获：", ex);

    // 响应参数
    Response<?> result;
    // 根据捕获的异常类型，设置不同的响应状态码和响应信息
    if (ex instanceof NotLoginException) { // 未登录异常
      // 权限认证失败时，设置401状态码
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      // 构建响应结果
      result = Response.fail(ResponseCodeEnum.UNAUTHORIZED.getErrorCode(), ex.getMessage());
    } else if (ex instanceof NotPermissionException) { // 无权限异常
      // 权限认证失败时，设置401状态码
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      // 构建响应结果
      result =
          Response.fail(
              ResponseCodeEnum.UNAUTHORIZED.getErrorCode(),
              ResponseCodeEnum.UNAUTHORIZED.getErrorMessage());
    } else { // 其他异常，则统一提示“系统繁忙” 错误
      result = Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }

    // 设置响应头的内容为 application/json;charset=utf-8,表示响应体为json格式
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
    // 设置body响应体
    return response.writeWith(
        Mono.fromSupplier(
            () -> { // 使用Mono.formSupplier 创建响应体
              DataBufferFactory bufferFactory = response.bufferFactory();
              try {
                // 使用objectMapper将result对象转换为json字节数组
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
              } catch (Exception e) {
                // 如果转换的过程出现异常 则返回空字节数组
                return bufferFactory.wrap(new byte[0]);
              }
            }));
  }
}
