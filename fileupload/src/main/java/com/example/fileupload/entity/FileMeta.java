package com.example.fileupload.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "file_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMeta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String key; // s3 key

  @Column(nullable = false)
  private String filename;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Long size;

  @Column(nullable = false)
  private String userId; // owner id

  private String thumbnailKey;

  private OffsetDateTime createdAt;
}
