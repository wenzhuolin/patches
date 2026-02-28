package com.patches.plm.api.dto;

import java.util.List;

public record KpiEvaluationResponse(
        boolean pass,
        String summary,
        List<Detail> details
) {
    public record Detail(
            String ruleCode,
            String metricKey,
            Double metricValue,
            String threshold,
            boolean pass,
            String reason
    ) {
    }
}
