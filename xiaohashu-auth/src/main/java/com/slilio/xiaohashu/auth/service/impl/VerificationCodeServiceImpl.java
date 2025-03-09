package com.slilio.xiaohashu.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.auth.constant.RedisKeyConstants;
import com.slilio.xiaohashu.auth.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.auth.model.vo.veriticationcode.SendVerificationCodeReqVO;
import com.slilio.xiaohashu.auth.service.VerificationCodeService;
import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {
  @Resource private RedisTemplate<String, Object> redisTemplate;

  /**
   * 发送验证码
   *
   * @param sendVerificationCodeReqVO
   * @return
   */
  @Override
  public Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO) {
    // 手机号
    String phone = sendVerificationCodeReqVO.getPhone();

    // 构建验证码
    String key = RedisKeyConstants.buildVerificationCodeKey(phone);

    // 判断验证码是否已经发送
    boolean isSent = redisTemplate.hasKey(key);
    if (isSent) {
      // 若之前发送的验证码未过期，则提示发送频繁
      throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
    }

    // 生成6位随机数字验证码
    String verificationCode = RandomUtil.randomNumbers(6);

    // todo：调用第三方短信发送服务
    log.info("===> 手机号：{}，已发送验证码为：【{}】", phone, verificationCode);

    // 存储验证码到redis，并设置过期时间为3分钟
    redisTemplate.opsForValue().set(key, verificationCode, 3, TimeUnit.MINUTES);

    return Response.success();
  }
}
