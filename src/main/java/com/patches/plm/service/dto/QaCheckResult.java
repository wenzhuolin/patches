package com.patches.plm.service.dto;

import java.util.List;

public record QaCheckResult(boolean pass, boolean rejected, List<String> reasons) {
}
