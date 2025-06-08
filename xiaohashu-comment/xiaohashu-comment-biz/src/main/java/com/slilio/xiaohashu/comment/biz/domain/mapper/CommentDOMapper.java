package com.slilio.xiaohashu.comment.biz.domain.mapper;

import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.model.bo.CommentBO;
import com.slilio.xiaohashu.comment.biz.model.bo.CommentHeatBO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CommentDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(CommentDO record);

  int insertSelective(CommentDO record);

  CommentDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(CommentDO record);

  int updateByPrimaryKey(CommentDO record);

  /**
   * 根据评论ID批量查询评论信息
   *
   * @param commentIds
   * @return
   */
  List<CommentDO> selectByCommentIds(@Param("commentIds") List<Long> commentIds);

  /**
   * 评论插入评论
   *
   * @param comments
   * @return
   */
  int batchInsert(@Param("comments") List<CommentBO> comments);

  /**
   * 批量更新热度值
   *
   * @param commentIds
   * @param commentHeatBOS
   * @return
   */
  int batchUpdateHeatByCommentIds(
      @Param("commentIds") List<Long> commentIds,
      @Param("commentHeatBOS") List<CommentHeatBO> commentHeatBOS);
}
