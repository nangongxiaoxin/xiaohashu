package com.slilio.xiaohashu.auth.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.auth.model.vo.veriticationcode.SendVerificationCodeReqVO;

public interface VerificationCodeService {

  /**
   * 发送验证码
   *
   * @param sendVerificationCodeReqVO
   * @return
   */
  Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}
