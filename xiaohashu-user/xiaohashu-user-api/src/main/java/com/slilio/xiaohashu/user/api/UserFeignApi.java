package com.slilio.xiaohashu.user.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.constant.ApiConstants;
import com.slilio.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.slilio.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.slilio.xiaohashu.user.dto.req.UpdateUserPasswordReqDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {
  String PREFIX = "/user";

  /**
   * 用户注册
   *
   * @param registerUserReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/register")
  Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

  /**
   * 按手机号查找用户
   *
   * @param findUserByPhoneReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/findByPhone")
  Response<FindUserByPhoneRspDTO> findByPhone(
      @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

  /**
   * 更新密码
   *
   * @param updateUserPasswordReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/password/update")
  Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);
}
