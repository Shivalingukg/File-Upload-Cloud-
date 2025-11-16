package com.example.fileupload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.net.URL;
import java.time.Duration;

@Service
public class S3Service {

  private final S3Client s3;
  private final S3Presigner presigner;
  private final String bucket;
  private final long presignSeconds;

  public S3Service(S3Client s3, S3Presigner presigner,
                   @Value("${app.s3.bucket}") String bucket,
                   @Value("${app.s3.presign.expireSeconds}") long presignSeconds) {
    this.s3 = s3;
    this.presigner = presigner;
    this.bucket = bucket;
    this.presignSeconds = presignSeconds;
  }

  public String generateKey(String userId, String filename) {
    String ts = String.valueOf(System.currentTimeMillis());
    String safe = filename.replaceAll("\\s+","_");
    return String.format("%s/%s_%s", userId, ts, safe);
  }

  public URL presignPutUrl(String key, String contentType) {
    PutObjectRequest putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(presignSeconds))
        .putObjectRequest(putReq)
        .build();

    PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
    return presigned.url();
  }

  public URL presignGetUrl(String key) {
    GetObjectRequest getReq = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(presignSeconds))
        .getObjectRequest(getReq)
        .build();

    PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
    return presigned.url();
  }

  public void delete(String key) {
    DeleteObjectRequest req = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();
    s3.deleteObject(req);
  }

  public boolean exists(String key) {
    try {
      HeadObjectRequest req = HeadObjectRequest.builder().bucket(bucket).key(key).build();
      s3.headObject(req);
      return true;
    } catch (S3Exception e) {
      return false;
    }
  }
}
