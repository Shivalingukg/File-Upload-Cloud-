package com.example.fileupload.dto;

public record PresignResponse(String uploadUrl, String key, long expiresIn) {}
