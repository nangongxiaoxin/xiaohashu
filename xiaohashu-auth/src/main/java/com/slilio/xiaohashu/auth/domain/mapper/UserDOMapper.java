package com.slilio.xiaohashu.auth.domain.mapper;

import com.slilio.xiaohashu.auth.domain.dataobject.UserDO;

public interface UserDOMapper {

    /**
     * 根据主键ID查询
     * @param id
     * @return
     */
    UserDO selectByPrimaryKey(Long id);

    /**
     * 根据主键id删除
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 插入记录
     * @param record
     * @return
     */
    int insert(UserDO record);

    /**
     * 更新记录
     * @param record
     * @return
     */
    int updateByPrimaryKey(UserDO record);
}
