package com.slilio.xiaohashu.oss.biz.strategy.impl;

import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class MinioFileStrategy implements FileStrategy {

  /**
   * 文件上传
   *
   * @param file
   * @param bucketName
   * @return
   */
  @Override
  public String upload(MultipartFile file, String bucketName) {
    log.info("## 上传文件至 Minio 。。。");
    return null;
  }
}
