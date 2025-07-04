package com.slilio.xiaohashu.user.biz.domain.mapper;

import com.slilio.xiaohashu.user.biz.domain.dataobject.UserDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(UserDO record);

  int insertSelective(UserDO record);

  UserDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(UserDO record);

  int updateByPrimaryKey(UserDO record);

  /**
   * 根据手机号查询记录
   *
   * @param phone
   * @return
   */
  UserDO selectByPhone(String phone);

  List<UserDO> selectByIds(@Param("ids") List<Long> ids);
}
