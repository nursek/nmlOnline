package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDetailDto;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleReport;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BattleReportService {

    private static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int MAX_REPORTS_PER_PLAYER = 100;

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

        BattleReportDetailDto dto = new BattleReportDetailDto();
        dto.setSchemaVersion(SCHEMA_VERSION);
        dto.setKind("ASSAUT");
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
        dto.setForces(result.forces());
        dto.setHighlights(result.highlights());
        persist(dto, participants.stream().map(Player::getId).toList());
    }

    public BattleReportDetailDto saveRangedReport(int turn, int sectorNumber, List<Player> participants,
                                                  List<CombatService.CasualtyInfo> casualties,
                                                  List<CombatService.ExperienceGain> experienceGains,
                                                  String rangedFire, Map<Long, Boolean> characterLost) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        Map<Long, String> names = participants.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
        BattleReportDetailDto dto = new BattleReportDetailDto();
        dto.setSchemaVersion(SCHEMA_VERSION);
        dto.setKind("TIR");
        dto.setTurn(turn);
        dto.setSectorNumber(sectorNumber);
        dto.setStandoff(false);
        dto.setRanged(true);
        dto.setRangedFire(rangedFire);
        dto.setWinnerPlayerId(null);
        dto.setWinnerName(null);
        dto.setCapturedBuildings(0);
        dto.setCamps(participants.stream()
                .map(player -> camp(player, false,
                        characterLost.getOrDefault(player.getId(), false)))
                .toList());
        dto.setCasualties(mapCasualties(casualties, names));
        dto.setExperienceGains(mapExperienceGains(experienceGains, names));
        persist(dto, participants.stream().map(Player::getId).toList());
        return dto;
    }

    public void saveStandoffReport(int turn, int sectorNumber, List<Player> participants,
                                   CombatService.StandoffBattleResult result) {
        if (!result.success()) {
            return;
        }
        Map<Long, String> names = participants.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
        Map<Long, Player> playersById = participants.stream()
                .collect(Collectors.toMap(Player::getId, player -> player, (a, b) -> a));

        BattleReportDetailDto dto = new BattleReportDetailDto();
        dto.setSchemaVersion(SCHEMA_VERSION);
        dto.setKind("IMPASSE");
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
                    Player player = playersById.get(outcome.playerId());
                    camp.setPlayerEliminated(player != null && player.isEliminated());
                    camp.setEliminationCause(player != null && player.getEliminationCause() != null
                            ? player.getEliminationCause().name() : null);
                    return camp;
                })
                .toList());
        dto.setCasualties(mapCasualties(result.casualtyDetails(), names));
        dto.setExperienceGains(mapExperienceGains(result.experienceGains(), names));
        dto.setForces(result.forces());
        dto.setHighlights(result.highlights());
        persist(dto, participants.stream().map(Player::getId).toList());
    }

    @Transactional(readOnly = true)
    public List<BattleReportDto> getReportsForUser(Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId));
        List<BattleReportDto> reports = new ArrayList<>();
        List<BattleReport> page = reportRepository.findByParticipantId(
                player.getId(), PageRequest.of(0, MAX_REPORTS_PER_PLAYER));
        for (BattleReport report : page) {
            reports.add(toPlayerReport(readPayload(report)));
        }
        return reports;
    }

    BattleReportDetailDto readPayload(BattleReport report) {
        try {
            BattleReportDetailDto detail =
                    objectMapper.readValue(report.getPayload(), BattleReportDetailDto.class);
            if (detail.getSchemaVersion() == 0) {
                return fromLegacy(objectMapper.readValue(report.getPayload(), BattleReportDto.class));
            }
            return detail;
        } catch (JacksonException e) {
            throw new IllegalStateException("Rapport de combat " + report.getId() + " illisible", e);
        }
    }

    BattleReportDto toPlayerReport(BattleReportDetailDto detail) {
        BattleReportDto dto = new BattleReportDto();
        dto.setTurn(detail.getTurn());
        dto.setSectorNumber(detail.getSectorNumber());
        dto.setKind(detail.getKind());
        dto.setStandoff(detail.isStandoff());
        dto.setRanged(detail.isRanged());
        dto.setRangedFire(detail.getRangedFire());
        dto.setWinnerPlayerId(detail.getWinnerPlayerId());
        dto.setWinnerName(detail.getWinnerName());
        dto.setCapturedBuildings(detail.getCapturedBuildings());
        dto.setCamps(detail.getCamps());
        dto.setCasualties(detail.getCasualties());
        dto.setExperienceGains(detail.getExperienceGains());
        dto.setForces(detail.getForces());
        dto.setHighlights(detail.getHighlights() == null ? List.of()
                : detail.getHighlights().stream()
                        .filter(highlight -> BattleHighlights.PUBLIC.equals(highlight.getVisibility()))
                        .toList());
        return dto;
    }

    private BattleReportDetailDto fromLegacy(BattleReportDto legacy) {
        BattleReportDetailDto detail = new BattleReportDetailDto();
        detail.setSchemaVersion(LEGACY_SCHEMA_VERSION);
        detail.setKind(legacy.isStandoff() ? "IMPASSE" : legacy.isRanged() ? "TIR" : "ASSAUT");
        detail.setTurn(legacy.getTurn());
        detail.setSectorNumber(legacy.getSectorNumber());
        detail.setStandoff(legacy.isStandoff());
        detail.setRanged(legacy.isRanged());
        detail.setRangedFire(legacy.getRangedFire());
        detail.setWinnerPlayerId(legacy.getWinnerPlayerId());
        detail.setWinnerName(legacy.getWinnerName());
        detail.setCapturedBuildings(legacy.getCapturedBuildings());
        detail.setCamps(legacy.getCamps());
        detail.setCasualties(legacy.getCasualties());
        detail.setExperienceGains(legacy.getExperienceGains());
        detail.setForces(legacy.getForces());
        detail.setHighlights(legacy.getHighlights());
        return detail;
    }

    private BattleReportDto.CampDto camp(Player player, boolean eliminated, boolean characterLost) {
        BattleReportDto.CampDto camp = new BattleReportDto.CampDto();
        camp.setPlayerId(player.getId());
        camp.setPlayerName(player.getName());
        camp.setEliminated(eliminated);
        camp.setCharacterLost(characterLost);
        camp.setPlayerEliminated(player.isEliminated());
        camp.setEliminationCause(player.getEliminationCause() != null
                ? player.getEliminationCause().name() : null);
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

    private void persist(BattleReportDetailDto dto, Collection<Long> participantIds) {
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

    private String writePayload(BattleReportDetailDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JacksonException e) {
            throw new IllegalStateException("Sérialisation du rapport de combat impossible", e);
        }
    }
}
