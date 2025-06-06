package com.slilio.xiaohashu.comment.biz.rpc;

import com.slilio.xiaohashu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Component
public class DistributedIdGeneratorRpcService {
  @Resource private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

  /**
   * 生成评论ID
   *
   * @return
   */
  public String generateCommentId() {
    return distributedIdGeneratorFeignApi.getSegmentId("leaf-segment-comment-id");
  }
}
