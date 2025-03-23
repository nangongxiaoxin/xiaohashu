package com.slilio.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.enums.DeletedEnum;
import com.slilio.framework.common.enums.StatusEnum;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.auth.constant.RedisKeyConstants;
import com.slilio.xiaohashu.auth.constant.RoleConstants;
import com.slilio.xiaohashu.auth.domain.dataobject.RoleDO;
import com.slilio.xiaohashu.auth.domain.dataobject.UserDO;
import com.slilio.xiaohashu.auth.domain.dataobject.UserRoleDO;
import com.slilio.xiaohashu.auth.domain.mapper.RoleDOMapper;
import com.slilio.xiaohashu.auth.domain.mapper.UserDOMapper;
import com.slilio.xiaohashu.auth.domain.mapper.UserRoleDOMapper;
import com.slilio.xiaohashu.auth.enums.LoginTypeEnum;
import com.slilio.xiaohashu.auth.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.auth.model.vo.user.UpdatePasswordReqVO;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Resource private UserDOMapper userDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private UserRoleDOMapper userRoleDOMapper;
  @Resource private TransactionTemplate transactionTemplate;
  @Resource private RoleDOMapper roleDOMapper;
  @Resource private PasswordEncoder passwordEncoder;

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
        String password = userLoginReqVO.getPassword();
        // 根据手机号查询
        UserDO userDO1 = userDOMapper.selectByPhone(phone);

        // 判断该手机号是否注册
        if (Objects.isNull(userDO1)) {
          throw new BizException(ResponseCodeEnum.USER_NOT_FOUND); // 用户不存在
        }

        // 从数据库拿到密文密码
        String encodePassword = userDO1.getPassword();

        // 匹配密码是否一致
        boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

        // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
        if (!isPasswordCorrect) {
          throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR); // 密码或手机号错误
        }

        userId = userDO1.getId();

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

    // 获取当前用户请求的ID
    Long userId = LoginUserContextHolder.getUserId();

    UserDO userDO =
        UserDO.builder()
            .id(userId)
            .password(encodePassword)
            .updateTime(LocalDateTime.now())
            .build();

    // 更新密码
    userDOMapper.updateByPrimaryKeySelective(userDO);

    return Response.success();
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
            // 获取全局自增的小哈书 ID
            Long xiaohashuId =
                redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHASHU_ID_GENERATOR_KEY);

            UserDO userDO =
                UserDO.builder()
                    .phone(phone)
                    .xiaohashuId(String.valueOf(xiaohashuId)) // 自动生成小红书号 ID
                    .nickname("小红薯" + xiaohashuId) // 自动生成昵称, 如：小红薯10000
                    .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                    .build();

            // 添加入库
            userDOMapper.insert(userDO);

            // 获取刚刚添加入库的用户 ID
            Long userId = userDO.getId();

            // 给该用户分配一个默认角色
            UserRoleDO userRoleDO =
                UserRoleDO.builder()
                    .userId(userId)
                    .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeletedEnum.NO.getValue())
                    .build();
            userRoleDOMapper.insert(userRoleDO);

            RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

            // 将该用户的角色 ID 存入 Redis 中
            List<String> roles = new ArrayList<>(1);
            roles.add(roleDO.getRoleKey());

            String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
            redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

            return userId;
          } catch (Exception e) {
            status.setRollbackOnly(); // 标记事务为回滚
            log.error("==> 系统注册用户异常: ", e);
            return null;
          }
        });
  }
}
