package com.slilio.xiaohashu.user.biz.exception;

import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 捕获自定义业务异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({BizException.class})
  @ResponseBody
  public Response<Object> handleException(HttpServletRequest request, BizException e) {
    log.warn(
        "{} request fail, errorCode: {}, errorMessage: {}",
        request.getRequestURI(),
        e.getErrorCode(),
        e.getErrorMessage());
    return Response.fail(e);
  }

  /**
   * 捕获参数校验异常
   *
   * @param request
   * @param e
   * @return
   */
  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  @ResponseBody
  public Response<Object> handleControllerException(HttpServletRequest request, Throwable e) {
    // 参数错误异常码
    String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

    // 获取 BindingResult
    BindingResult bindingResult = null;

    // 检查异常类型，并强制类型转换，获取绑定结果
    if (e instanceof MethodArgumentNotValidException) {
      bindingResult = (((MethodArgumentNotValidException) e)).getBindingResult();
    } else if (e instanceof BindException) {
      bindingResult = ((BindException) e).getBindingResult();
    }

    StringBuilder sb = new StringBuilder();

    // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确，当前值为："123qq.com";
    Optional.ofNullable(bindingResult.getFieldErrors())
        .ifPresent(
            errors -> {
              errors.forEach(
                  error -> {
                    sb.append(error.getField())
                        .append(" ")
                        .append(error.getDefaultMessage())
                        .append(", 当前值： '")
                        .append(error.getRejectedValue())
                        .append("';");
                  });
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
        "{} request error,errorCode: {}, errorMessage: {}",
        request.getRequestURI(),
        errorCode,
        errorMessage);
    return Response.fail(errorCode, errorMessage);
  }

  /**
   * 其他类型异常
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
