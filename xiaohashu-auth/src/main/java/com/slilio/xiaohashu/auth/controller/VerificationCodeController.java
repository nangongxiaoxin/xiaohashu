package com.slilio.xiaohashu.auth.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.auth.model.vo.veriticationcode.SendVerificationCodeReqVO;
import com.slilio.xiaohashu.auth.service.VerificationCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class VerificationCodeController {
  @Resource private VerificationCodeService verificationCodeService;

  /**
   * 发送验证码
   *
   * @param sendVerificationCodeReqVO
   * @return
   */
  @PostMapping("/verification/code/send")
  @ApiOperationLog(description = "发送短信验证码")
  public Response<?> send(
      @Validated @RequestBody SendVerificationCodeReqVO sendVerificationCodeReqVO) {
    return verificationCodeService.send(sendVerificationCodeReqVO);
  }
}
