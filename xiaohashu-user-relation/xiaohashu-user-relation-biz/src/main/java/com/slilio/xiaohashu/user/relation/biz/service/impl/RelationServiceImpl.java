package com.slilio.xiaohashu.user.relation.biz.service.impl;

import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.user.relation.biz.model.vo.FollowUserReqVO;
import com.slilio.xiaohashu.user.relation.biz.rpc.UserRpcService;
import com.slilio.xiaohashu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-04-19 @Description: 关注接口 @Version: 1.0
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {
  @Resource private UserRpcService userRpcService;

  /**
   * 关注用户
   *
   * @param followUserReqVO
   * @return
   */
  @Override
  public Response<?> follow(FollowUserReqVO followUserReqVO) {
    // 关注的用户ID
    Long followUserId = followUserReqVO.getFollowUserId();

    // 当前登录的用户ID
    Long userId = LoginUserContextHolder.getUserId();

    // 校验：无法关注自己
    if (Objects.equals(userId, followUserId)) {
      throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
    }

    // 检验关注的用户是否存在
    FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);

    if (Objects.isNull(findUserByIdRspDTO)) {
      throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
    }
    // TODO 校验关注数是否已经达到上限

    // TODO 写入Redis ZSET 关注列表

    // TODO 发送MQ

    return Response.success();
  }
}
