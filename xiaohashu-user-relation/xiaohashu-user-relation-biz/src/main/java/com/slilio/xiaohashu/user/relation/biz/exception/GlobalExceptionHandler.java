package com.slilio.xiaohashu.user.relation.biz.exception;

import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author: slilio @CreateTime: 2025-04-18 @Description: 全局异常处理 @Version: 1.0
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  /** 捕获自定义异常错误 */
  @ExceptionHandler({BizException.class})
  @ResponseBody
  public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
    log.warn(
        "{} request fail,errorCode: {},errorMessage: {}",
        request.getRequestURI(),
        e.getErrorCode(),
        e.getMessage());
    return Response.fail(e);
  }

  /** 捕获参数校验异常 */
  @ExceptionHandler({MethodArgumentNotValidException.class})
  @ResponseBody
  public Response<Object> handleMethodArgumentNotValidException(
      HttpServletRequest request, MethodArgumentNotValidException e) {
    // 参数错误异常码
    String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

    // 获取BindingResult
    BindingResult bindingResult = e.getBindingResult();

    StringBuilder sb = new StringBuilder();

    // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确，当前值为：'123qq.com'；
    Optional.ofNullable(bindingResult.getFieldErrors())
        .ifPresent(
            errors -> {
              errors.forEach(
                  error ->
                      sb.append(error.getField())
                          .append(" ")
                          .append(error.getDefaultMessage())
                          .append("，当前值为：'")
                          .append(error.getRejectedValue())
                          .append("'; "));
            });

    // 错误信息
    String errorMessage = sb.toString();

    log.warn(
        "{} request fail,errorCode: {},errorMessage: {}",
        request.getRequestURI(),
        errorCode,
        errorMessage);

    return Response.fail(errorCode, errorMessage);
  }

  /** 其他类型异常 */
  @ExceptionHandler({Exception.class})
  @ResponseBody
  public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
    log.error("{} request error, ", request.getRequestURI(), e);
    return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
  }
}
