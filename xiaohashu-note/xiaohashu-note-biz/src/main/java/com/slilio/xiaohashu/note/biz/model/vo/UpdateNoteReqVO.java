package com.slilio.xiaohashu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateNoteReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long id;

  @NotNull(message = "笔记类型不能为空")
  private Integer type;

  private List<String> imgUris;

  private String videoUri;

  private String title;

  private String content;

  private Long topicId;
}
