package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.PatchAction;
import jakarta.validation.constraints.NotNull;

public record PatchActionRequest(
        @NotNull PatchAction action,
        String comment
) {
}
