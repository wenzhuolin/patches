package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record PatchAttachmentResponse(
        Long id,
        Long patchId,
        String stage,
        String fileName,
        String fileUrl,
        String fileHash,
        Long fileSize,
        Long uploaderId,
        String scanStatus,
        OffsetDateTime createdAt
) {
}
