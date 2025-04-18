package com.slilio.xiaohashu.user.relation.biz.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.api.UserFeignApi;
import com.slilio.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-04-19 @Description: 远程调用类 @Version: 1.0
 */
@Component
public class UserRpcService {
  @Resource private UserFeignApi userFeignApi;

  public FindUserByIdRspDTO findById(Long userId) {
    FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
    findUserByIdReqDTO.setId(userId);

    Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);

    if (!response.isSuccess() || Objects.isNull(response.getData())) {
      return null;
    }

    return response.getData();
  }
}
