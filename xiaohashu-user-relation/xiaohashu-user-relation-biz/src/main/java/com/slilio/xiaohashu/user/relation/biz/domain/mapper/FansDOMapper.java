package com.slilio.xiaohashu.user.relation.biz.domain.mapper;

import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FansDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(FansDO record);

  int insertSelective(FansDO record);

  FansDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(FansDO record);

  int updateByPrimaryKey(FansDO record);

  int deleteByUserIdAndFansUserId(
      @Param("userId") Long userId, @Param("fansUserId") Long fansUserId);

  /**
   * 查询记录总数
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
  List<FansDO> selectPageListByUserId(
      @Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

  /**
   * 查询最新关注的5000位粉丝
   *
   * @param userId
   * @return
   */
  List<FansDO> select5000FansByUserId(Long userId);
}
