package com.slilio.xiaohashu.auth.service.impl;

import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.auth.constant.RedisKeyConstants;
import com.slilio.xiaohashu.auth.domain.dataobject.UserDO;
import com.slilio.xiaohashu.auth.domain.mapper.UserDOMapper;
import com.slilio.xiaohashu.auth.enums.LoginTypeEnum;
import com.slilio.xiaohashu.auth.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.slilio.xiaohashu.auth.service.UserService;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Resource private UserDOMapper userDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;

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

    Long userId = null;

    // 判断登录类型
    switch (loginTypeEnum) {
      case VERIFICATION_CODE: // 验证码登录
        String verificationCode = userLoginReqVO.getCode();

        // 校验入参验证码是否为空
        if (StringUtils.isBlank(verificationCode)) {
          return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "验证码不能为空");
        }

        // 构建验证码Redis key
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        // 查询存储在Redis中该用户的登录验证码
        String sentCode = (String) redisTemplate.opsForValue().get(key);

        // 判断用户提交的验证码，与redis是否一致
        if (!StringUtils.equals(verificationCode, sentCode)) {
          throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
        }

        // 通过手机号查询记录
        UserDO userDO = userDOMapper.selectByPhone(phone);

        log.info("===> 用户是否注册，phone：{}，userDO：{}", phone, JsonUtils.toJsonString(userDO));

        // 判断是否注册
        if (Objects.isNull(userDO)) {
          // 若此用户还没有注册，系统则自动注册
          // todo
        } else {
          // 已注册，则获取用户ID
          userId = userDO.getId();
        }
        break;
      case PASSWORD: // 密码登录
        // todo
        break;
      default:
        break;
    }
    // Sa-token 登录用户，并返回存储token令牌
    // todo

    return Response.success("");
  }
}
