package com.mg.nmlonline.domain.model.board;

import com.mg.nmlonline.domain.model.sector.Sector;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Représente la carte complète du jeu - Entité JPA
 * Contient TOUS les secteurs (vides ou possédés par des joueurs).
 */
@Entity
@Table(name = "BOARDS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // URLs des assets de la carte (image JPG + overlay SVG)
    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Column(name = "svg_overlay_url")
    private String svgOverlayUrl;

    // Tous les secteurs de la carte (source unique de vérité)
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectorsList = new ArrayList<>();

    // === GESTION DES SECTEURS ===

    /**
     * Construit une map numéro -> secteur à partir de la liste persistante.
     * Utilisé en interne pour les accès rapides sans maintenir de double source.
     */
    private Map<Integer, Sector> sectorMap() {
        Map<Integer, Sector> map = new LinkedHashMap<>();
        if (sectorsList != null) {
            for (Sector sector : sectorsList) {
                map.put(sector.getNumber(), sector);
            }
        }
        return map;
    }

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
        if (sectorMap().containsKey(sector.getNumber())) {
            throw new IllegalStateException("Sector " + sector.getNumber() + " already exists");
        }
        sector.setBoard(this);
        sectorsList.add(sector);
    }

    /**
     * Récupère un secteur par son numéro.
     */
    public Sector getSector(int number) {
        if (sectorsList == null) return null;
        for (Sector sector : sectorsList) {
            if (sector.getNumber() == number) {
                return sector;
            }
        }
        return null;
    }

    /**
     * Retourne tous les secteurs de la carte.
     */
    public Collection<Sector> getAllSectors() {
        if (sectorsList == null) return Collections.emptyList();
        return Collections.unmodifiableList(sectorsList);
    }

    /**
     * Retourne le nombre total de secteurs.
     */
    public int getSectorCount() {
        return sectorsList == null ? 0 : sectorsList.size();
    }

    /**
     * Vérifie si un secteur existe.
     */
    public boolean hasSector(int number) {
        return getSector(number) != null;
    }

    /**
     * Supprime un secteur de la carte.
     */
    public void removeSector(int number) {
        Sector removed = getSector(number);
        if (removed != null) {
            sectorsList.remove(removed);
            // Nettoyer les références dans les voisins
            for (Sector s : sectorsList) {
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
        if (sectorsList == null) return Collections.emptyList();
        return sectorsList.stream()
                .filter(s -> s.isOwnedBy(playerId))
                .toList();
    }

    /**
     * Retourne tous les secteurs neutres (sans propriétaire).
     */
    public List<Sector> getNeutralSectors() {
        if (sectorsList == null) return Collections.emptyList();
        return sectorsList.stream()
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

    // === PATHFINDING ===

    /**
     * Valide qu'une route est un enchaînement contigu de secteurs voisins.
     * Ne vérifie PAS la propriété des secteurs.
     *
     * @param route Liste ordonnée de numéros de secteurs (départ inclus)
     * @return true si la route est invalide (null, trop courte ou secteurs non adjacents)
     */
    public boolean isInvalidRoute(List<Integer> route) {
        if (route == null || route.size() < 2) return true;
        for (int i = 0; i < route.size() - 1; i++) {
            if (!areNeighbors(route.get(i), route.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valide qu'une route ne traverse que des secteurs alliés.
     * Le secteur de destination doit aussi être allié.
     *
     * @param route Liste ordonnée de numéros de secteurs
     * @param ownerId ID du joueur propriétaire
     * @return true si la route est valide et tous les secteurs sont alliés
     */
    public boolean isAlliedRoute(List<Integer> route, Long ownerId) {
        if (isInvalidRoute(route)) return false;
        for (int sectorNumber : route) {
            Sector sector = getSector(sectorNumber);
            if (sector == null || !sector.isOwnedBy(ownerId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recherche un chemin le plus court entre deux secteurs via BFS.
     * Retourne une liste vide si aucun chemin n'existe dans la limite de hops.
     *
     * @param from Numéro du secteur de départ
     * @param to Numéro du secteur d'arrivée
     * @param maxHops Nombre maximum de sauts autorisés
     * @return La route (liste de secteurs de départ à arrivée) ou liste vide si aucun chemin trouvé
     */
    public List<Integer> findRoute(int from, int to, int maxHops) {
        if (from == to) return List.of(from);
        if (!hasSector(from) || !hasSector(to)) return Collections.emptyList();

        // BFS
        Queue<List<Integer>> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(List.of(from));
        visited.add(from);

        while (!queue.isEmpty()) {
            List<Integer> path = queue.poll();
            if (path.size() > maxHops + 1) break; // +1, car le départ compte

            int current = path.getLast();
            Sector currentSector = getSector(current);
            if (currentSector == null) continue;

            for (int neighbor : currentSector.getNeighbors()) {
                if (neighbor == to) {
                    List<Integer> fullPath = new ArrayList<>(path);
                    fullPath.add(to);
                    return fullPath;
                }
                if (!visited.contains(neighbor) && path.size() < maxHops + 1) {
                    visited.add(neighbor);
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        return Collections.emptyList(); // Aucun chemin trouvé dans la limite de hops
    }

    @Override
    public String toString() {
        return String.format("Board{id=%d, name='%s', sectors=%d}", id, name, getSectorCount());
    }
}
