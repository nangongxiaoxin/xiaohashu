package com.slilio.xiaohashu.search.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.xiaohashu.search.model.vo.SearchUserReqVO;
import com.slilio.xiaohashu.search.model.vo.SearchUserRspVO;
import com.slilio.xiaohashu.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-05-28 @Description: @Version: 1.0
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {
  @Resource private UserService userService;

  @PostMapping("/user")
  @ApiOperationLog(description = "搜索用户")
  public PageResponse<SearchUserRspVO> searchUser(
      @RequestBody @Validated SearchUserReqVO searchUserReqVO) {
    return userService.searchUser(searchUserReqVO);
  }
}
