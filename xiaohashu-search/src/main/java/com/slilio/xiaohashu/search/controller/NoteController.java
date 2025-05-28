package com.slilio.xiaohashu.search.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.xiaohashu.search.model.vo.SearchNoteReqVO;
import com.slilio.xiaohashu.search.model.vo.SearchNoteRspVO;
import com.slilio.xiaohashu.search.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-05-29 @Description: @Version: 1.0
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class NoteController {
  @Resource private NoteService noteService;

  @PostMapping("/note")
  @ApiOperationLog(description = "搜索笔记")
  public PageResponse<SearchNoteRspVO> searchNote(
      @RequestBody @Validated SearchNoteReqVO searchNoteReqVO) {
    return noteService.searchNote(searchNoteReqVO);
  }
}
