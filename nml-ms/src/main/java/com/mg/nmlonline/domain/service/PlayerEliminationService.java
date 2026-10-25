package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.alliance.AllianceProposal;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.alliance.AnnouncementType;
import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import com.mg.nmlonline.domain.model.alliance.ProposalStatus;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.AllianceProposalRepository;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Conséquences matérielles d'une élimination, appliquées à la finalisation du tour. */
@Service
@Transactional
public class PlayerEliminationService {

    private final EntityManager entityManager;
    private final PlayerRepository playerRepository;
    private final AllianceRepository allianceRepository;
    private final AllianceProposalRepository proposalRepository;
    private final AnnouncementRepository announcementRepository;
    private final ExchangeOfferRepository exchangeOfferRepository;
    private final PendingCaptureRepository pendingCaptureRepository;
    private final BuildingService buildingService;

    public PlayerEliminationService(EntityManager entityManager,
                                    PlayerRepository playerRepository,
                                    AllianceRepository allianceRepository,
                                    AllianceProposalRepository proposalRepository,
                                    AnnouncementRepository announcementRepository,
                                    ExchangeOfferRepository exchangeOfferRepository,
                                    PendingCaptureRepository pendingCaptureRepository,
                                    BuildingService buildingService) {
        this.entityManager = entityManager;
        this.playerRepository = playerRepository;
        this.allianceRepository = allianceRepository;
        this.proposalRepository = proposalRepository;
        this.announcementRepository = announcementRepository;
        this.exchangeOfferRepository = exchangeOfferRepository;
        this.pendingCaptureRepository = pendingCaptureRepository;
        this.buildingService = buildingService;
    }

    public void apply(Board board, Player victim, Long killerId, int turn) {
        Player killer = killerId != null ? playerRepository.findById(killerId).orElse(null) : null;
        String killerColor = killer != null ? playerColor(board, killerId) : "#ffffff";
        boolean headquartersCaptured = victim.getEliminationCause() == EliminationCause.HEADQUARTERS_CAPTURED;
        Integer headquartersSector = headquartersSector(victim);
        Set<Integer> affectedSectors = new HashSet<>();
        List<Sector> ownedSectors = new ArrayList<>(board.getSectorsByOwner(victim.getId()));

        if (headquartersCaptured && killer != null && headquartersSector != null) {
            lootHeadquartersSector(victim, headquartersSector, killerId, turn);
        }
        for (Building building : new ArrayList<>(victim.getBuildings())) {
            Sector sector = building.getSector();
            if (sector != null) {
                sector.getBuildings().remove(building);
                affectedSectors.add(sector.getNumber());
            }
            victim.getBuildings().remove(building);
        }

        deleteUnitsAndCharacters(victim);
        deleteVehicles(victim.getId());

        victim.getEquipments().clear();
        victim.getResources().clear();
        victim.getStats().setMoney(0);
        victim.getStats().setStartingMoneyRemaining(0);
        victim.getStats().setTotalEquipmentValue(0);
        victim.getStats().setTotalVehiclesValue(0);
        victim.calculateTotalEconomyPower();

        for (Sector sector : ownedSectors) {
            boolean goesToKiller = killer != null
                    && (headquartersCaptured
                        || (headquartersSector != null && sector.getNumber() == headquartersSector));
            if (goesToKiller) {
                sector.setOwnerAndColor(killerId, killerColor);
            } else {
                sector.setOwnerAndColor(null, "#ffffff");
            }
            affectedSectors.add(sector.getNumber());
        }

        // Mort du personnage : le secteur du QG revient nu au tueur, même s'il n'était pas possédé par la victime.
        if (!headquartersCaptured && headquartersSector != null) {
            Sector headquarters = board.getSector(headquartersSector);
            if (headquarters != null) {
                if (killer != null) {
                    headquarters.setOwnerAndColor(killerId, killerColor);
                } else {
                    headquarters.setOwnerAndColor(null, "#ffffff");
                }
                affectedSectors.add(headquartersSector);
            }
        }

        breakAlliances(victim, turn);
        cancelPendingProposals(victim, turn);
        cancelPendingExchanges(victim, turn);
        deletePendingCaptures(victim.getId());

        for (Integer sectorNumber : affectedSectors) {
            Sector sector = board.getSector(sectorNumber);
            if (sector != null) {
                sector.recalculateMilitaryPower();
            }
        }

        victim.setEliminatedByPlayerId(killerId);
        playerRepository.save(victim);
    }

