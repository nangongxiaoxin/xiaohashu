package com.slilio.xiaohashu.oss.biz.strategy.impl;

import com.slilio.xiaohashu.oss.biz.config.MinioProperties;
import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class MinioFileStrategy implements FileStrategy {

  @Resource private MinioProperties minioProperties;
  @Resource private MinioClient minioClient;

  @Value("${storage.minio-bucket-name}")
  private String bucketName;

  /**
   * 文件上传
   *
   * @param file
   * @return
   */
  @Override
  @SneakyThrows
  public String uploadFile(MultipartFile file) {
    log.info("## 上传文件至 Minio 。。。");
    // 判断文件是否为空
    if (file == null || file.getSize() == 0) {
      log.error("===》 上传文件异常：文件大小为空。。。");
      throw new RuntimeException("文件大小不能为空");
    }

    // 文件的原始名称
    String originalFilename = file.getOriginalFilename();
    // 文件的Content-Type
    String contentType = file.getContentType();

    // 生成存储对象名称（将UUID字符串的-替换为空字符串）
    String key = UUID.randomUUID().toString().replace("-", "");
    // 获取文件的后缀
    String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

    // 拼接上文件后缀，即为要存储的文件名
    String objectName = String.format("%s%s", key, suffix);

    log.info("===》 文件上传至Minio,ObjectName:{}", objectName);

    // 上传文件至Minio
    minioClient.putObject(
        PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                file.getInputStream(), file.getSize(), -1)
            .contentType(contentType)
            .build());

    // 返回文件的访问链接
    String url = String.format("%s/%s/%s", minioProperties.getEndpoint(), bucketName, objectName);
    log.info("===> 上传文件至Minio成功，访问路径：{}", url);
    return url;
  }
}
