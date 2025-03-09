package com.slilio.xiaohashu.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import java.time.LocalDateTime;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

  // 登录测试
  @RequestMapping("/user/doLogin")
  public String doLogin(String username, String password) {
    // 模拟登录测试
    if ("zhang".equals(username) && "123".equals(password)) {
      StpUtil.login(10001);
      return "登录成功";
    }
    return "登录失败";
  }

  // 查询登录状态
  @RequestMapping("/user/isLogin")
  public String isLogin() {
    return "当前回话是否登录：" + StpUtil.isLogin();
  }
}
