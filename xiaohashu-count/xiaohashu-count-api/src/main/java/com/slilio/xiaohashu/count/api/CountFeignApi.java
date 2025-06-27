package com.slilio.xiaohashu.count.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.constants.ApiConstants;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: slilio @CreateTime: 2025-06-27 @Description: @Version: 1.0
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface CountFeignApi {
  String PREFIX = "/count";

  @PostMapping(value = PREFIX + "/user/data")
  Response<FindUserCountsByIdRspDTO> findUserCount(
      @RequestBody FindUserCountsByIdReqDTO findUserCountsByIdReqDTO);
}
