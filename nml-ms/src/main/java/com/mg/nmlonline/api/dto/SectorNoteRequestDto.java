package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SectorNoteRequestDto {
    @Size(max = 500, message = "Note limitée à 500 caractères")
    private String note;
}
