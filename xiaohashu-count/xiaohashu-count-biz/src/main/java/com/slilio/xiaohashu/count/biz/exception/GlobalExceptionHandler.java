package com.slilio.xiaohashu.count.biz.exception;

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
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 自定义业务异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({BizException.class})
  @ResponseBody
  public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
    log.warn(
        "{} request fail, errorCode: {}, errorMessage: {}",
        request.getRequestURI(),
        e.getErrorCode(),
        e.getErrorMessage());
    return Response.fail(e);
  }

  /**
   * 捕获参数异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({MethodArgumentNotValidException.class})
  @ResponseBody
  public Response<Object> handleMethodArgumentNotValidException(
      HttpServletRequest request, MethodArgumentNotValidException e) {
    // 参数错误异常
    String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

    // 获取绑定
    BindingResult bindingResult = e.getBindingResult();

    StringBuilder sb = new StringBuilder();

    // 获取校验不通过的字段，并且拼接
    Optional.ofNullable(bindingResult.getFieldErrors())
        .ifPresent(
            errors -> {
              errors.forEach(
                  error ->
                      sb.append(error.getField())
                          .append(" ")
                          .append(error.getDefaultMessage())
                          .append(", 当前值: '")
                          .append(error.getRejectedValue())
                          .append("'; "));
            });

    // 错误信息
    String errorMessage = sb.toString();

    log.warn(
        "{} request error, errorCode: {}, errorMessage: {}",
        request.getRequestURI(),
        errorCode,
        errorMessage);

    return Response.fail(errorCode, errorMessage);
  }

  /**
   * 捕获guava参数校验异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({IllegalArgumentException.class})
  @ResponseBody
  public Response<Object> handleIllegalArgumentException(
      HttpServletRequest request, IllegalArgumentException e) {
    // 参数错误异常码
    String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();
    // 错误信息
    String errorMessage = e.getMessage();

    log.warn(
        "{} request error, errorCode: {}, errorMessage: {}",
        request.getRequestURI(),
        errorCode,
        errorMessage);

    return Response.fail(errorCode, errorMessage);
  }

  /**
   * 熔断：异常测试
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({RuntimeException.class})
  @ResponseBody
  public Response<Object> handleRuntimeException(HttpServletRequest request, Exception e)
      throws Exception {
    throw e;
  }

  /**
   * 全局异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({Exception.class})
  @ResponseBody
  public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
    log.error("{} request error, ", request.getRequestURI(), e);
    return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
  }
}
