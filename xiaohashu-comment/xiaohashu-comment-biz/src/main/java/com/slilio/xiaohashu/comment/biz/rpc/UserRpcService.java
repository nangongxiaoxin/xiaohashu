package com.slilio.xiaohashu.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.api.UserFeignApi;
import com.slilio.xiaohashu.user.dto.req.FindUsersByIdsReqDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-15 @Description: @Version: 1.0
 */
@Component
public class UserRpcService {
  @Resource private UserFeignApi userFeignApi;

  public List<FindUserByIdRspDTO> findByIds(List<Long> userIds) {
    if (CollUtil.isEmpty(userIds)) {
      return null;
    }

    FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
    // 去重，并设置用户ID集合
    findUsersByIdsReqDTO.setIds(userIds.stream().distinct().collect(Collectors.toList()));

    Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);

    if (!response.isSuccess()
        || Objects.isNull(response.getData())
        || CollUtil.isEmpty(response.getData())) {
      return null;
    }

    return response.getData();
  }
}
