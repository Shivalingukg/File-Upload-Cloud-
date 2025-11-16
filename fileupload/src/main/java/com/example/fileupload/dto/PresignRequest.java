package com.example.fileupload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresignRequest(
  @NotBlank String filename,
  @NotBlank String contentType,
  @Positive long size
) {}
