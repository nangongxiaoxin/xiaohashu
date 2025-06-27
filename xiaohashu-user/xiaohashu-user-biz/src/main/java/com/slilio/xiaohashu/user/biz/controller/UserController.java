package com.slilio.xiaohashu.user.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.model.vo.FindUserProfileReqVO;
import com.slilio.xiaohashu.user.biz.model.vo.FindUserProfileRspVO;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.biz.service.UserService;
import com.slilio.xiaohashu.user.dto.req.*;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
  @Resource private UserService userService;

  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ApiOperationLog(description = " 用户信息修改")
  public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
    return userService.updateUserInfo(updateUserInfoReqVO);
  }

  // ===================================== 对其他服务提供的接口 =====================================
  @PostMapping("/register")
  @ApiOperationLog(description = "用户注册")
  public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
    return userService.register(registerUserReqDTO);
  }

  @PostMapping("/findByPhone")
  @ApiOperationLog(description = "手机号查询用户信息")
  public Response<FindUserByPhoneRspDTO> findByPhone(
      @Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
    return userService.findByPhone(findUserByPhoneReqDTO);
  }

  @PostMapping("/password/update")
  @ApiOperationLog(description = "更新密码")
  public Response<?> updatePassword(
      @Validated @RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
    return userService.updatePassword(updateUserPasswordReqDTO);
  }

  @PostMapping("/findById")
  @ApiOperationLog(description = "Id查询用户信息")
  public Response<FindUserByIdRspDTO> findById(
      @Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO) {
    return userService.findById(findUserByIdReqDTO);
  }

  @PostMapping("/findByIds")
  @ApiOperationLog(description = "批量查询用户信息")
  public Response<List<FindUserByIdRspDTO>> findByIds(
      @Validated @RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
    return userService.findByIds(findUsersByIdsReqDTO);
  }

  @PostMapping("/profile")
  @ApiOperationLog(description = "获取用户首页信息")
  public Response<FindUserProfileRspVO> findUserProfile(
      @Validated @RequestBody FindUserProfileReqVO findUserProfileReqVO) {
    return userService.findUserProfile(findUserProfileReqVO);
  }
}
