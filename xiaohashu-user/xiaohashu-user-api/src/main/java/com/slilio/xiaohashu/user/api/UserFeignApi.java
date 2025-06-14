package com.slilio.xiaohashu.user.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.constant.ApiConstants;
import com.slilio.xiaohashu.user.dto.req.*;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import java.util.List;
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

  /**
   * 根据用户ID查询用户信息
   *
   * @param findUserByIdReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/findById")
  Response<FindUserByIdRspDTO> findById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

  /**
   * 批量查询用户信息
   *
   * @param findUsersByIdsReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/findByIds")
  Response<List<FindUserByIdRspDTO>> findByIds(
      @RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}
