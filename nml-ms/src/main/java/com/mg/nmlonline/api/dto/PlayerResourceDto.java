package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * DTO pour représenter une ressource possédée par un joueur
 * Le prix de base est récupéré depuis Resource côté service
 */
@Data
public class PlayerResourceDto {
    private String name;
    private int quantity;
    private Double baseValue;
}
