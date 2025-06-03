package com.slilio.xiaohashu.search.biz.service;

import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.search.biz.model.vo.SearchNoteReqVO;
import com.slilio.xiaohashu.search.biz.model.vo.SearchNoteRspVO;
import com.slilio.xiaohashu.search.dto.RebuildNoteDocumentReqDTO;
import com.slilio.xiaohashu.search.dto.RebuildUserDocumentReqDTO;

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

  /**
   * 笔记重建文档
   *
   * @param rebuildNoteDocumentReqDTO
   * @return
   */
  Response<Long> rebuildDocument(RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO);

  /**
   * 用户文档重建
   *
   * @param rebuildUserDocumentReqDTO
   * @return
   */
  Response<Long> rebuildDocument(RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO);
}
