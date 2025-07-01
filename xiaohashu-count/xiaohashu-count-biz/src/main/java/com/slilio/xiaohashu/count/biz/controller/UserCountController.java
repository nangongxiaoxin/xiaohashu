package com.slilio.xiaohashu.count.biz.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
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
  @SentinelResource(
      value = "findUserCountData4Controller",
      blockHandler = "blockHandler4findUserCountData")
  public Response<FindUserCountsByIdRspDTO> findUserCountData(
      @Validated @RequestBody FindUserCountsByIdReqDTO findUserCountsByIdReqDTO) {
    // 模拟超时异常
    //    try {
    //      // 模拟接口响应慢
    //      TimeUnit.MILLISECONDS.sleep(1100);
    //    } catch (InterruptedException e) {
    //    }

    // 模拟接口随机发生异常，抛出概率约为 50%
    //    if (Math.random() > 0.5) {
    //      throw new RuntimeException();
    //    }
    return userCountService.findUserCountData(findUserCountsByIdReqDTO);
  }

  /**
   * blockHandler函数，原方法调用被限流、降级、系统保护的时候调用 注意：方法中需要包含限流方法的所有参数，和BlockException参数
   *
   * @param findUserCountsByIdReqDTO
   * @param blockException
   * @return
   */
  public Response<FindUserCountsByIdRspDTO> blockHandler4findUserCountData(
      FindUserCountsByIdReqDTO findUserCountsByIdReqDTO, BlockException blockException) {
    log.warn("## /count/user/count 接口被限流: {}", JsonUtils.toJsonString(findUserCountsByIdReqDTO));

    // 抛出异常
    //   1.方法1 throw new BizException(ResponseCodeEnum.FLOW_LIMIT);
    // 2.方法2
    return Response.success(
        FindUserCountsByIdRspDTO.builder()
            .userId(findUserCountsByIdReqDTO.getUserId())
            .collectTotal(0L)
            .fansTotal(0L)
            .followingTotal(0L)
            .likeTotal(0L)
            .noteTotal(0L)
            .build());
  }
}
