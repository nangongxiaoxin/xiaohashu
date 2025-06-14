package com.slilio.xiaohashu.comment.biz.domain.mapper;

import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentLikeDO;

public interface CommentLikeDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CommentLikeDO record);

    int insertSelective(CommentLikeDO record);

    CommentLikeDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CommentLikeDO record);

    int updateByPrimaryKey(CommentLikeDO record);
}