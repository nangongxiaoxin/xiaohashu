package com.slilio.xiaohashu.user.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.dto.req.RegisterUserReqDTO;

public interface UserService {
  /**
   * 更新用户信息
   *
   * @param updateUserInfoReqVO
   * @return
   */
  Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

  /**
   * 用户注册
   *
   * @param registerUserReqDTO
   * @return
   */
  Response<Long> register(RegisterUserReqDTO registerUserReqDTO);
}
