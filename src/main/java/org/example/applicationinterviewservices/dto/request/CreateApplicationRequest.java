package org.example.applicationinterviewservices.dto.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record CreateApplicationRequest(
        UUID jobId,
        String firstName,
        String lastName,
        String email,
        String phone,
        MultipartFile resume
) { }
