package com.slilio.xiaohashu.user.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.dto.req.*;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import java.util.List;

public interface UserService {
  /**
   * 更新用户信息
   *
   * @param updateUserInfoReqVO
   * @return
   */
  Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

  /**
   * 用户注册
   *
   * @param registerUserReqDTO
   * @return
   */
  Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

  /**
   * 根据手机号查询用户信息
   *
   * @param findUserByPhoneReqDTO
   * @return
   */
  Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

  /**
   * 更新密码
   *
   * @param updateUserPasswordReqDTO
   * @return
   */
  Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

  /**
   * 根据用户ID查询用户信息
   *
   * @param findUserByIdReqDTO
   * @return
   */
  Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);

  /**
   * 批量根据用户ID查询用户信息
   *
   * @param findUsersByIdsReqDTO
   * @return
   */
  Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}
