package com.slilio.xiaohashu.comment.biz.domain.mapper;

import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentLikeDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CommentLikeDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(CommentLikeDO record);

  int insertSelective(CommentLikeDO record);

  CommentLikeDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(CommentLikeDO record);

  int updateByPrimaryKey(CommentLikeDO record);

  /**
   * 查询某个评论是否被点赞
   *
   * @param userId
   * @param commentId
   * @return
   */
  int selectCountByUserIdAndCommentId(
      @Param("userId") Long userId, @Param("commentId") Long commentId);

  /**
   * 查询对应用户点赞的所有评论
   *
   * @param userId
   * @return
   */
  List<CommentLikeDO> selectByUserId(@Param("userId") Long userId);
}
