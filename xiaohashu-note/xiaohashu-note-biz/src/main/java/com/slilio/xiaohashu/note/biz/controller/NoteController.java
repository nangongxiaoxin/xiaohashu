package com.slilio.xiaohashu.note.biz.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.note.biz.model.vo.*;
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

  @PostMapping(value = "/detail")
  @ApiOperationLog(description = "笔记详情")
  public Response<FindNoteDetailRspVO> findNoteDetail(
      @Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO) {
    return noteService.findNoteDetail(findNoteDetailReqVO);
  }

  @PostMapping("/update")
  @ApiOperationLog(description = "笔记修改")
  public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO) {
    return noteService.updateNote(updateNoteReqVO);
  }

  @PostMapping(value = "/delete")
  @ApiOperationLog(description = "删除笔记")
  public Response<?> deleteNote(@Validated @RequestBody DeleteNoteReqVO deleteNoteReqVO) {
    return noteService.deleteNote(deleteNoteReqVO);
  }

  @PostMapping(value = "/visible/onlyMe")
  @ApiOperationLog(description = "笔记仅对自己可见")
  public Response<?> visibleOnlyMe(
      @Validated @RequestBody UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
    return noteService.visibleOnlyMe(updateNoteVisibleOnlyMeReqVO);
  }

  @PostMapping(value = "/top")
  @ApiOperationLog(description = "置顶、取消置顶笔记")
  public Response<?> topNote(@Validated @RequestBody TopNoteReqVO topNoteReqVO) {
    return noteService.topNote(topNoteReqVO);
  }

  @PostMapping(value = "/like")
  @ApiOperationLog(description = "点赞笔记")
  public Response<?> likeNote(@Validated @RequestBody LikeNoteReqVO likeNoteReqVO) {
    return noteService.likeNote(likeNoteReqVO);
  }

  @PostMapping(value = "/unlike")
  @ApiOperationLog(description = "取消点赞笔记")
  public Response<?> unlikeNote(@Validated @RequestBody UnlikeNoteReqVO unlikeNoteReqVO) {
    return noteService.unLikeNote(unlikeNoteReqVO);
  }

  @PostMapping(value = "/collect")
  @ApiOperationLog(description = "收藏笔记")
  public Response<?> collectNote(@Validated @RequestBody CollectNoteReqVO collectNoteReqVO) {
    return noteService.collectNote(collectNoteReqVO);
  }
}
