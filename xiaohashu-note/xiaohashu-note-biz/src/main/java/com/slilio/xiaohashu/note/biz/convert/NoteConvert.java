package com.slilio.xiaohashu.note.biz.convert;

import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.slilio.xiaohashu.note.biz.model.dto.PublishNoteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@Mapper
public interface NoteConvert {
  // 初始化convert实例
  NoteConvert INSTANCE = Mappers.getMapper(NoteConvert.class);

  /**
   * 将DO转换为DTO
   *
   * @param bean
   * @return
   */
  PublishNoteDTO convertDO2DTO(NoteDO bean);

  /**
   * 将DTO转换为DO
   *
   * @param bean
   * @return
   */
  NoteDO convertDTO2DO(PublishNoteDTO bean);
}
