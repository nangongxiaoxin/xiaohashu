package com.slilio.xiaohashu.user.relation.biz.domain.mapper;

import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FollowingDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(FollowingDO record);

  int insertSelective(FollowingDO record);

  FollowingDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(FollowingDO record);

  int updateByPrimaryKey(FollowingDO record);

  List<FollowingDO> selectByUserId(Long userId);

  int deleteByUserIdAndFollowingUserId(
      @Param("userId") Long userId, @Param("unfollowUserId") Long unfollowUserId);

  /**
   * 关注人数查询
   *
   * @param userId
   * @return
   */
  long selectCountByUserId(Long userId);

  /**
   * 分页查询
   *
   * @param userId
   * @param offset
   * @param limit
   * @return
   */
  List<FollowingDO> selectPageListByUserId(
      @Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

  /**
   * 查询全部关注用户列表
   *
   * @param userId
   * @return
   */
  List<FollowingDO> selectAllByUserId(Long userId);
}
