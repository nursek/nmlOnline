package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleReport;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BattleReportService {

    private final BattleReportRepository reportRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    public BattleReportService(BattleReportRepository reportRepository,
                               PlayerRepository playerRepository,
                               ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
    }

    public void saveDuelReport(int turn, int sectorNumber, List<Player> attackerCamp, List<Player> defenderCamp,
                               CombatService.SectorBattleResult result) {
        if (!result.success()) {
            return;
        }
        List<Player> participants = new ArrayList<>(attackerCamp);
        participants.addAll(defenderCamp);
        Map<Long, String> names = participants.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));

        BattleReportDto dto = new BattleReportDto();
        dto.setTurn(turn);
        dto.setSectorNumber(sectorNumber);
        dto.setStandoff(false);
        dto.setWinnerPlayerId(result.winner() != null ? result.winner().getId() : null);
        dto.setWinnerName(result.winner() != null ? result.winner().getName() : null);
        dto.setCapturedBuildings(result.capturedBuildings());

        boolean attackerEliminated = result.winner() != null && defenderCamp.stream()
                .anyMatch(player -> player.getId().equals(result.winner().getId()));
        boolean defenderEliminated = result.winner() != null && attackerCamp.stream()
                .anyMatch(player -> player.getId().equals(result.winner().getId()));
        List<BattleReportDto.CampDto> camps = new ArrayList<>();
        attackerCamp.forEach(player -> camps.add(
                camp(player, attackerEliminated, result.attackerCharacterLost())));
        defenderCamp.forEach(player -> camps.add(
                camp(player, defenderEliminated, result.defenderCharacterLost())));
        dto.setCamps(camps);

        dto.setCasualties(mapCasualties(result.casualtyDetails(), names));
        dto.setExperienceGains(mapExperienceGains(result.experienceGains(), names));
        persist(dto, participants.stream().map(Player::getId).toList());
    }

    public void saveStandoffReport(int turn, int sectorNumber, List<Player> participants,
                                   CombatService.StandoffBattleResult result) {
        if (!result.success()) {
            return;
        }
        Map<Long, String> names = participants.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));

        BattleReportDto dto = new BattleReportDto();
        dto.setTurn(turn);
        dto.setSectorNumber(sectorNumber);
        dto.setStandoff(true);
        dto.setWinnerPlayerId(result.winner() != null ? result.winner().getId() : null);
        dto.setWinnerName(result.winner() != null ? result.winner().getName() : null);
        dto.setCapturedBuildings(result.capturedBuildings());
        dto.setCamps(result.outcomes().stream()
                .map(outcome -> {
                    BattleReportDto.CampDto camp = new BattleReportDto.CampDto();
                    camp.setPlayerId(outcome.playerId());
                    camp.setPlayerName(names.get(outcome.playerId()));
                    camp.setEliminated(outcome.eliminated());
                    camp.setCharacterLost(outcome.characterLost());
                    return camp;
                })
                .toList());
        dto.setCasualties(mapCasualties(result.casualtyDetails(), names));
        dto.setExperienceGains(mapExperienceGains(result.experienceGains(), names));
        persist(dto, participants.stream().map(Player::getId).toList());
    }

    @Transactional(readOnly = true)
    public List<BattleReportDto> getReportsForUser(Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId));
        List<BattleReportDto> reports = new ArrayList<>();
        for (BattleReport report : reportRepository.findByParticipantId(player.getId())) {
            reports.add(readPayload(report));
        }
        return reports;
    }

    private BattleReportDto.CampDto camp(Player player, boolean eliminated, boolean characterLost) {
        BattleReportDto.CampDto camp = new BattleReportDto.CampDto();
        camp.setPlayerId(player.getId());
        camp.setPlayerName(player.getName());
        camp.setEliminated(eliminated);
        camp.setCharacterLost(characterLost);
        return camp;
    }

    private List<BattleReportDto.CasualtyDto> mapCasualties(List<CombatService.CasualtyInfo> casualties,
                                                            Map<Long, String> names) {
        return casualties.stream().map(casualty -> {
            BattleReportDto.CasualtyDto dto = new BattleReportDto.CasualtyDto();
            dto.setPlayerId(casualty.playerId());
            dto.setPlayerName(names.get(casualty.playerId()));
            dto.setLabel(casualty.label());
            dto.setCategory(casualty.category());
            dto.setUnitType(casualty.unitType() != null ? casualty.unitType().name() : null);
            dto.setUnitNumber(casualty.unitNumber());
            dto.setExperience(casualty.experience());
            return dto;
        }).toList();
    }

    private List<BattleReportDto.ExperienceGainDto> mapExperienceGains(List<CombatService.ExperienceGain> gains,
                                                                       Map<Long, String> names) {
        return gains.stream().map(gain -> {
            BattleReportDto.ExperienceGainDto dto = new BattleReportDto.ExperienceGainDto();
            dto.setPlayerId(gain.playerId());
            dto.setPlayerName(names.get(gain.playerId()));
            dto.setUnitNumber(gain.unitNumber());
            dto.setTypeBefore(gain.typeBefore().name());
            dto.setExperienceBefore(gain.experienceBefore());
            dto.setGained(gain.gained());
            dto.setTypeAfter(gain.typeAfter().name());
            dto.setExperienceAfter(gain.experienceAfter());
            return dto;
        }).toList();
    }

    private void persist(BattleReportDto dto, Collection<Long> participantIds) {
        BattleReport report = new BattleReport();
        report.setTurn(dto.getTurn());
        report.setSectorNumber(dto.getSectorNumber());
        report.setStandoff(dto.isStandoff());
        report.setWinnerPlayerId(dto.getWinnerPlayerId());
        report.setCapturedBuildings(dto.getCapturedBuildings());
        report.setPayload(writePayload(dto));
        report.getParticipantIds().addAll(participantIds);
        reportRepository.save(report);
    }

    private String writePayload(BattleReportDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation du rapport de combat impossible", e);
        }
    }

    private BattleReportDto readPayload(BattleReport report) {
        try {
            return objectMapper.readValue(report.getPayload(), BattleReportDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Rapport de combat " + report.getId() + " illisible", e);
        }
    }
}
