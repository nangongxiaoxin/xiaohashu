package com.slilio.xiaohashu.count.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;

/**
 * @Author: slilio @CreateTime: 2025-06-26 @Description: @Version: 1.0
 */
public interface UserCountService {

  /**
   * 查询用户计数
   *
   * @param findUserCountsByIdReqDTO
   * @return
   */
  Response<FindUserCountsByIdRspDTO> findUserCountData(
      FindUserCountsByIdReqDTO findUserCountsByIdReqDTO);
}
