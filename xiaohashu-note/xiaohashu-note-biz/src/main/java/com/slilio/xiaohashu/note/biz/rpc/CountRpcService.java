package com.slilio.xiaohashu.note.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.api.CountFeignApi;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
@Component
public class CountRpcService {

  @Resource private CountFeignApi countFeignApi;

  /**
   * 批量查询笔记计数
   *
   * @param noteIds
   * @return
   */
  public List<FindNoteCountsByIdRspDTO> findByNotesIds(List<Long> noteIds) {
    FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO = new FindNoteCountsByIdsReqDTO();
    findNoteCountsByIdsReqDTO.setNoteIds(noteIds);

    Response<List<FindNoteCountsByIdRspDTO>> response =
        countFeignApi.findNotesCount(findNoteCountsByIdsReqDTO);

    if (!response.isSuccess()
        || Objects.isNull(response.getData())
        || CollUtil.isEmpty(response.getData())) {
      return null;
    }

    return response.getData();
  }
}
