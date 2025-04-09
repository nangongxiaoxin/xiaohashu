package com.slilio.xiaohashu.note.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.note.biz.model.vo.PublishNoteReqVO;

public interface NoteService {
  /**
   * 笔记发布
   *
   * @param publishNoteReqVO
   * @return
   */
  Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);
}
