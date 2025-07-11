package com.slilio.xiaohashu.count.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.constants.ApiConstants;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import com.slilio.xiaohashu.count.fallback.CountFeignApiFallback;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: slilio @CreateTime: 2025-06-27 @Description: @Version: 1.0
 */
@FeignClient(name = ApiConstants.SERVICE_NAME, fallback = CountFeignApiFallback.class)
public interface CountFeignApi {
  String PREFIX = "/count";

  /**
   * 查询用户计数
   *
   * @param findUserCountsByIdReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/user/data")
  Response<FindUserCountsByIdRspDTO> findUserCount(
      @RequestBody FindUserCountsByIdReqDTO findUserCountsByIdReqDTO);

  /**
   * 批量查询笔记数量
   *
   * @param findNoteCountsByIdsReqDTO
   * @return
   */
  @PostMapping(value = PREFIX + "/notes/data")
  Response<List<FindNoteCountsByIdRspDTO>> findNotesCount(
      FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO);
}
