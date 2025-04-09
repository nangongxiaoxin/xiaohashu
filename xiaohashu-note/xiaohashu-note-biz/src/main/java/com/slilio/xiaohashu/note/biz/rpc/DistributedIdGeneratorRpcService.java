package com.slilio.xiaohashu.note.biz.rpc;

import com.slilio.xiaohashu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {
  @Resource private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

  public String getSnowflakeId() {
    /** 生成雪花算法ID */
    return distributedIdGeneratorFeignApi.getSnowflakeId("test");
  }
}
