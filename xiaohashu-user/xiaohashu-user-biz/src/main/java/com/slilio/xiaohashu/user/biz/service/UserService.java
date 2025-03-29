package com.slilio.xiaohashu.user.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;

public interface UserService {
  /**
   * 更新用户信息
   *
   * @param updateUserInfoReqVO
   * @return
   */
  Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);
}
