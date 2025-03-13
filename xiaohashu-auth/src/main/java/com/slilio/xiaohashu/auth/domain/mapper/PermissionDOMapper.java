package com.slilio.xiaohashu.auth.domain.mapper;

import com.slilio.xiaohashu.auth.domain.dataobject.PermissionDO;
import java.util.List;

public interface PermissionDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(PermissionDO record);

  int insertSelective(PermissionDO record);

  PermissionDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(PermissionDO record);

  int updateByPrimaryKey(PermissionDO record);

  /**
   * 查询app端所有被启用的权限
   *
   * @return
   */
  List<PermissionDO> selectAppEnabledList();
}
