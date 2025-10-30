package com.mg.nmlonline.domain.model.board;

import com.mg.nmlonline.domain.model.sector.Sector;

import java.util.*;

/**
 * Représente la carte complète du jeu.
 * Contient TOUS les secteurs (vides ou possédés par des joueurs).
 * Les secteurs sont la source de vérité unique (stockés en BDD).
 * Board gère l'organisation spatiale et les relations entre secteurs.
 */
public class Board {

    // Map de tous les secteurs : numéro → Sector
    // Les secteurs commencent au numéro 1, pas de doublon possible
    private final Map<Integer, Sector> sectors;

    public Board() {
        this.sectors = new LinkedHashMap<>();
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
        sectors.put(sector.getNumber(), sector);
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
     * Utilisé pour déterminer si une bataille doit avoir lieu.
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
        // Conflit si les deux secteurs ont des propriétaires différents (non nulls)
        return s1.getOwnerId() != null
                && s2.getOwnerId() != null
                && !s1.getOwnerId().equals(s2.getOwnerId());
    }

    @Override
    public String toString() {
        return String.format("Board{sectors=%d}", sectors.size());
    }
}
