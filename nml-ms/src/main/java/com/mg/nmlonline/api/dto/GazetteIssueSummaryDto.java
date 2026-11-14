package com.mg.nmlonline.api.dto;

public record GazetteIssueSummaryDto(
        int turn,
        int issueNumber,
        String status,
        String mainHeadline,
        String publishedAt,
        String updatedAt,
        boolean finalEdition) {
}
