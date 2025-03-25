package com.slilio.xiaohashu.oss.biz.service.impl;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.oss.biz.service.FileService;
import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
  @Resource private FileStrategy fileStrategy;
  private static final String BUCKET_NAME = "xiaohashu";

  /**
   * 文件上传
   *
   * @param file
   * @return
   */
  @Override
  public Response<?> uploadFile(MultipartFile file) {
    // 上传文件到哪里
    String url = fileStrategy.uploadFile(file, BUCKET_NAME);
    return Response.success(url);
  }
}
