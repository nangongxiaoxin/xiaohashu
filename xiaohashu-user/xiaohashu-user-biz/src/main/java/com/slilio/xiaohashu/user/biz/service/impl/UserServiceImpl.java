package com.slilio.xiaohashu.user.biz.service.impl;

import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.ParamUtils;
import com.slilio.xiaohashu.user.biz.domain.dataobject.UserDO;
import com.slilio.xiaohashu.user.biz.domain.mapper.UserDOMapper;
import com.slilio.xiaohashu.user.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.user.biz.enums.SexEnum;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.biz.rpc.OssRpcService;
import com.slilio.xiaohashu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
  @Resource private UserDOMapper userDOMapper;
  @Resource private OssRpcService ossRpcService;

  /**
   * 更新用户信息
   *
   * @param updateUserInfoReqVO
   * @return
   */
  @Override
  public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
    UserDO userDO = new UserDO();
    // 设置当前需要更新的用户ID
    userDO.setId(LoginUserContextHolder.getUserId());
    // 标识位：是否需要更新
    boolean needUpdate = false;

    // 头像
    MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();

    if (Objects.nonNull(avatarFile)) {
      String avatar = ossRpcService.uploadFile(avatarFile);
      log.info("===> 调用oss服务成功，上传头像，url：{}", avatar);

      // 若头像上传失败，抛出业务异常
      if (StringUtils.isBlank(avatar)) {
        throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
      }

      userDO.setAvatar(avatar);
      needUpdate = true;
    }

    // 昵称
    String nickname = updateUserInfoReqVO.getNickname();
    if (StringUtils.isNotBlank(nickname)) {
      Preconditions.checkArgument(
          ParamUtils.checkNickName(nickname),
          ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
      userDO.setNickname(nickname);
      needUpdate = true;
    }

    // 小哈书号
    String xiaohashuId = updateUserInfoReqVO.getXiaohashuId();
    if (StringUtils.isNotBlank(xiaohashuId)) {
      Preconditions.checkArgument(
          ParamUtils.checkXiaohashuId(xiaohashuId),
          ResponseCodeEnum.XIAOHASHU_ID_VALID_FAIL.getErrorMessage());
      userDO.setXiaohashuId(xiaohashuId);
      needUpdate = true;
    }

    // 性别
    Integer sex = updateUserInfoReqVO.getSex();
    if (Objects.nonNull(sex)) {
      Preconditions.checkArgument(
          SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
      userDO.setSex(sex);
      needUpdate = true;
    }

    // 生日
    LocalDate birthday = updateUserInfoReqVO.getBirthday();
    if (Objects.nonNull(birthday)) {
      userDO.setBirthday(birthday);
      needUpdate = true;
    }

    // 个人简介
    String introduction = updateUserInfoReqVO.getIntroduction();
    if (StringUtils.isNotBlank(introduction)) {
      Preconditions.checkArgument(
          ParamUtils.checkLength(introduction, 100),
          ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
    }

    // 背景图
    MultipartFile backgroundImg = updateUserInfoReqVO.getBackgroundImg();
    if (Objects.nonNull(backgroundImg)) {
      // todo 调用对象存储上传文件
      String background = ossRpcService.uploadFile(backgroundImg);
      log.info("===> 调用oss服务成功，上传背景图，url：{}", background);

      // 若头像上传失败，抛出业务异常
      if (StringUtils.isBlank(background)) {
        throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
      }

      userDO.setBackgroundImg(background);
      needUpdate = true;
    }

    if (needUpdate) {
      // 更新用户信息
      userDO.setUpdateTime(LocalDateTime.now());
      userDOMapper.updateByPrimaryKeySelective(userDO);
    }

    return Response.success();
  }
}
