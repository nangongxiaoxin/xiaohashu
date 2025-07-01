package com.slilio.xiaohashu.count.fallback;

import com.google.common.collect.Lists;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.api.CountFeignApi;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@Component
public class CountFeignApiFallback implements CountFeignApi {
  /**
   * 查询用户计数
   *
   * @param findUserCountsByIdReqDTO
   * @return
   */
  @Override
  public Response<FindUserCountsByIdRspDTO> findUserCount(
      FindUserCountsByIdReqDTO findUserCountsByIdReqDTO) {
    // 要查询的用户ID
    Long userId = findUserCountsByIdReqDTO.getUserId();

    // 降级后
    return Response.success(
        FindUserCountsByIdRspDTO.builder()
            .userId(userId)
            .noteTotal(0L)
            .likeTotal(0L)
            .followingTotal(0L)
            .fansTotal(0L)
            .collectTotal(0L)
            .build());
  }

  /**
   * 批量查询笔记数量
   *
   * @param findNoteCountsByIdsReqDTO
   * @return
   */
  @Override
  public Response<List<FindNoteCountsByIdRspDTO>> findNotesCount(
      FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO) {

    List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS = Lists.newArrayList();

    List<Long> noteIds = findNoteCountsByIdsReqDTO.getNoteIds();

    noteIds.forEach(
        noteId ->
            findNoteCountsByIdRspDTOS.add(
                FindNoteCountsByIdRspDTO.builder()
                    .noteId(noteId)
                    .collectTotal(0L)
                    .commentTotal(0L)
                    .likeTotal(0L)
                    .build()));

    return Response.success(findNoteCountsByIdRspDTOS);
  }
}
