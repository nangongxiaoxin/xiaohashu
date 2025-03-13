package com.slilio.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.slilio.framework.common.enums.DeleteEnum;
import com.slilio.framework.common.enums.StatusEnum;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.auth.constant.RedisKeyConstants;
import com.slilio.xiaohashu.auth.constant.RoleConstants;
import com.slilio.xiaohashu.auth.domain.dataobject.UserDO;
import com.slilio.xiaohashu.auth.domain.dataobject.UserRoleDO;
import com.slilio.xiaohashu.auth.domain.mapper.UserDOMapper;
import com.slilio.xiaohashu.auth.domain.mapper.UserRoleDOMapper;
import com.slilio.xiaohashu.auth.enums.LoginTypeEnum;
import com.slilio.xiaohashu.auth.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.slilio.xiaohashu.auth.service.UserService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Resource private UserDOMapper userDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private UserRoleDOMapper userRoleDOMapper;
  @Resource private TransactionTemplate transactionTemplate;

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
        Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

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
          userId = registerUser(phone);
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
    StpUtil.login(userId);
    // 获取token令牌
    SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
    // 返回token令牌
    return Response.success(tokenInfo.tokenValue);
  }

  /**
   * 系统自动注册用户（编程异常事务处理）
   *
   * @param phone
   * @return
   */
  private Long registerUser(String phone) {
    return transactionTemplate.execute(
        status -> {
          try {
            // 获取全局自增的小哈书ID
            Long xiaohashuId =
                redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHASHU_ID_GENERATOR_KEY);

            UserDO userDO =
                UserDO.builder()
                    .phone(phone)
                    .xiaohashuId(String.valueOf(xiaohashuId)) // 自动生成小哈书ID号
                    .nickname("小红薯" + xiaohashuId) // 自动生成昵称
                    .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeleteEnum.NO.getValue()) // 逻辑删除
                    .build();

            // 添加入库
            userDOMapper.insert(userDO);

            // 获取刚刚添加的用户角色
            Long userId = userDO.getId();

            // 给该用户分配一个默认角色
            UserRoleDO userRoleDO =
                UserRoleDO.builder()
                    .userId(userId)
                    .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeleteEnum.NO.getValue())
                    .build();

            // 添加入库
            userRoleDOMapper.insert(userRoleDO);

            // 将该角色存入redis
            List<Long> roles = new ArrayList<>();
            roles.add(RoleConstants.COMMON_USER_ROLE_ID);
            String userRolesKey = RedisKeyConstants.buildUserRolesKey(phone);
            redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

            return userId;
          } catch (Exception e) {
            status.setRollbackOnly(); // 标记事务为回滚
            log.error("==》 系统注册用户异常：", e);
            return null;
          }
        });
  }
}
