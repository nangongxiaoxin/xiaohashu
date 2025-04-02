package com.slilio.xiaohashu.auth.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.api.UserFeignApi;
import com.slilio.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.slilio.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.slilio.xiaohashu.user.dto.req.UpdateUserPasswordReqDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
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

  /**
   * 按手机号查询用户
   *
   * @param phone
   * @return
   */
  public FindUserByPhoneRspDTO findUserByPhone(String phone) {
    FindUserByPhoneReqDTO findUserByPhoneReqDTO = new FindUserByPhoneReqDTO();
    findUserByPhoneReqDTO.setPhone(phone);

    Response<FindUserByPhoneRspDTO> response = userFeignApi.findByPhone(findUserByPhoneReqDTO);

    if (!response.isSuccess()) {
      return null;
    }
    return response.getData();
  }

  /**
   * 更新密码
   *
   * @param encodePassword
   */
  public void updatePassword(String encodePassword) {
    UpdateUserPasswordReqDTO updateUserPasswordReqDTO = new UpdateUserPasswordReqDTO();
    updateUserPasswordReqDTO.setEncodePassword(encodePassword);

    userFeignApi.updatePassword(updateUserPasswordReqDTO);
  }
}
