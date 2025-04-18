package com.slilio.xiaohashu.user.relation.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.relation.biz.model.vo.FollowUserReqVO;

/**
 * @Author: slilio @CreateTime: 2025-04-19 @Description: 关注接口 @Version: 1.0
 */
public interface RelationService {
  /**
   * 关注用户
   *
   * @param followUserReqVO
   * @return
   */
  Response<?> follow(FollowUserReqVO followUserReqVO);
}
