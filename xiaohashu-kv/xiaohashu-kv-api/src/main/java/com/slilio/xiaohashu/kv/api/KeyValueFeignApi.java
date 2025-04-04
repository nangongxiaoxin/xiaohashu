package com.slilio.xiaohashu.kv.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.constant.ApiConstants;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
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
  Response<FindNoteContentRspDTO> findNoteContent(@RequestBody FindNoteContentReqDTO findNoteContentReqDTO);
}
