package com.slilio.xiaohashu.oss.biz.strategy.impl;

import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class AliyunOSSFileStrategy implements FileStrategy {
  /**
   * 上传文件
   *
   * @param file
   * @param bucketName
   * @return
   */
  @Override
  public String uploadFile(MultipartFile file, String bucketName) {
    log.info("## 上传文件至 阿里云OSS 。。。");
    return "";
  }
}
