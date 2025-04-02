package com.slilio.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.auth.constant.RedisKeyConstants;
import com.slilio.xiaohashu.auth.enums.LoginTypeEnum;
import com.slilio.xiaohashu.auth.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.auth.model.vo.user.UpdatePasswordReqVO;
import com.slilio.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.slilio.xiaohashu.auth.rpc.UserRpcService;
import com.slilio.xiaohashu.auth.service.AuthService;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private PasswordEncoder passwordEncoder;
  @Resource private UserRpcService userRpcService;

  /**
   * 登录与注册
   *
   * @param userLoginReqVO
   * @return
   */
  @Override
  public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
    String phone = userLoginReqVO.getPhone();
    Integer type = userLoginReqVO.getType();
    LoginTypeEnum loginTypeEnum = LoginTypeEnum.valeOf(type);

    // 登录类型错误
    if (Objects.isNull(loginTypeEnum)) {
      throw new BizException(ResponseCodeEnum.LOGIN_TYPE_ERROR);
    }

    Long userId = null;

    // 判断登录类型
    switch (loginTypeEnum) {
      case VERIFICATION_CODE: // 验证码登录
        String verificationCode = userLoginReqVO.getCode();

        // 校验入参验证码是否为空
        Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

        // 构建验证码Redis key
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        // 查询存储在Redis中该用户的登录验证码
        String sentCode = (String) redisTemplate.opsForValue().get(key);

        // 判断用户提交的验证码，与redis是否一致
        if (!StringUtils.equals(verificationCode, sentCode)) {
          throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
        }

        // RPC： 调用用户服务，注册用户
        Long userIdTmp = userRpcService.registerUser(phone);
        // 若调用用户服务返回的用户ID为空，则提示登录失败
        if (Objects.isNull(userIdTmp)) {
          throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
        }
        userId = userIdTmp;
        break;
      case PASSWORD: // 密码登录
        String password = userLoginReqVO.getPassword();

        // RPC：调用用户服务，通过手机号查询用户
        FindUserByPhoneRspDTO findUserByPhoneRspDTO = userRpcService.findUserByPhone(phone);

        // 判断该手机号是否注册
        if (Objects.isNull(findUserByPhoneRspDTO)) {
          throw new BizException(ResponseCodeEnum.USER_NOT_FOUND); // 用户不存在
        }

        // 从数据库拿到密文密码
        String encodePassword = findUserByPhoneRspDTO.getPassword();

        // 匹配密码是否一致
        boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

        // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
        if (!isPasswordCorrect) {
          throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR); // 密码或手机号错误
        }

        userId = findUserByPhoneRspDTO.getId();

        break;
      default:
        break;
    }
    // Sa-token 登录用户，并返回存储token令牌
    StpUtil.login(userId);
    // 获取token令牌
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    // 返回token令牌
    return Response.success(tokenInfo.tokenValue);
  }

  /**
   * 退出登录
   *
   * @return
   */
  @Override
  public Response<?> logout() {
    Long userId = LoginUserContextHolder.getUserId();

    log.info("==> 用户退出登录, userId: {}", userId);

    threadPoolTaskExecutor.submit(
        () -> {
          Long userId2 = LoginUserContextHolder.getUserId();
          log.info("==> 异步线程中获取 userId: {}", userId2);
        });

    // 退出登录 (指定用户 ID)
    StpUtil.logout(userId);

    return Response.success();
  }

  /**
   * 修改密码
   *
   * @param updatePasswordReqVO
   * @return
   */
  @Override
  public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
    // 新密码
    String newPassword = updatePasswordReqVO.getNewPassword();
    // 密码加密
    String encodePassword = passwordEncoder.encode(newPassword);

    // RPC：调用用户服务：更新密码
    userRpcService.updatePassword(encodePassword);

    return Response.success();
  }
}
