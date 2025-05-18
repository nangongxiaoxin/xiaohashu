package com.slilio.xiaohashu.data.align.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-19 @Description: @Version: 1.0
 */
@Component
public class CreateTableXxlJob {
  @XxlJob("createTableJobHandler")
  public void createTableJobHandler() throws Exception {
    XxlJobHelper.log("## 开始初始化明日增量数据表...");

    // todo
  }
}
