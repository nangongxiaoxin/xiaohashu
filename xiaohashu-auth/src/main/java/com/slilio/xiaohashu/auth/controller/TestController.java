package com.slilio.xiaohashu.auth.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class TestController {

    @GetMapping("/test")
    @ApiOperationLog(description = "测试接口")
    public Response<User> test(){
        return Response.success(User.builder()
                .nickName("犬小哈")
                .createTime(LocalDateTime.now())
                .build());
    }
}
