package com.slilio.xiaohashu.count.biz.service;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import java.util.List;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
public interface NoteCountService {

  /**
   * 批量查询笔记计数
   *
   * @param findNoteCountsByIdsReqDTO
   * @return
   */
  Response<List<FindNoteCountsByIdRspDTO>> findNotesCountData(
      FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO);
}
