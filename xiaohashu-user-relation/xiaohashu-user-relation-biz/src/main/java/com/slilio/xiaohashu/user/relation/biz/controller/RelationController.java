package com.slilio.xiaohashu.user.relation.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.relation.biz.model.vo.*;
import com.slilio.xiaohashu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-04-19 @Description: 用户关系 @Version: 1.0
 */
@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {
  @Resource private RelationService relationService;

  @PostMapping("/follow")
  @ApiOperationLog(description = "关注用户")
  public Response<?> follow(@Validated @RequestBody FollowUserReqVO followUserReqVO) {
    return relationService.follow(followUserReqVO);
  }

  @PostMapping("/unfollow")
  @ApiOperationLog(description = "取注用户")
  public Response<?> unfollow(@Validated @RequestBody UnfollowUserReqVO unfollowUserReqVO) {
    return relationService.unfollow(unfollowUserReqVO);
  }

  @PostMapping("/following/list")
  @ApiOperationLog(description = "查询用户关注列表")
  public PageResponse<FindFollowingUserRspVO> findFollowingList(
      @Validated @RequestBody FindFollowingListReqVO findFollowingListReqVO) {
    return relationService.findFollowingList(findFollowingListReqVO);
  }

  @PostMapping("/fans/list")
  @ApiOperationLog(description = "查询用户粉丝列表")
  public PageResponse<FindFansUserRspVO> findFansList(
      @Validated @RequestBody FindFansListReqVO findFansListReqVO) {
    return relationService.findFansList(findFansListReqVO);
  }
}
