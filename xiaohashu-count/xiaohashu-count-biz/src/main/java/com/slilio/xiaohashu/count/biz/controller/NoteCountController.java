package com.slilio.xiaohashu.count.biz.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.biz.enums.ResponseCodeEnum;
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

  // 接口/count/user/count接口达到 关联 流控设定时，会触发限制本接口/notes/data的流量

  @PostMapping(value = "/notes/data")
  @ApiOperationLog(description = "批量获取笔记计数数据")
  @SentinelResource(
      value = "findNotesCountData4Controller",
      blockHandler = "blockHandler4findNotesCountData")
  public Response<List<FindNoteCountsByIdRspDTO>> findNotesCountData(
      @Validated @RequestBody FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO) {
    return noteCountService.findNotesCountData(findNoteCountsByIdsReqDTO);
  }

  /**
   * blockHandler 函数，原方法调用被限流/降级/系统保护的时候调用 注意, 需要包含限流方法的所有参数，和 BlockException 参数
   *
   * @param findNoteCountsByIdsReqDTO
   * @param blockException
   * @return
   */
  public Response<List<FindNoteCountsByIdRspDTO>> blockHandler4findNotesCountData(
      FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO, BlockException blockException) {
    throw new BizException(ResponseCodeEnum.FLOW_LIMIT);
  }
}
