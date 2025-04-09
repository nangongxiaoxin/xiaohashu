package com.slilio.xiaohashu.note.biz.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.api.KeyValueFeignApi;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class KeyValueRpcService {
  @Resource private KeyValueFeignApi keyValueFeignApi;

  /**
   * 保存笔记内容
   *
   * @param uuid
   * @param content
   * @return
   */
  public boolean saveNoteContent(String uuid, String content) {
    AddNoteContentReqDTO addNoteContentReqDTO = new AddNoteContentReqDTO();

    addNoteContentReqDTO.setUuid(uuid);
    addNoteContentReqDTO.setContent(content);

    Response<?> response = keyValueFeignApi.addNoteContent(addNoteContentReqDTO);

    if (Objects.isNull(response) || !response.isSuccess()) {
      return false;
    }

    return true;
  }

  /**
   * 删除笔记内容
   *
   * @param uuid
   * @return
   */
  public boolean deleteNoteContent(String uuid) {
    DeleteNoteContentReqDTO deleteNoteContentReqDTO = new DeleteNoteContentReqDTO();
    deleteNoteContentReqDTO.setUuid(uuid);

    Response<?> response = keyValueFeignApi.deleteNoteContent(deleteNoteContentReqDTO);

    if (Objects.isNull(response) || !response.isSuccess()) {
      return false;
    }

    return true;
  }
}
