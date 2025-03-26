package com.slilio.xiaohashu.oss.biz.strategy.impl;

import com.aliyun.oss.OSS;
import com.slilio.xiaohashu.oss.biz.config.AliyunOSSProperties;
import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class AliyunOSSFileStrategy implements FileStrategy {
  @Resource private AliyunOSSProperties aliyunOSSProperties;
  @Resource private OSS ossClient;

  /**
   * 上传文件
   *
   * @param file
   * @param bucketName
   * @return
   */
  @Override
  @SneakyThrows
  public String uploadFile(MultipartFile file, String bucketName) {
    log.info("## 上传文件至 阿里云OSS 。。。");

    // 判断文件是否为空
    if (file == null || file.getSize() == 0) {
      log.error("===》 上传文件异常：文件大小为空。。。");
      throw new RuntimeException("文件大小为空");
    }

    // 文件的原始名称
    String originalFilename = file.getOriginalFilename();
    // 生成存储对象的名称（UUID）
    String key = UUID.randomUUID().toString().replace("-", "");
    // 获取文件的后缀
    String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

    // 拼接上文件后缀，即为要存储的文件名
    String objectName = String.format("%s%s", key, suffix);

    log.info("===》 开始上传文件至阿里云OSS，ObjectName：{}", objectName);

    // 上传文件至OSS
    ossClient.putObject(
        bucketName, objectName, new ByteArrayInputStream(file.getInputStream().readAllBytes()));

    // 返回文件的访问链接
    String url =
        String.format(
            "https://%s.%s/%s", bucketName, aliyunOSSProperties.getEndpoint(), objectName);
    log.info("==> 上传文件至阿里云 OSS 成功，访问路径: {}", url);
    return url;
  }
}
