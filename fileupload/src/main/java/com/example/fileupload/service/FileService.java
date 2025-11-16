package com.example.fileupload.service;

import com.example.fileupload.entity.FileMeta;
import com.example.fileupload.repository.FileMetaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.OffsetDateTime;

@Service
public class FileService {

  private final S3Service s3;
  private final FileMetaRepository repo;

  public FileService(S3Service s3, FileMetaRepository repo) {
    this.s3 = s3;
    this.repo = repo;
  }

  /**
   * Returns presigned PUT URL and key as pair (array[0]=url, array[1]=key).
   */
  public PresignResult createPresign(String userId, String filename, String contentType) {
    String key = s3.generateKey(userId, filename);
    URL url = s3.presignPutUrl(key, contentType);
    return new PresignResult(url.toString(), key);
  }

  public FileMeta confirm(String userId, String key, String filename, String contentType, long size) {
    // optional: verify S3 object exists using s3.exists(key)
    FileMeta meta = FileMeta.builder()
        .key(key)
        .filename(filename)
        .contentType(contentType)
        .size(size)
        .userId(userId)
        .createdAt(OffsetDateTime.now())
        .build();
    return repo.save(meta);
  }

  public Page<FileMeta> list(String userId, Pageable page) {
    return repo.findByUserId(userId, page);
  }

  public URL download(String userId, String key) throws IllegalAccessException {
    FileMeta meta = repo.findByKey(key).orElseThrow();
    if (!meta.getUserId().equals(userId)) throw new IllegalAccessException("forbidden");
    return s3.presignGetUrl(key);
  }

  public void delete(String userId, String key) throws IllegalAccessException {
    FileMeta meta = repo.findByKey(key).orElseThrow();
    if (!meta.getUserId().equals(userId)) throw new IllegalAccessException("forbidden");
    s3.delete(key);
    repo.deleteByKey(key);
  }

  // small helper record
  public static class PresignResult {
    private final String url;
    private final String key;
    public PresignResult(String url, String key) { this.url = url; this.key = key; }
    public String url() { return url; }
    public String key() { return key; }
  }
}
