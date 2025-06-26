package com.slilio.xiaohashu.count.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.biz.service.UserCountService;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-06-26 @Description: @Version: 1.0
 */
@RestController
@RequestMapping("/count")
@Slf4j
public class UserCountController {

  @Resource private UserCountService userCountService;

  @PostMapping(value = "/user/data")
  @ApiOperationLog(description = "获取用户计数数据")
  public Response<FindUserCountsByIdRspDTO> findUserCountData(
      @Validated @RequestBody FindUserCountsByIdReqDTO findUserCountsByIdReqDTO) {
    return userCountService.findUserCountData(findUserCountsByIdReqDTO);
  }
}
