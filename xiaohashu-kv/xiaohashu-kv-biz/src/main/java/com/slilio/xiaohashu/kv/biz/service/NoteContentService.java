package com.slilio.xiaohashu.kv.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;

public interface NoteContentService {
  /**
   * 添加笔记内容
   *
   * @param addNoteContentReqDTO
   * @return
   */
  Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

  /**
   * 查询笔记内容
   *
   * @param findNoteContentReqDTO
   * @return
   */
  Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);
}
