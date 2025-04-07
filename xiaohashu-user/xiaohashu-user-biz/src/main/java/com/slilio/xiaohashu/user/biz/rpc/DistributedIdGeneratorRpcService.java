package com.slilio.xiaohashu.user.biz.rpc;

import com.slilio.xiaohashu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {
  @Resource private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

  /** leaf 号段模式：小哈书ID业务标识 */
  private static final String BIZ_TAG_XIAOHASHU_ID = "leaf-segment-xiaohashu-id";

  /** 调用分布式 ID 生成服务用户 ID */
  private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";

  /**
   * 调用分布式ID生成服务生成小哈书ID
   *
   * @return
   */
  public String getXiaohashuId() {
    return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_XIAOHASHU_ID);
  }

  /** 调用分布式 ID 生成服务用户 ID */
  public String getUserId() {
    return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
  }
}
