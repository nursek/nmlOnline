package com.mg.nmlonline.domain.model.board;

import com.mg.nmlonline.domain.model.sector.Sector;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Représente la carte complète du jeu - Entité JPA
 * Contient TOUS les secteurs (vides ou possédés par des joueurs).
 */
@Entity
@Table(name = "BOARDS")
@Data
@NoArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // URLs des assets de la carte (image JPG + overlay SVG)
    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Column(name = "svg_overlay_url")
    private String svgOverlayUrl;

    // Tous les secteurs de la carte
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectorsList = new ArrayList<>();

    // Map transient pour accès rapide par numéro (utilisé par le code métier)
    @Transient
    private Map<Integer, Sector> sectors = new LinkedHashMap<>();

    /**
     * Initialise la map des secteurs à partir de la liste (après chargement JPA)
     */
    @PostLoad
    public void initSectorsMap() {
        sectors = new LinkedHashMap<>();
        if (sectorsList != null) {
            for (Sector sector : sectorsList) {
                sectors.put(sector.getNumber(), sector);
            }
        }
    }

    // === GESTION DES SECTEURS ===

    /**
     * Ajoute un secteur à la carte. Le numéro du secteur doit être unique.
     */
    public void addSector(Sector sector) {
        if (sector == null) {
            throw new IllegalArgumentException("Sector cannot be null");
        }
        if (sector.getNumber() < 1) {
            throw new IllegalArgumentException("Sector number must be >= 1");
        }
        if (sectors.containsKey(sector.getNumber())) {
            throw new IllegalStateException("Sector " + sector.getNumber() + " already exists");
        }
        sector.setBoard(this);
        sectors.put(sector.getNumber(), sector);
        sectorsList.add(sector);
    }

    /**
     * Récupère un secteur par son numéro.
     */
    public Sector getSector(int number) {
        return sectors.get(number);
    }

    /**
     * Retourne tous les secteurs de la carte.
     */
    public Collection<Sector> getAllSectors() {
        return Collections.unmodifiableCollection(sectors.values());
    }

    /**
     * Retourne le nombre total de secteurs.
     */
    public int getSectorCount() {
        return sectors.size();
    }

    /**
     * Vérifie si un secteur existe.
     */
    public boolean hasSector(int number) {
        return sectors.containsKey(number);
    }

    /**
     * Supprime un secteur de la carte.
     */
    public void removeSector(int number) {
        Sector removed = sectors.remove(number);
        if (removed != null) {
            sectorsList.remove(removed);
            // Nettoyer les références dans les voisins
            for (Sector s : sectors.values()) {
                s.removeNeighbor(number);
            }
        }
    }

    // === GESTION DES PROPRIÉTAIRES ===

    /**
     * Assigne un propriétaire à un secteur et met à jour sa couleur.
     */
    public void assignOwner(int sectorNumber, Long playerId, String colorHex) {
        Sector sector = getSector(sectorNumber);
        if (sector == null) {
            throw new IllegalArgumentException("Sector " + sectorNumber + " does not exist");
        }
        sector.setOwnerAndColor(playerId, colorHex);
    }

    /**
     * Retourne tous les secteurs possédés par un joueur.
     */
    public List<Sector> getSectorsByOwner(Long playerId) {
        return sectors.values().stream()
                .filter(s -> s.isOwnedBy(playerId))
                .toList();
    }

    /**
     * Retourne tous les secteurs neutres (sans propriétaire).
     */
    public List<Sector> getNeutralSectors() {
        return sectors.values().stream()
                .filter(Sector::isNeutral)
                .toList();
    }

    /**
     * Vérifie si deux secteurs sont voisins (utile pour valider les déplacements).
     */
    public boolean areNeighbors(int sector1, int sector2) {
        Sector s1 = getSector(sector1);
        return s1 != null && s1.isNeighbor(sector2);
    }

    /**
     * Vérifie s'il y a conflit entre deux secteurs (propriétaires différents et voisins).
     */
    public boolean hasConflict(int sector1, int sector2) {
        if (!areNeighbors(sector1, sector2)) {
            return false;
        }
        Sector s1 = getSector(sector1);
        Sector s2 = getSector(sector2);
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.getOwnerId() != null
                && s2.getOwnerId() != null
                && !s1.getOwnerId().equals(s2.getOwnerId());
    }

    @Override
    public String toString() {
        return String.format("Board{id=%d, name='%s', sectors=%d}", id, name, sectors.size());
    }
}
