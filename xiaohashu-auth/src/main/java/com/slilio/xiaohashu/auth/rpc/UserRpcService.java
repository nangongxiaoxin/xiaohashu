package com.slilio.xiaohashu.auth.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.api.UserFeignApi;
import com.slilio.xiaohashu.user.dto.req.RegisterUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class UserRpcService {
  @Resource private UserFeignApi userFeignApi;

  /**
   * 用户注册
   *
   * @param phone
   * @return
   */
  public Long registerUser(String phone) {
    RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
    registerUserReqDTO.setPhone(phone);

    Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);
    if (!response.isSuccess()) {
      return null;
    }

    return response.getData();
  }
}
