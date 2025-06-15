package com.slilio.xiaohashu.kv.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.constant.ApiConstants;
import com.slilio.xiaohashu.kv.dto.req.*;
import com.slilio.xiaohashu.kv.dto.rsp.FindCommentContentRspDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** kv键值存储Feign接口 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {
  String PREFIX = "/kv";

  @PostMapping(value = PREFIX + "/note/content/add")
  Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

  @PostMapping(value = PREFIX + "/note/content/find")
  Response<FindNoteContentRspDTO> findNoteContent(
      @RequestBody FindNoteContentReqDTO findNoteContentReqDTO);

  @PostMapping(value = PREFIX + "/note/content/delete")
  Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO);

  @PostMapping(value = PREFIX + "/comment/content/batchAdd")
  Response<?> batchAddCommentContent(
      @RequestBody BatchAddCommentContentReqDTO batchAddCommentContentReqDTO);

  @PostMapping(value = PREFIX + "/comment/content/batchFind")
  Response<List<FindCommentContentRspDTO>> batchFindCommentContent(
      @RequestBody BatchFindCommentContentReqDTO batchFindCommentContentReqDTO);
}
