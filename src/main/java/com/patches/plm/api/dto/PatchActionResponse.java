package com.patches.plm.api.dto;

import com.patches.plm.domain.enums.PatchState;

import java.util.List;

public record PatchActionResponse(
        PatchState fromState,
        PatchState toState,
        boolean kpiPassed,
        boolean qaPassed,
        List<String> reasons
) {
}
