package com.slilio.xiaohashu.oss.biz.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface FileStrategy {

  /**
   * 文件上传
   *
   * @param file
   * @return
   */
  String uploadFile(MultipartFile file);
}
