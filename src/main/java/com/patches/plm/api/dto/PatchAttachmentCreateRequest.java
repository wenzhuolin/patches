package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PatchAttachmentCreateRequest(
        @NotBlank String stage,
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        String fileHash,
        Long fileSize,
        String scanStatus
) {
}
