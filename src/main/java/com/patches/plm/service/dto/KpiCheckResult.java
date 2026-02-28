package com.patches.plm.service.dto;

import com.patches.plm.api.dto.KpiEvaluationResponse;

import java.util.List;

public record KpiCheckResult(boolean pass, String summary, List<KpiEvaluationResponse.Detail> details) {
}
