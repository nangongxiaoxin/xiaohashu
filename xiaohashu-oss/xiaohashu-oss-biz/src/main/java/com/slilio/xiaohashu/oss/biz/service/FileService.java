package com.slilio.xiaohashu.oss.biz.service;

import com.slilio.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

  /**
   * 上传文件
   *
   * @param file
   * @return
   */
  Response<?> upload(MultipartFile file);
}
