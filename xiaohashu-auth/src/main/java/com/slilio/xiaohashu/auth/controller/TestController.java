package com.slilio.xiaohashu.auth.controller;

import com.slilio.framework.common.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public Response<String> test(){
        return Response.success("Hello, 犬小哈专栏");
    }
}
