package com.boxing.bracket.bout.admin.dto;

import java.util.List;
import java.util.stream.Collectors;

public class AdminBoutImportResponse {

    private final int importedCount;
    private final List<Long> boutIds;

    private AdminBoutImportResponse(int importedCount, List<Long> boutIds) {
        this.importedCount = importedCount;
        this.boutIds = boutIds;
    }

    public static AdminBoutImportResponse from(List<AdminBoutResponse> bouts) {
        List<Long> boutIds = bouts.stream()
                .map(AdminBoutResponse::getBoutId)
                .collect(Collectors.toList());
        return new AdminBoutImportResponse(bouts.size(), boutIds);
    }

    public int getImportedCount() {
        return importedCount;
    }

    public List<Long> getBoutIds() {
        return boutIds;
    }
}
