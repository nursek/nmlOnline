package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class GazetteAdminIssueDto {

    private GazetteIssueDto issue;
    private String status;
    private GazetteEditorNotesDto editorNotes;
    private String mainHeadline;
    private String publishedAt;
    private String updatedAt;
}
