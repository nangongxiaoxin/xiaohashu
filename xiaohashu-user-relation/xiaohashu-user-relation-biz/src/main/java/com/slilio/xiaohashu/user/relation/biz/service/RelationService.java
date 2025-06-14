package com.slilio.xiaohashu.user.relation.biz.service;

import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.relation.biz.model.vo.*;

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

  /**
   * 取关用户
   *
   * @param unfollowUserReqVO
   * @return
   */
  Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);

  /**
   * 查询关注列表
   *
   * @param findFollowingListReqVO
   * @return
   */
  PageResponse<FindFollowingUserRspVO> findFollowingList(
      FindFollowingListReqVO findFollowingListReqVO);

  /**
   * 查询粉丝列表
   *
   * @param findFansListReqVO
   * @return
   */
  PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO);
}
