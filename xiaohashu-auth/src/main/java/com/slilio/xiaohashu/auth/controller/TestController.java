package com.slilio.xiaohashu.auth.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import java.time.LocalDateTime;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/test")
  @ApiOperationLog(description = "测试接口")
  public Response<User> test() {
    return Response.success(User.builder().nickName("犬小哈").createTime(LocalDateTime.now()).build());
  }

  @PostMapping("/test2")
  @ApiOperationLog(description = "测试接口2")
  public Response<User> test2(@RequestBody @Validated User user) {
    return Response.success(user);
  }
}
