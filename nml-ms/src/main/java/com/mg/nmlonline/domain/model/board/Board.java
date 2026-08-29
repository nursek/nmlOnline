package com.mg.nmlonline.domain.model.board;

import com.mg.nmlonline.domain.model.sector.Sector;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Entity
@Table(name = "BOARDS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "board_seq")
    @SequenceGenerator(name = "board_seq", sequenceName = "boards_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Column(name = "svg_overlay_url")
    private String svgOverlayUrl;

    /** Tour courant — source unique de vérité du plateau (incrémenté par TurnService.advanceTurn). */
    @Column(name = "current_turn", nullable = false)
    private int currentTurn = 1;

    /** Refuse tout host/schéma externe : un admin compromis ne doit pas pouvoir faire charger du JS arbitraire via l'overlay SVG. */
    public void setSvgOverlayUrl(String svgOverlayUrl) {
        if (svgOverlayUrl != null && !isSameOriginPath(svgOverlayUrl)) {
            throw new IllegalArgumentException(
                    "svgOverlayUrl doit être un chemin relatif same-origin (commençant par '/' mais pas par '//') : "
                            + svgOverlayUrl);
        }
        this.svgOverlayUrl = svgOverlayUrl;
    }

    private static boolean isSameOriginPath(String url) {
        return url.startsWith("/") && !url.startsWith("//");
    }

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectorsList = new ArrayList<>();

    /** Map numéro→secteur reconstruite à la volée depuis sectorsList (pas de seconde source persistée). */
    private Map<Integer, Sector> sectorMap() {
        Map<Integer, Sector> map = new LinkedHashMap<>();
        if (sectorsList != null) {
            for (Sector sector : sectorsList) {
                map.put(sector.getNumber(), sector);
            }
        }
        return map;
    }

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

    public Sector getSector(int number) {
        if (sectorsList == null) return null;
        for (Sector sector : sectorsList) {
            if (sector.getNumber() == number) {
                return sector;
            }
        }
        return null;
    }

    public Collection<Sector> getAllSectors() {
        if (sectorsList == null) return Collections.emptyList();
        return Collections.unmodifiableList(sectorsList);
    }

    public int getSectorCount() {
        return sectorsList == null ? 0 : sectorsList.size();
    }

    public boolean hasSector(int number) {
        return getSector(number) != null;
    }

    public void removeSector(int number) {
        Sector removed = getSector(number);
        if (removed != null) {
            sectorsList.remove(removed);
            for (Sector s : sectorsList) {
                s.removeNeighbor(number);
            }
        }
    }

    public void assignOwner(int sectorNumber, Long playerId, String colorHex) {
        Sector sector = getSector(sectorNumber);
        if (sector == null) {
            throw new IllegalArgumentException("Sector " + sectorNumber + " does not exist");
        }
        sector.setOwnerAndColor(playerId, colorHex);
    }

    public List<Sector> getSectorsByOwner(Long playerId) {
        if (sectorsList == null) return Collections.emptyList();
        return sectorsList.stream()
                .filter(s -> s.isOwnedBy(playerId))
                .toList();
    }

    public List<Sector> getNeutralSectors() {
        if (sectorsList == null) return Collections.emptyList();
        return sectorsList.stream()
                .filter(Sector::isNeutral)
                .toList();
    }

    public boolean areNeighbors(int sector1, int sector2) {
        Sector s1 = getSector(sector1);
        return s1 != null && s1.isNeighbor(sector2);
    }

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

    public boolean isInvalidRoute(List<Integer> route) {
        if (route == null || route.size() < 2) return true;
        for (int i = 0; i < route.size() - 1; i++) {
            if (!areNeighbors(route.get(i), route.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

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

    public List<Integer> findRoute(int from, int to, int maxHops) {
        if (from == to) return List.of(from);
        if (!hasSector(from) || !hasSector(to)) return Collections.emptyList();

        Queue<List<Integer>> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(List.of(from));
        visited.add(from);

        while (!queue.isEmpty()) {
            List<Integer> path = queue.poll();
            if (path.size() > maxHops + 1) break; // +1 : le départ compte

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
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return String.format("Board{id=%d, name='%s', sectors=%d}", id, name, getSectorCount());
    }
}
