package com.slilio.xiaohashu.auth.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.auth.model.vo.user.UpdatePasswordReqVO;
import com.slilio.xiaohashu.auth.model.vo.user.UserLoginReqVO;

public interface UserService {
  /**
   * 登录与注册
   *
   * @param userLoginReqVO
   * @return
   */
  Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

  /**
   * 退出登录
   *
   * @param userId
   * @return
   */
  Response<?> logout();

  /**
   * 修改密码
   *
   * @param updatePasswordReqVO
   * @return
   */
  Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}
