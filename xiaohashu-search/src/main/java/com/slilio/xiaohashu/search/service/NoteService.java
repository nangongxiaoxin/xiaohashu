package com.slilio.xiaohashu.search.service;

import com.slilio.framework.common.response.PageResponse;
import com.slilio.xiaohashu.search.model.vo.SearchNoteReqVO;
import com.slilio.xiaohashu.search.model.vo.SearchNoteRspVO;

/**
 * @Author: slilio @CreateTime: 2025-05-29 @Description: @Version: 1.0
 */
public interface NoteService {

  /**
   * 搜索笔记
   *
   * @param searchNoteReqVO
   * @return
   */
  PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);
}
