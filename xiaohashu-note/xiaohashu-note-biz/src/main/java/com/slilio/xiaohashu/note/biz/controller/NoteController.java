package com.slilio.xiaohashu.note.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.note.biz.model.vo.PublishNoteReqVO;
import com.slilio.xiaohashu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/note")
@Slf4j
public class NoteController {
  @Resource private NoteService noteService;

  @PostMapping(value = "/publish")
  @ApiOperationLog(description = "笔记发布")
  public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO) {
    return noteService.publishNote(publishNoteReqVO);
  }
}
