package com.slilio.xiaohashu.oss.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
@Slf4j
public class TestFeignController {

  @PostMapping("/test")
  @ApiOperationLog(description = "Feign 测试接口")
  public Response<?> test() {
    return Response.success();
  }
}
