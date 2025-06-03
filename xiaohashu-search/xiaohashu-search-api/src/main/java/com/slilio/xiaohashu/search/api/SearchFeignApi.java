package com.slilio.xiaohashu.search.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.search.constant.ApiConstants;
import com.slilio.xiaohashu.search.dto.RebuildNoteDocumentReqDTO;
import com.slilio.xiaohashu.search.dto.RebuildUserDocumentReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: slilio @CreateTime: 2025-06-03 @Description: @Version: 1.0
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface SearchFeignApi {
  String PREFIX = "/search";

  /**
   * 笔记文档重建
   *
   * @param rebuildNoteDocumentReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/note/document/rebuild")
  Response<?> rebuildNoteDocument(@RequestBody RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO);

  /**
   * 用户文档重建
   *
   * @param rebuildUserDocumentReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/user/document/rebuild")
  Response<?> rebuildUserDocument(@RequestBody RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO);
}
