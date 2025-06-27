package com.slilio.xiaohashu.user.biz.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.api.CountFeignApi;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-27 @Description: @Version: 1.0
 */
@Component
public class CountRpcService {
  @Resource private CountFeignApi countFeignApi;

  /**
   * 查询用户计数信息
   *
   * @param userId
   * @return
   */
  public FindUserCountsByIdRspDTO findUserCountById(Long userId) {
    FindUserCountsByIdReqDTO findUserCountsByIdReqDTO = new FindUserCountsByIdReqDTO();
    findUserCountsByIdReqDTO.setUserId(userId);

    Response<FindUserCountsByIdRspDTO> response =
        countFeignApi.findUserCount(findUserCountsByIdReqDTO);

    if (Objects.isNull(response) || !response.isSuccess()) {
      return null;
    }

    return response.getData();
  }
}
