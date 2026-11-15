package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GazetteContentRequestDto {

    private List<GazetteIssueDto.ArticleDto> frontPage;
    private List<GazetteIssueDto.ArticleDto> briefs;
}
