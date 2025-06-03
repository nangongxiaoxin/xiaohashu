package com.slilio.xiaohashu.data.align.rpc;

import com.slilio.xiaohashu.search.api.SearchFeignApi;
import com.slilio.xiaohashu.search.dto.RebuildNoteDocumentReqDTO;
import com.slilio.xiaohashu.search.dto.RebuildUserDocumentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-03 @Description: @Version: 1.0
 */
@Component
public class SearchRpcService {
  @Resource private SearchFeignApi searchFeignApi;

  /**
   * 调用重建笔记文档接口
   *
   * @param noteId
   */
  public void rebuildNoteDocument(Long noteId) {
    RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO =
        RebuildNoteDocumentReqDTO.builder().id(noteId).build();

    searchFeignApi.rebuildNoteDocument(rebuildNoteDocumentReqDTO);
  }

  /**
   * 调用重建用户文档接口
   *
   * @param userId
   */
  public void rebuildUserDocument(Long userId) {
    RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO =
        RebuildUserDocumentReqDTO.builder().id(userId).build();

    searchFeignApi.rebuildUserDocument(rebuildUserDocumentReqDTO);
  }
}
