package com.slilio.xiaohashu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublishNoteReqVO {
  @NotNull(message = "笔记类型不能为空")
  private Integer type;

  private List<String> imgUris;

  private String videoUri;

  private String title;

  private String content;

  private Long topicId;
}
