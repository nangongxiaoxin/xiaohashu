package com.slilio.xiaohashu.note.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.note.biz.model.vo.*;

public interface NoteService {
  /**
   * 笔记发布
   *
   * @param publishNoteReqVO
   * @return
   */
  Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

  /**
   * 笔记详情
   *
   * @param findNoteDetailReqVO
   * @return
   */
  Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

  /**
   * 笔记更新
   *
   * @param updateNoteReqVO
   * @return
   */
  Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

  /**
   * 删除本地笔记缓存
   *
   * @param noteId
   */
  void deleteNoteLocalCache(Long noteId);

  /**
   * 删除笔记
   *
   * @param deleteNoteReqVO
   * @return
   */
  Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

  /**
   * 笔记仅自己可见
   *
   * @param updateNoteVisibleOnlyMeReqVO
   * @return
   */
  Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

  /**
   * 笔记置顶、取消
   *
   * @param topNoteReqVO
   * @return
   */
  Response<?> topNote(TopNoteReqVO topNoteReqVO);

  /**
   * 点赞笔记
   *
   * @param likeNoteReqVO
   * @return
   */
  Response<?> likeNote(LikeNoteReqVO likeNoteReqVO);

  /**
   * 取消点赞笔记
   *
   * @param unLikeNoteReqVO
   * @return
   */
  Response<?> unLikeNote(UnlikeNoteReqVO unLikeNoteReqVO);

  /**
   * 收藏笔记
   *
   * @param collectNoteReqVO
   * @return
   */
  Response<?> collectNote(CollectNoteReqVO collectNoteReqVO);

  /**
   * 取消收藏笔记
   *
   * @param unCollectNoteReqVO
   * @return
   */
  Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO);

  /**
   * 获取是否点赞，收藏数据
   *
   * @param findNoteIsLikedAndCollectedReqVO
   * @return
   */
  Response<FindNoteIsLikedAndCollectedRspVO> isLikedAndCollectedData(
      FindNoteIsLikedAndCollectedReqVO findNoteIsLikedAndCollectedReqVO);

  /**
   * 用户主页查询-查询已经发布的笔记列表
   *
   * @param findPublishedNoteListReqVO
   * @return
   */
  Response<FindPublishedNoteListRspVO> findPublishedNoteList(
      FindPublishedNoteListReqVO findPublishedNoteListReqVO);
}