    private void lootHeadquartersSector(Player victim, int sectorNumber, Long killerId, int turn) {
        for (Building building : new ArrayList<>(victim.getBuildings())) {
            if (building.isDestroyed() || building.isCaptured()
                    || building.getSector() == null
                    || building.getSector().getNumber() != sectorNumber) {
                continue;
            }
            if (building instanceof WeaponCache cache) {
                buildingService.captureWeaponCache(cache.getId(), killerId, turn);
            } else if (building instanceof Bank bank) {
                buildingService.captureBank(bank.getId(), killerId, turn);
            }
        }
    }

    private Integer headquartersSector(Player victim) {
        return victim.getBuildings().stream()
                .filter(Headquarters.class::isInstance)
                .map(Building::getSector)
                .filter(Objects::nonNull)
                .map(Sector::getNumber)
                .findFirst()
                .orElse(null);
    }

    private void deleteUnitsAndCharacters(Player victim) {
        List<CombatEntity> entities = entityManager.createQuery(
                        "select e from CombatEntity e where e.playerId = :playerId", CombatEntity.class)
                .setParameter("playerId", victim.getId())
                .getResultList();
        for (CombatEntity entity : entities) {
            if (entity instanceof Building) {
                continue;
            }
            if (entity instanceof Unit unit) {
                detachFromVehicles(unit);
                Sector sector = unit.getSector();
                if (sector != null) {
                    sector.getUnits().remove(unit);
                }
                entityManager.remove(unit);
            } else if (entity instanceof GameCharacter character) {
                detachFromVehicles(character);
                Sector sector = character.getSector();
                if (sector != null) {
                    sector.getCharacters().remove(character);
                }
                if (victim.getCharacter() != null && character.getId().equals(victim.getCharacter().getId())) {
                    victim.setCharacter(null); // orphanRemoval → DELETE au flush
                } else {
                    entityManager.remove(character);
                }
            }
        }
    }

    private void deleteVehicles(Long playerId) {
        List<Vehicle> vehicles = entityManager.createQuery(
                        "select v from Vehicle v where v.playerId = :playerId", Vehicle.class)
                .setParameter("playerId", playerId)
                .getResultList();
        for (Vehicle vehicle : vehicles) {
            vehicle.disembarkAll();
            Sector sector = vehicle.getSector();
            if (sector != null) {
                sector.getVehicles().remove(vehicle);
            }
            entityManager.remove(vehicle);
        }
    }

    /** Un pilote ou passager supprimé doit être détaché avant le DELETE (FK pilot_id / table de passagers). */
    private void detachFromVehicles(CombatEntity entity) {
        List<Vehicle> vehicles = entityManager.createQuery(
                        "select distinct v from Vehicle v left join v.passengers p "
                                + "where v.pilot.id = :entityId or p.id = :entityId", Vehicle.class)
                .setParameter("entityId", entity.getId())
                .getResultList();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getPilot() != null && entity.getId().equals(vehicle.getPilot().getId())) {
                vehicle.removePilot();
            }
            vehicle.disembark(entity);
        }
    }

    private void breakAlliances(Player victim, int turn) {
        for (Alliance alliance : allianceRepository.findAllActive()) {
            if (!alliance.involves(victim.getId())) {
                continue;
            }
            Long allyId = alliance.other(victim.getId());
            alliance.end(turn, victim.getId(), false);
            allianceRepository.save(alliance);
            announcementRepository.save(Announcement.create(
                    AnnouncementType.ALLIANCE_BROKEN, victim.getId(), allyId, turn));
        }
    }

    private void cancelPendingProposals(Player victim, int turn) {
        List<AllianceProposal> pending = new ArrayList<>();
        pending.addAll(proposalRepository.findByFromPlayerIdAndStatus(victim.getId(), ProposalStatus.PENDING));
        pending.addAll(proposalRepository.findByToPlayerIdAndStatus(victim.getId(), ProposalStatus.PENDING));
        pending.forEach(proposal -> proposal.withdraw(turn));
        proposalRepository.saveAll(pending);
    }

    private void cancelPendingExchanges(Player victim, int turn) {
        List<ExchangeOffer> offers = exchangeOfferRepository
                .findBySenderPlayerIdOrReceiverPlayerIdOrderByCreatedAtDesc(victim.getId(), victim.getId());
        List<ExchangeOffer> pending = offers.stream().filter(ExchangeOffer::isPending).toList();
        pending.forEach(offer -> offer.cancel(turn));
        exchangeOfferRepository.saveAll(pending);
    }

    private void deletePendingCaptures(Long victimId) {
        for (PendingCapture capture : pendingCaptureRepository.findByResolvedFalseOrderByIdAsc()) {
            if (capture.candidateIds().contains(victimId)) {
                pendingCaptureRepository.delete(capture);
            }
        }
    }

    private String playerColor(Board board, Long playerId) {
        return board.getSectorsByOwner(playerId).stream()
                .map(Sector::getColor)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("#ffffff");
    }
}
