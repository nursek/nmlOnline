package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RankingCommentRequestDto {
    @Size(max = 500, message = "Commentaire limité à 500 caractères")
    private String comment;
}
