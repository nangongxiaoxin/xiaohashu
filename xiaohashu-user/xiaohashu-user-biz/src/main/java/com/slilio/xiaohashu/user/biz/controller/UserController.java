package com.slilio.xiaohashu.user.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.biz.service.UserService;
import com.slilio.xiaohashu.user.dto.req.RegisterUserReqDTO;
import jakarta.annotation.Resource;
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
  public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
    return userService.updateUserInfo(updateUserInfoReqVO);
  }

  // ===================================== 对其他服务提供的接口 =====================================
  @PostMapping("/register")
  @ApiOperationLog(description = "用户注册")
  public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
    return userService.register(registerUserReqDTO);
  }
}
