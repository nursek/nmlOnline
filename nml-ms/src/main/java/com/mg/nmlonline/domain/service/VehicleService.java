package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service pour l'achat et la gestion des véhicules.
 */
@Service
public class VehicleService {

    private final PlayerRepository playerRepository;
    private final VehicleRepository vehicleRepository;
    private final SectorRepository sectorRepository;

    public VehicleService(PlayerRepository playerRepository, VehicleRepository vehicleRepository,
                          SectorRepository sectorRepository) {
        this.playerRepository = playerRepository;
        this.vehicleRepository = vehicleRepository;
        this.sectorRepository = sectorRepository;
    }

    /**
     * Retourne tous les types de véhicules disponibles à l'achat.
     */
    public List<VehicleType> getAllVehicleTypes() {
        return Arrays.asList(VehicleType.values());
    }

    /**
     * Achète un ou plusieurs véhicules pour le joueur authentifié.
     * Déduit le coût total de son solde et crée les entités Vehicle.
     *
     * @param userId          l'id de l'utilisateur authentifié (extrait du JWT)
     * @param vehicleTypeName le nom de l'enum VehicleType
     * @param quantity        le nombre de véhicules à acheter (≥ 1)
     * @return la liste des vehicles créés
     */
    @Transactional
    public List<Vehicle> buyVehicle(Long userId, String vehicleTypeName, int quantity) {
        if (vehicleTypeName == null || vehicleTypeName.isBlank()) {
            throw new IllegalArgumentException("Le type de véhicule est requis");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantité doit être au moins 1");
        }
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(vehicleTypeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de véhicule invalide : " + vehicleTypeName);
        }

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        List<Vehicle> created = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            boolean success = player.buyVehicle(vehicleType);
            if (!success) {
                throw new IllegalStateException("Fonds insuffisants pour acheter ce véhicule (coût : " + vehicleType.getCost() + " ₡)");
            }
            Vehicle vehicle = new Vehicle(vehicleType, player.getId());
            created.add(vehicleRepository.save(vehicle));
        }
        playerRepository.save(player);
        return created;
    }

    /**
     * Retourne tous les véhicules appartenant au joueur (déployés et non-déployés).
     *
     * @param userId l'id de l'utilisateur authentifié (extrait du JWT)
     * @return la liste de ses véhicules
     */
    public List<Vehicle> getPlayerVehicles(Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));
        return vehicleRepository.findByPlayerId(player.getId());
    }

    /**
     * Déploie un véhicule sur un secteur possédé par le joueur.
     *
     * @param vehicleId    l'ID du véhicule
     * @param boardId      l'ID du board contenant le secteur cible
     * @param sectorNumber le numéro du secteur cible
     * @param userId       l'id de l'utilisateur authentifié (extrait du JWT)
     * @return le véhicule mis à jour
     */
    @Transactional
    public Vehicle placeVehicle(Long vehicleId, Long boardId, int sectorNumber, Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Véhicule introuvable : " + vehicleId));

        if (!player.getId().equals(vehicle.getPlayerId())) {
            throw new SecurityException("Ce véhicule ne vous appartient pas");
        }

        Sector sector = sectorRepository.findByBoard_IdAndNumber(boardId, sectorNumber)
                .orElseThrow(() -> new RuntimeException("Secteur introuvable"));

        if (!player.getId().equals(sector.getOwnerId())) {
            throw new SecurityException("Vous ne possédez pas ce secteur");
        }

        vehicle.setSector(sector);
        return vehicleRepository.save(vehicle);
    }
}
