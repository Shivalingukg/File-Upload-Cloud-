package com.example.fileupload.repository;

import com.example.fileupload.entity.FileMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
  Page<FileMeta> findByUserId(String userId, Pageable pageable);
  Optional<FileMeta> findByKey(String key);
  void deleteByKey(String key);
}
