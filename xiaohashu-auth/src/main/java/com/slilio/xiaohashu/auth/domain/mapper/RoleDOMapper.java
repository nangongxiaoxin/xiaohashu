package com.slilio.xiaohashu.auth.domain.mapper;

import com.slilio.xiaohashu.auth.domain.dataobject.RoleDO;

public interface RoleDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoleDO record);

    int insertSelective(RoleDO record);

    RoleDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoleDO record);

    int updateByPrimaryKey(RoleDO record);
}