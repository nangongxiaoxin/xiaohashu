package com.slilio.xiaohashu.note.biz.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.api.UserFeignApi;
import com.slilio.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class UserRpcService {

  @Resource private UserFeignApi userFeignApi;

  /**
   * 查询用户信息
   *
   * @param userId
   * @return
   */
  public FindUserByIdRspDTO findById(Long userId) {
    FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
    findUserByIdReqDTO.setId(userId);

    Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);

    if (Objects.isNull(response) || !response.isSuccess()) {
      return null;
    }

    return response.getData();
  }
}
