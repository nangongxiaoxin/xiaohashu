package com.slilio.xiaohashu.search.biz.service;

import com.slilio.framework.common.response.PageResponse;
import com.slilio.xiaohashu.search.biz.model.vo.SearchUserReqVO;
import com.slilio.xiaohashu.search.biz.model.vo.SearchUserRspVO;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
public interface UserService {

  /**
   * 搜索用户
   *
   * @param searchUserReqVO
   * @return
   */
  PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO);
}
