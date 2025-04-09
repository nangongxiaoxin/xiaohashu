package com.slilio.xiaohashu.kv.biz.service.impl;

import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.slilio.xiaohashu.kv.biz.domain.repository.NoteContentRepository;
import com.slilio.xiaohashu.kv.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.kv.biz.service.NoteContentService;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
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
    String uuid = addNoteContentReqDTO.getUuid();
    // 笔记内容
    String content = addNoteContentReqDTO.getContent();

    // 构建数据库DO实体类
    NoteContentDO noteContentDO =
        NoteContentDO.builder()
            .id(UUID.fromString(uuid)) // todo 暂时使用UUID
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
    String uuid = findNoteContentReqDTO.getUuid();
    // 根据笔记ID查询笔记内容
    Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(uuid));

    // 若笔记内容不存在
    if (!optional.isPresent()) {
      throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
    }

    NoteContentDO noteContentDO = optional.get();
    // 构建返参DTO
    FindNoteContentRspDTO findNoteContentRspDTO =
        FindNoteContentRspDTO.builder()
            .uuid(noteContentDO.getId())
            .content(noteContentDO.getContent())
            .build();

    return Response.success(findNoteContentRspDTO);
  }

  /**
   * 删除笔记内容
   *
   * @param deleteNoteContentReqDTO
   * @return
   */
  @Override
  public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
    // 笔记ID
    String uuid = deleteNoteContentReqDTO.getUuid();
    // 删除笔记内容
    noteContentRepository.deleteById(UUID.fromString(uuid));

    return Response.success();
  }
}
