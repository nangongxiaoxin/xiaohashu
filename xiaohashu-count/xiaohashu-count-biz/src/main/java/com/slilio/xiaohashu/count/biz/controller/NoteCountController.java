package com.slilio.xiaohashu.count.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.biz.service.NoteCountService;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
@RestController
@RequestMapping("/count")
@Slf4j
public class NoteCountController {
  @Resource private NoteCountService noteCountService;

  @PostMapping(value = "/notes/data")
  @ApiOperationLog(description = "批量获取笔记计数数据")
  public Response<List<FindNoteCountsByIdRspDTO>> findNotesCountData(
      @Validated @RequestBody FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO) {
    return noteCountService.findNotesCountData(findNoteCountsByIdsReqDTO);
  }
}
