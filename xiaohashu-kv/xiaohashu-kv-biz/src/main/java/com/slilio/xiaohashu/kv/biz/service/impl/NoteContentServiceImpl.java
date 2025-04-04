package com.slilio.xiaohashu.kv.biz.service.impl;

import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.slilio.xiaohashu.kv.biz.domain.repository.NoteContentRepository;
import com.slilio.xiaohashu.kv.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.kv.biz.service.NoteContentService;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {
  @Resource private NoteContentRepository noteContentRepository;

  /**
   * 添加笔记内容
   *
   * @param addNoteContentReqDTO
   * @return
   */
  @Override
  public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
    // 笔记ID
    Long noteId = addNoteContentReqDTO.getNoteId();
    // 笔记内容
    String content = addNoteContentReqDTO.getContent();

    // 构建数据库DO实体类
    NoteContentDO noteContentDO =
        NoteContentDO.builder()
            .id(UUID.randomUUID()) // todo 暂时使用UUID
            .content(content)
            .build();
    // 插入数据
    noteContentRepository.save(noteContentDO);
    return Response.success();
  }

  /**
   * 查询笔记内容
   *
   * @param findNoteContentReqDTO
   * @return
   */
  @Override
  public Response<FindNoteContentRspDTO> findNoteContent(
      FindNoteContentReqDTO findNoteContentReqDTO) {
    // 笔记ID
    String noteId = findNoteContentReqDTO.getNoteId();
    // 根据笔记ID查询笔记内容
    Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(noteId));

    // 若笔记内容不存在
    if (!optional.isPresent()) {
      throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
    }

    NoteContentDO noteContentDO = optional.get();
    // 构建返参DTO
    FindNoteContentRspDTO findNoteContentRspDTO =
        FindNoteContentRspDTO.builder()
            .noteId(noteContentDO.getId())
            .content(noteContentDO.getContent())
            .build();

    return Response.success(findNoteContentRspDTO);
  }
}
