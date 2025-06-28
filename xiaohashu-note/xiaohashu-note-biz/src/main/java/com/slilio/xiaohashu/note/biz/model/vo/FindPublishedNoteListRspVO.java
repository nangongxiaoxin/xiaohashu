package com.slilio.xiaohashu.note.biz.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-28 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindPublishedNoteListRspVO {
  private List<NoteItemRspVO> notes; // 笔记分页数据

  private Long nextCursor; // 下一页游标
}
