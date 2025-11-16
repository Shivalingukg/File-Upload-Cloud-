package com.example.fileupload.controller;

import com.example.fileupload.dto.ConfirmRequest;
import com.example.fileupload.dto.PresignRequest;
import com.example.fileupload.dto.PresignResponse;
import com.example.fileupload.entity.FileMeta;
import com.example.fileupload.service.FileService;
import com.example.fileupload.service.FileService.PresignResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {

  private final FileService fileService;
  private final long presignExpiry;

  public FileController(FileService fileService, @Value("${app.s3.presign.expireSeconds}") long presignExpiry) {
    this.fileService = fileService;
    this.presignExpiry = presignExpiry;
  }

  // Simple header-based user id simulation (replace with real auth)
  private String userIdFrom(String header) {
    if (header == null || header.isBlank()) throw new RuntimeException("unauth");
    return header;
  }

  @PostMapping("/presign")
  public ResponseEntity<PresignResponse> presign(@RequestHeader(value = "X-User-Id", required = false) String user,
                                                 @Validated @RequestBody PresignRequest req) {
    String userId = userIdFrom(user);
    PresignResult res = fileService.createPresign(userId, req.filename(), req.contentType());
    return ResponseEntity.ok(new PresignResponse(res.url(), res.key(), presignExpiry));
  }

  @PostMapping("/confirm")
  public ResponseEntity<?> confirm(@RequestHeader(value = "X-User-Id", required = false) String user,
                                   @Validated @RequestBody ConfirmRequest req) {
    String userId = userIdFrom(user);
    FileMeta meta = fileService.confirm(userId, req.key(), req.filename(), req.contentType(), req.size());
    return ResponseEntity.status(201).body(Map.of(
      "id", meta.getId(),
      "key", meta.getKey(),
      "filename", meta.getFilename(),
      "createdAt", meta.getCreatedAt()
    ));
  }

  @GetMapping
  public ResponseEntity<?> list(@RequestHeader(value = "X-User-Id", required = false) String user,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
    String userId = userIdFrom(user);
    var items = fileService.list(userId, PageRequest.of(page, size));
    var body = Map.of(
        "items", items.getContent().stream().map(m -> Map.of(
            "id", m.getId(),
            "key", m.getKey(),
            "filename", m.getFilename(),
            "size", m.getSize(),
            "contentType", m.getContentType(),
            "createdAt", m.getCreatedAt()
        )).collect(Collectors.toList()),
        "page", items.getNumber(),
        "size", items.getSize(),
        "totalPages", items.getTotalPages(),
        "totalElements", items.getTotalElements()
    );
    return ResponseEntity.ok(body);
  }

  @GetMapping("/{key}/download")
  public ResponseEntity<?> download(@RequestHeader(value = "X-User-Id", required = false) String user,
                                    @PathVariable String key) {
    String userId = userIdFrom(user);
    try {
      URL url = fileService.download(userId, key);
      return ResponseEntity.ok(Map.of("downloadUrl", url.toString(), "expiresIn", presignExpiry));
    } catch (IllegalAccessException e) {
      return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
    } catch (Exception e) {
      return ResponseEntity.status(404).body(Map.of("error", "not_found"));
    }
  }

  @DeleteMapping("/{key}")
  public ResponseEntity<?> delete(@RequestHeader(value = "X-User-Id", required = false) String user,
                                  @PathVariable String key) {
    String userId = userIdFrom(user);
    try {
      fileService.delete(userId, key);
      return ResponseEntity.noContent().build();
    } catch (IllegalAccessException e) {
      return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
    } catch (Exception e) {
      return ResponseEntity.status(404).body(Map.of("error", "not_found"));
    }
  }
}
    