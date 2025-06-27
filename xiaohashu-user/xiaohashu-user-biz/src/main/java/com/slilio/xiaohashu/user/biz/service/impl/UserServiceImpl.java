package com.slilio.xiaohashu.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.enums.DeletedEnum;
import com.slilio.framework.common.enums.StatusEnum;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.framework.common.util.NumberUtils;
import com.slilio.framework.common.util.ParamUtils;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import com.slilio.xiaohashu.user.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.user.biz.constant.RoleConstants;
import com.slilio.xiaohashu.user.biz.domain.dataobject.RoleDO;
import com.slilio.xiaohashu.user.biz.domain.dataobject.UserDO;
import com.slilio.xiaohashu.user.biz.domain.dataobject.UserRoleDO;
import com.slilio.xiaohashu.user.biz.domain.mapper.RoleDOMapper;
import com.slilio.xiaohashu.user.biz.domain.mapper.UserDOMapper;
import com.slilio.xiaohashu.user.biz.domain.mapper.UserRoleDOMapper;
import com.slilio.xiaohashu.user.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.user.biz.enums.SexEnum;
import com.slilio.xiaohashu.user.biz.model.vo.FindUserProfileReqVO;
import com.slilio.xiaohashu.user.biz.model.vo.FindUserProfileRspVO;
import com.slilio.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.slilio.xiaohashu.user.biz.rpc.CountRpcService;
import com.slilio.xiaohashu.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.slilio.xiaohashu.user.biz.rpc.OssRpcService;
import com.slilio.xiaohashu.user.biz.service.UserService;
import com.slilio.xiaohashu.user.dto.req.*;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
  @Resource private UserDOMapper userDOMapper;
  @Resource private OssRpcService ossRpcService;
  @Resource private UserRoleDOMapper userRoleDOMapper;
  @Resource private RoleDOMapper roleDOMapper;
  @Resource private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private CountRpcService countRpcService;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /** 用户信息本地缓存 */
  private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(10000) // 设置初始容量为10000个条目
          .maximumSize(10000) // 设置缓存的最大容量为10000个条目
          .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在1小时后过期
          .build();

  /** 用户主页消息本地缓存 */
  private static final Cache<Long, FindUserProfileRspVO> PROFILE_LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(10000) // 初始容量
          .maximumSize(10000) // 最大容量
          .expireAfterWrite(5, TimeUnit.MINUTES) // 5分钟后过期
          .build();

  /**
   * 更新用户信息
   *
   * @param updateUserInfoReqVO
   * @return
   */
  @Override
  public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
    // 被更新的用户ID
    Long userId = updateUserInfoReqVO.getUserId();
    // 当前登录的ID
    Long loginUserId = LoginUserContextHolder.getUserId();

    // 校验是否本人修改
    if (!Objects.equals(loginUserId, userId)) {
      throw new BizException(ResponseCodeEnum.CANT_UPDATE_OTHER_USER_PROFILE);
    }

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
      userDO.setIntroduction(introduction);
      needUpdate = true;
    }

    // 背景图
    MultipartFile backgroundImg = updateUserInfoReqVO.getBackgroundImg();
    if (Objects.nonNull(backgroundImg)) {
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
      // 删除用户缓存
      deleteUserRedisCache(userId);

      // 更新用户信息
      userDO.setUpdateTime(LocalDateTime.now());
      userDOMapper.updateByPrimaryKeySelective(userDO);
    }

    // todo：延迟双删 解决分布式情况下，多服务直接的数据不一致，即此应用信息变更了，但是别的还没有变更

    return Response.success();
  }

  /**
   * 删除Redis中的用户缓存
   *
   * @param userId
   */
  private void deleteUserRedisCache(Long userId) {
    // 构建redis key
    String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);
    String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

    // 批量删除
    redisTemplate.delete(Arrays.asList(userProfileRedisKey, userInfoRedisKey));
  }

  /**
   * 用户注册
   *
   * @param registerUserReqDTO
   * @return
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
    String phone = registerUserReqDTO.getPhone();

    // 先判断该手机号是否注册
    UserDO userDO1 = userDOMapper.selectByPhone(phone);
    log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(userDO1));

    // 若已注册，则直接返回用户 ID
    if (Objects.nonNull(userDO1)) {
      return Response.success(userDO1.getId());
    }

    // 否则注册新用户
    // 获取全局自增的小哈书 ID
    //    Long xiaohashuId =
    // redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHASHU_ID_GENERATOR_KEY);

    // RPC：调用分布式ID生成服务生成小哈书ID
    String xiaohashuId = distributedIdGeneratorRpcService.getXiaohashuId();

    // RPC：调用分布式ID生成服务生成用户ID
    String userIdStr = distributedIdGeneratorRpcService.getUserId();
    Long userId = Long.valueOf(userIdStr);

    UserDO userDO =
        UserDO.builder()
            .id(userId)
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
    //    Long userId = userDO.getId();

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

    return Response.success(userId);
  }

  /**
   * 根据手机号查询信息
   *
   * @param findUserByPhoneReqDTO
   * @return
   */
  @Override
  public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
    String phone = findUserByPhoneReqDTO.getPhone();

    // 根据手机号查询用户信息
    UserDO userDO = userDOMapper.selectByPhone(phone);

    // 判空
    if (Objects.isNull(userDO)) {
      throw new BizException(ResponseCodeEnum.USER_NOT_FOUNT);
    }

    // 构建返参
    FindUserByPhoneRspDTO findUserByPhoneRspDTO =
        FindUserByPhoneRspDTO.builder().id(userDO.getId()).password(userDO.getPassword()).build();

    return Response.success(findUserByPhoneRspDTO);
  }

  /**
   * 更新密码
   *
   * @param updateUserPasswordReqDTO
   * @return
   */
  @Override
  public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
    // 获取当前的用户ID
    Long userId = LoginUserContextHolder.getUserId();

    if (Objects.isNull(userId)) {
      throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
    }

    UserDO userDO =
        UserDO.builder()
            .id(userId)
            .password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
            .updateTime(LocalDateTime.now())
            .build();
    // 更新密码
    userDOMapper.updateByPrimaryKeySelective(userDO);
    return Response.success();
  }

  /**
   * 根据用户ID查询用户
   *
   * @param findUserByIdReqDTO
   * @return
   */
  @Override
  public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
    Long userId = findUserByIdReqDTO.getId();

    // 先从本地缓存中查询
    FindUserByIdRspDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(userId);
    if (Objects.nonNull(findUserByIdRspDTOLocalCache)) {
      log.info("===> 命中了本地缓存；{}", findUserByIdRspDTOLocalCache);
      return Response.success(findUserByIdRspDTOLocalCache);
    }

    // 用户缓存Redis key 名称
    String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
    // 先从redis缓存中查询
    String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);

    // 若Redis中存在该用户信息
    if (StringUtils.isNotBlank(userInfoRedisValue)) {
      // 将存储的json字符串转换为对象，并返回
      FindUserByIdRspDTO findUserByIdRspDTO =
          JsonUtils.parseObject(userInfoRedisValue, FindUserByIdRspDTO.class);
      // 异步线程中将以用户信息存入本地缓存
      threadPoolTaskExecutor.submit(
          () -> {
            if (Objects.nonNull(findUserByIdRspDTO)) {
              // 写入本地缓存
              LOCAL_CACHE.put(userId, findUserByIdRspDTO);
            }
          });
      return Response.success(findUserByIdRspDTO);
    }

    // 否则，从数据库中查询
    // 根据用户ID查询用户信息
    UserDO userDO = userDOMapper.selectByPrimaryKey(userId);

    // 判空 设置防止缓存击穿机制
    if (Objects.isNull(userDO)) {
      threadPoolTaskExecutor.execute(
          () -> {
            // 防止缓存穿透，将空数据存入Redis缓存（过期时间不宜过长）
            // 保底1分钟+随机秒数
            long expireSeconds = 60 + RandomUtil.randomInt(60);
            redisTemplate
                .opsForValue()
                .set(userInfoRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
          });
      throw new BizException(ResponseCodeEnum.USER_NOT_FOUNT);
    }

    // 构建返参
    FindUserByIdRspDTO findUserByIdRspDTO =
        FindUserByIdRspDTO.builder()
            .id(userDO.getId())
            .nickName(userDO.getNickname())
            .avatar(userDO.getAvatar())
            .introduction(userDO.getIntroduction())
            .build();

    // 异步将用户信息存入redis缓存，提升响应速度
    threadPoolTaskExecutor.submit(
        () -> {
          // 过期时间（基础为1天+随机秒数，防止同一时间大量缓存失效）
          // 防止缓存雪崩
          long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
          redisTemplate
              .opsForValue()
              .set(
                  userInfoRedisKey,
                  JsonUtils.toJsonString(findUserByIdRspDTO),
                  expireSeconds,
                  TimeUnit.SECONDS);
        });

    return Response.success(findUserByIdRspDTO);
  }

  /**
   * 批量根据用户ID查询用户信息
   *
   * @param findUsersByIdsReqDTO
   * @return
   */
  @Override
  public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
    // 需要查询的用户ID集合
    List<Long> userIds = findUsersByIdsReqDTO.getIds();

    // 构建Redis Key集合
    List<String> redisKeys = userIds.stream().map(RedisKeyConstants::buildUserInfoKey).toList();

    // 先从Redis缓存中查询，multiGet批量查询提升性能
    List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
    // 如果缓存中不为空
    if (CollUtil.isNotEmpty(redisValues)) {
      // 过滤掉为空的数据
      redisValues = redisValues.stream().filter(Objects::nonNull).toList();
    }

    // 返参
    List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();

    // 将过滤后的缓存集合，转换为DTO返参实体类
    if (CollUtil.isNotEmpty(redisValues)) {
      findUserByIdRspDTOS =
          redisValues.stream()
              .map(value -> JsonUtils.parseObject(String.valueOf(value), FindUserByIdRspDTO.class))
              .collect(Collectors.toCollection(ArrayList::new));
    }

    // 如果被插叙的用户信息，都在redis中，则直接返回
    if (CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)) {
      return Response.success(findUserByIdRspDTOS);
    }

    // 其他情况：1.redis缓存中没有数据；2.redis缓存数据不齐全，则需从数据库补充
    List<Long> userIdsNeedQuery = null;

    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      // 将findUserInfoByIdRspDTOS集合转Map
      Map<Long, FindUserByIdRspDTO> map =
          findUserByIdRspDTOS.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

      // 筛选出需要查数据库的用户ID
      userIdsNeedQuery = userIds.stream().filter(id -> Objects.isNull(map.get(id))).toList();
    } else {
      // 缓存中一条用户信息都没有的，则提交的用户ID集合都需要查询数据库
      userIdsNeedQuery = userIds;
    }

    // 从数据库中批量查询
    List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);

    List<FindUserByIdRspDTO> findUserByIdRspDTOS2 = null;

    // 若数据查询的记录不为空
    if (CollUtil.isNotEmpty(userDOS)) {
      // DO转DTO
      findUserByIdRspDTOS2 =
          userDOS.stream()
              .map(
                  userDO ->
                      FindUserByIdRspDTO.builder()
                          .id(userDO.getId())
                          .nickName(userDO.getNickname())
                          .avatar(userDO.getAvatar())
                          .introduction(userDO.getIntroduction())
                          .build())
              .toList();

      // 异步线程将用户信息同步到redis中
      List<FindUserByIdRspDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;
      threadPoolTaskExecutor.submit(
          () -> {
            // DTO集合转Map
            Map<Long, FindUserByIdRspDTO> map =
                finalFindUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

            // 执行pipeline操作
            redisTemplate.executePipelined(
                new SessionCallback<>() {
                  @Override
                  public Object execute(RedisOperations operations) {
                    for (UserDO userDO : userDOS) {
                      Long userId = userDO.getId();

                      // 用户信息缓存Redis Key
                      String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
                      // DTO转JSON字符串
                      FindUserByIdRspDTO findUserInfoByIdRspDTO = map.get(userId);
                      String value = JsonUtils.toJsonString(findUserInfoByIdRspDTO);

                      // 过期时间
                      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                      operations
                          .opsForValue()
                          .set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                    }
                    return null;
                  }
                });
          });
    }

    // 合并数据
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS2)) {
      findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
    }

    return Response.success(findUserByIdRspDTOS);
  }

  /**
   * 获取用户主页信息
   *
   * @param findUserProfileReqVO
   * @return
   */
  @Override
  public Response<FindUserProfileRspVO> findUserProfile(FindUserProfileReqVO findUserProfileReqVO) {
    Long userId = findUserProfileReqVO.getUserId();

    // 如果入参id为空，则为当前登录的用户ID
    if (Objects.isNull(userId)) {
      userId = LoginUserContextHolder.getUserId();
    }
    // 1.1 优先查询本地缓存
    if (!Objects.equals(userId, LoginUserContextHolder.getUserId())) { // 本人查看不走缓存
      FindUserProfileRspVO userProfileLocalCache = PROFILE_LOCAL_CACHE.getIfPresent(userId);

      if (Objects.nonNull(userProfileLocalCache)) {
        log.info("## 用户主页信息命中本地缓存: {}", JsonUtils.toJsonString(userProfileLocalCache));
        return Response.success(userProfileLocalCache);
      }
    }

    // 1.2 查询redis缓存
    String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);
    String userProfileJson = (String) redisTemplate.opsForValue().get(userProfileRedisKey);
    if (StringUtils.isNotBlank(userProfileJson)) {

      FindUserProfileRspVO findUserProfileRspVO =
          JsonUtils.parseObject(userProfileJson, FindUserProfileRspVO.class);

      // 异步同步到本地缓存
      syncUserProfile2LocalCache(userId, findUserProfileRspVO);
      // 如果是博主本人，保证计数的实时性
      authorGetActualCountData(userId, findUserProfileRspVO);

      return Response.success(findUserProfileRspVO);
    }

    //  2. 再查询数据库
    UserDO userDO = userDOMapper.selectByPrimaryKey(userId);

    if (Objects.isNull(userDO)) {
      throw new BizException(ResponseCodeEnum.USER_NOT_FOUNT);
    }

    // 返参
    FindUserProfileRspVO findUserProfileRspVO =
        FindUserProfileRspVO.builder()
            .userId(userId)
            .avatar(userDO.getAvatar())
            .nickname(userDO.getNickname())
            .xiaohashuId(userDO.getXiaohashuId())
            .sex(userDO.getSex())
            .introduction(userDO.getIntroduction())
            .build();

    // 年龄
    LocalDate birthDate = userDO.getBirthday();
    findUserProfileRspVO.setAge(Objects.isNull(birthDate) ? 0 : DateUtils.calculateAge(birthDate));

    // 3. Feign 调用计数服务

    // RPC调用计数服务
    rpcCountServiceAndSetData(userId, findUserProfileRspVO);

    // 异步同步到redis
    syncUserProfile2Redis(userProfileRedisKey, findUserProfileRspVO);

    return Response.success(findUserProfileRspVO);
  }

  /**
   * 如果是博主本人查看，保证计数的实时性
   *
   * @param userId
   * @param findUserProfileRspVO
   */
  private void authorGetActualCountData(Long userId, FindUserProfileRspVO findUserProfileRspVO) {
    if (Objects.equals(userId, LoginUserContextHolder.getUserId())) {
      // 如果是博主本人查看
      rpcCountServiceAndSetData(userId, findUserProfileRspVO);
    }
  }

  /**
   * Feign 调用计数服务, 并设置计数数据
   *
   * @param userId
   * @param findUserProfileRspVO
   */
  private void rpcCountServiceAndSetData(Long userId, FindUserProfileRspVO findUserProfileRspVO) {
    FindUserCountsByIdRspDTO findUserCountsByIdRspDTO = countRpcService.findUserCountById(userId);
    if (Objects.nonNull(findUserCountsByIdRspDTO)) {
      Long fansTotal = findUserCountsByIdRspDTO.getFansTotal();
      Long followingTotal = findUserCountsByIdRspDTO.getFollowingTotal();
      Long likeTotal = findUserCountsByIdRspDTO.getLikeTotal();
      Long collectTotal = findUserCountsByIdRspDTO.getCollectTotal();
      Long noteTotal = findUserCountsByIdRspDTO.getNoteTotal();

      findUserProfileRspVO.setFansTotal(NumberUtils.formatNumberString(fansTotal));
      findUserProfileRspVO.setFollowingTotal(NumberUtils.formatNumberString(followingTotal));
      findUserProfileRspVO.setNoteTotal(NumberUtils.formatNumberString(noteTotal));
      findUserProfileRspVO.setLikeTotal(NumberUtils.formatNumberString(likeTotal));
      findUserProfileRspVO.setCollectTotal(NumberUtils.formatNumberString(collectTotal));
      findUserProfileRspVO.setLikeAndCollectTotal(
          NumberUtils.formatNumberString(likeTotal + collectTotal));
    }
  }

  /**
   * 异步同步到本地缓存
   *
   * @param userId
   * @param findUserProfileRspVO
   */
  private void syncUserProfile2LocalCache(Long userId, FindUserProfileRspVO findUserProfileRspVO) {
    threadPoolTaskExecutor.submit(
        () -> {
          PROFILE_LOCAL_CACHE.put(userId, findUserProfileRspVO);
        });
  }

  /**
   * 异步同步到redis
   *
   * @param userProfileRedisKey
   * @param findUserProfileRspVO
   */
  private void syncUserProfile2Redis(
      String userProfileRedisKey, FindUserProfileRspVO findUserProfileRspVO) {
    threadPoolTaskExecutor.submit(
        () -> {
          // 过期时间
          long expireTime = 60 * 60 + RandomUtil.randomInt(60 * 60);
          // vo转json写入redis
          redisTemplate
              .opsForValue()
              .set(
                  userProfileRedisKey,
                  JsonUtils.toJsonString(findUserProfileRspVO),
                  expireTime,
                  TimeUnit.SECONDS);
        });
  }
}
