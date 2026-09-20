package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AllianceMeDto;
import com.mg.nmlonline.api.dto.AllianceMessageDto;
import com.mg.nmlonline.api.dto.AllianceProposalDto;
import com.mg.nmlonline.api.dto.AnnouncementDto;
import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.alliance.AllianceMessage;
import com.mg.nmlonline.domain.model.alliance.AllianceProposal;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.alliance.AnnouncementType;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.alliance.ProposalStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.AllianceMessageRepository;
import com.mg.nmlonline.infrastructure.repository.AllianceProposalRepository;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AllianceService {

    private static final int MESSAGE_PAGE_SIZE = 200;
    private static final int ANNOUNCEMENT_PAGE_SIZE = 50;

    private final AllianceRepository allianceRepository;
    private final AllianceProposalRepository proposalRepository;
    private final AllianceMessageRepository messageRepository;
    private final AnnouncementRepository announcementRepository;
    private final PlayerRepository playerRepository;
    private final BuildingService buildingService;
    private final TurnService turnService;
    private final AllianceGraph allianceGraph;

    public AllianceService(AllianceRepository allianceRepository,
                           AllianceProposalRepository proposalRepository,
                           AllianceMessageRepository messageRepository,
                           AnnouncementRepository announcementRepository,
                           PlayerRepository playerRepository,
                           BuildingService buildingService,
                           TurnService turnService,
                           AllianceGraph allianceGraph) {
        this.allianceRepository = allianceRepository;
        this.proposalRepository = proposalRepository;
        this.messageRepository = messageRepository;
        this.announcementRepository = announcementRepository;
        this.playerRepository = playerRepository;
        this.buildingService = buildingService;
        this.turnService = turnService;
        this.allianceGraph = allianceGraph;
    }

    @Transactional(readOnly = true)
    public AllianceMeDto getMe(Long userId) {
        Player player = requirePlayer(userId);
        int turn = turnService.getCurrentTurn();

        List<Alliance> active = allianceRepository.findActiveForPlayer(player.getId());
        List<AllianceProposal> incoming =
                proposalRepository.findByToPlayerIdAndStatus(player.getId(), ProposalStatus.PENDING);
        List<AllianceProposal> outgoing =
                proposalRepository.findByFromPlayerIdAndStatus(player.getId(), ProposalStatus.PENDING);

        Set<Long> otherIds = new HashSet<>();
        active.forEach(alliance -> otherIds.add(alliance.other(player.getId())));
        incoming.forEach(proposal -> otherIds.add(proposal.getFromPlayerId()));
        outgoing.forEach(proposal -> otherIds.add(proposal.getToPlayerId()));
        allianceRepository.findBetrayalsAtTurn(turn).stream()
                .filter(alliance -> player.getId().equals(alliance.getEndedByPlayerId()))
                .forEach(alliance -> otherIds.add(alliance.other(player.getId())));
        Map<Long, String> names = namesById(otherIds);

        AllianceMeDto dto = new AllianceMeDto();
        dto.setHeadquartersOperational(buildingService.hasOperationalHeadquarters(player.getId()));
        for (Alliance alliance : active) {
            AllianceMeDto.AllianceDto allianceDto = new AllianceMeDto.AllianceDto();
            allianceDto.setId(alliance.getId());
            Long allyId = alliance.other(player.getId());
            allianceDto.setAllyPlayerId(allyId);
            allianceDto.setAllyName(names.get(allyId));
            allianceDto.setCreatedTurn(alliance.getCreatedTurn());
            dto.getAlliances().add(allianceDto);

            AllianceMeDto.AlliedPlayerDto ally = new AllianceMeDto.AlliedPlayerDto();
            ally.setPlayerId(allyId);
            ally.setName(names.get(allyId));
            dto.getAlliedPlayers().add(ally);
        }
        incoming.forEach(proposal -> dto.getIncomingProposals().add(toProposalDto(proposal, names, "IN")));
        outgoing.forEach(proposal -> dto.getOutgoingProposals().add(toProposalDto(proposal, names, "OUT")));
        for (Alliance betrayal : allianceRepository.findBetrayalsAtTurn(turn)) {
            if (!player.getId().equals(betrayal.getEndedByPlayerId())) {
                continue;
            }
            Long victimId = betrayal.other(player.getId());
            AllianceMeDto.BetrayalBonusDto bonus = new AllianceMeDto.BetrayalBonusDto();
            bonus.setVictimPlayerId(victimId);
            bonus.setVictimName(names.get(victimId));
            bonus.setBonusPercent(15 + 10.0 * betrayal.durationTurns());
            dto.getActiveBetrayalBonuses().add(bonus);
        }
        return dto;
    }

    public AllianceProposalDto propose(Long userId, ProposalKind kind, Long targetPlayerId) {
        Player player = requirePlayer(userId);
        if (targetPlayerId == null || targetPlayerId.equals(player.getId())) {
            throw new IllegalArgumentException("Cible invalide pour la proposition.");
        }
        Player target = playerRepository.findById(targetPlayerId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur cible introuvable : " + targetPlayerId));
        requireHeadquarters(player);
        int turn = turnService.getCurrentTurn();

        if (kind == ProposalKind.ALLIANCE) {
            if (allianceRepository.findActiveBetween(player.getId(), target.getId()).isPresent()) {
                throw new IllegalStateException("Vous êtes déjà allié avec " + target.getName() + ".");
            }
        } else {
            if (allianceRepository.findActiveBetween(player.getId(), target.getId()).isEmpty()) {
                throw new IllegalStateException("Aucune alliance active avec " + target.getName() + ".");
            }
        }
        if (!proposalRepository.findByFromPlayerIdAndToPlayerIdAndKindAndStatus(
                player.getId(), target.getId(), kind, ProposalStatus.PENDING).isEmpty()) {
            throw new IllegalStateException("Une proposition identique est déjà en attente.");
        }

        AllianceProposal proposal;
        try {
            proposal = proposalRepository.saveAndFlush(
                    AllianceProposal.create(kind, player.getId(), target.getId(), turn));
        } catch (DataIntegrityViolationException e) {
            // Double-clic simultané : l'index unique partiel (prod) rejette le doublon au flush.
            throw new IllegalStateException("Une proposition identique est déjà en attente.");
        }
        return toProposalDto(proposal, Map.of(player.getId(), player.getName(), target.getId(), target.getName()), "OUT");
    }

    public void accept(Long userId, Long proposalId) {
        Player player = requirePlayer(userId);
        AllianceProposal proposal = requirePendingProposal(proposalId);
        if (!proposal.getToPlayerId().equals(player.getId())) {
            throw new SecurityException("Cette proposition ne vous est pas destinée.");
        }
        lockPlayersAscending(proposal.getFromPlayerId(), proposal.getToPlayerId());
        int turn = turnService.getCurrentTurn();

        if (proposal.getKind() == ProposalKind.ALLIANCE) {
            requireHeadquarters(player);
            Alliance existing = allianceRepository.findActiveBetween(
                    proposal.getFromPlayerId(), proposal.getToPlayerId()).orElse(null);
            if (existing != null) {
                proposal.decline(turn);
                proposalRepository.save(proposal);
                throw new IllegalStateException("Une alliance est déjà active entre ces joueurs.");
            }
            allianceRepository.save(Alliance.create(
                    proposal.getFromPlayerId(), proposal.getToPlayerId(), turn));
            announcementRepository.save(Announcement.create(AnnouncementType.ALLIANCE_FORMED,
                    proposal.getFromPlayerId(), proposal.getToPlayerId(), turn));
            cancelPendingAllianceProposals(proposal.getFromPlayerId(), proposal.getToPlayerId(),
                    proposal.getId(), turn);
        } else {
            requireHeadquarters(player);
            Alliance alliance = allianceRepository.findActiveBetween(
                            proposal.getFromPlayerId(), proposal.getToPlayerId())
                    .orElseThrow(() -> new IllegalStateException("L'alliance n'est plus active."));
            alliance.end(turn, proposal.getFromPlayerId(), false);
            allianceRepository.save(alliance);
            announcementRepository.save(Announcement.create(AnnouncementType.ALLIANCE_BROKEN,
                    proposal.getFromPlayerId(), proposal.getToPlayerId(), turn));
        }
        proposal.accept(turn);
        proposalRepository.save(proposal);
    }

    public void decline(Long userId, Long proposalId) {
        Player player = requirePlayer(userId);
        AllianceProposal proposal = requirePendingProposal(proposalId);
        if (!proposal.getToPlayerId().equals(player.getId())) {
            throw new SecurityException("Cette proposition ne vous est pas destinée.");
        }
        proposal.decline(turnService.getCurrentTurn());
        proposalRepository.save(proposal);
    }

    public void withdraw(Long userId, Long proposalId) {
        Player player = requirePlayer(userId);
        AllianceProposal proposal = requirePendingProposal(proposalId);
        if (!proposal.getFromPlayerId().equals(player.getId())) {
            throw new SecurityException("Vous n'êtes pas l'auteur de cette proposition.");
        }
        proposal.withdraw(turnService.getCurrentTurn());
        proposalRepository.save(proposal);
    }

    /** Trahison : rupture unilatérale, possible sans QG (sinon un QG détruit piégerait le joueur dans l'alliance). */
    public void betray(Long userId, Long allianceId) {
        Player player = requirePlayer(userId);
        Alliance alliance = allianceRepository.findById(allianceId)
                .orElseThrow(() -> new EntityNotFoundException("Alliance introuvable : " + allianceId));
        if (!alliance.involves(player.getId())) {
            throw new SecurityException("Vous ne faites pas partie de cette alliance.");
        }
        if (!alliance.isActive()) {
            throw new IllegalStateException("Cette alliance est déjà terminée.");
        }
        // Verrous joueurs avant la ligne alliance : même ordre que accept(), sinon deadlock au commit.
        lockPlayersAscending(alliance.getPlayerOneId(), alliance.getPlayerTwoId());
        int turn = turnService.getCurrentTurn();
        if (allianceRepository.endAsBetrayal(allianceId, turn, player.getId()) == 0) {
            throw new IllegalStateException("Cette alliance est déjà terminée.");
        }
        announcementRepository.save(Announcement.create(AnnouncementType.BETRAYAL,
                player.getId(), alliance.other(player.getId()), turn));
        cancelPendingProposalsFor(player.getId(), turn);
    }

    @Transactional(readOnly = true)
    public List<AllianceMessageDto> getMessages(Long userId, Long allianceId) {
        Player player = requirePlayer(userId);
        Alliance alliance = requireActiveMember(player.getId(), allianceId);
        Map<Long, String> names = namesById(Set.of(alliance.getPlayerOneId(), alliance.getPlayerTwoId()));
        List<AllianceMessage> messages = new ArrayList<>(messageRepository.findByAllianceIdOrderByIdDesc(
                allianceId, PageRequest.of(0, MESSAGE_PAGE_SIZE)));
        List<AllianceMessageDto> dtos = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AllianceMessage message = messages.get(i);
            AllianceMessageDto dto = new AllianceMessageDto();
            dto.setId(message.getId());
            dto.setSenderPlayerId(message.getSenderPlayerId());
            dto.setSenderName(names.get(message.getSenderPlayerId()));
            dto.setBody(message.getBody());
            dto.setTurn(message.getTurn());
            dto.setCreatedAt(message.getCreatedAt());
            dtos.add(dto);
        }
        return dtos;
    }

    public AllianceMessageDto postMessage(Long userId, Long allianceId, String body) {
        Player player = requirePlayer(userId);
        requireActiveMember(player.getId(), allianceId);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Le message est vide.");
        }
        if (body.length() > 1000) {
            throw new IllegalArgumentException("Le message dépasse 1000 caractères.");
        }
        AllianceMessage message = messageRepository.save(AllianceMessage.create(
                allianceId, player.getId(), body.trim(), turnService.getCurrentTurn()));
        AllianceMessageDto dto = new AllianceMessageDto();
        dto.setId(message.getId());
        dto.setSenderPlayerId(player.getId());
        dto.setSenderName(player.getName());
        dto.setBody(message.getBody());
        dto.setTurn(message.getTurn());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements() {
        int turn = turnService.getCurrentTurn();
        List<Announcement> announcements = announcementRepository
                .findByVisibleAtTurnLessThanEqualOrderByIdDesc(turn, PageRequest.of(0, ANNOUNCEMENT_PAGE_SIZE));
        Set<Long> ids = new HashSet<>();
        announcements.forEach(announcement -> {
            ids.add(announcement.getActorPlayerId());
            if (announcement.getTargetPlayerId() != null) {
                ids.add(announcement.getTargetPlayerId());
            }
        });
        Map<Long, String> names = namesById(ids);
        return announcements.stream().map(announcement -> {
            AnnouncementDto dto = new AnnouncementDto();
            dto.setId(announcement.getId());
            dto.setType(announcement.getType().name());
            dto.setActorPlayerId(announcement.getActorPlayerId());
            dto.setActorName(names.get(announcement.getActorPlayerId()));
            dto.setTargetPlayerId(announcement.getTargetPlayerId());
            dto.setTargetName(announcement.getTargetPlayerId() != null
                    ? names.get(announcement.getTargetPlayerId()) : null);
            dto.setTurnCreated(announcement.getTurnCreated());
            dto.setVisibleAtTurn(announcement.getVisibleAtTurn());
            return dto;
        }).toList();
    }

    private void cancelPendingAllianceProposals(Long firstId, Long secondId, Long acceptedProposalId, int turn) {
        List<AllianceProposal> pending = new ArrayList<>();
        pending.addAll(proposalRepository.findByFromPlayerIdAndToPlayerIdAndKindAndStatus(
                firstId, secondId, ProposalKind.ALLIANCE, ProposalStatus.PENDING));
        pending.addAll(proposalRepository.findByFromPlayerIdAndToPlayerIdAndKindAndStatus(
                secondId, firstId, ProposalKind.ALLIANCE, ProposalStatus.PENDING));
        for (AllianceProposal proposal : pending) {
            if (proposal.getId().equals(acceptedProposalId)) {
                continue;
            }
            proposal.withdraw(turn);
        }
        proposalRepository.saveAll(pending);
    }

    private void cancelPendingProposalsFor(Long playerId, int turn) {
        List<AllianceProposal> pending = new ArrayList<>();
        pending.addAll(proposalRepository.findByFromPlayerIdAndStatus(playerId, ProposalStatus.PENDING));
        pending.addAll(proposalRepository.findByToPlayerIdAndStatus(playerId, ProposalStatus.PENDING));
        for (AllianceProposal proposal : pending) {
            proposal.withdraw(turn);
        }
        proposalRepository.saveAll(pending);
    }

    private Alliance requireActiveMember(Long playerId, Long allianceId) {
        Alliance alliance = allianceRepository.findById(allianceId)
                .orElseThrow(() -> new EntityNotFoundException("Alliance introuvable : " + allianceId));
        if (!alliance.involves(playerId)) {
            throw new SecurityException("Vous ne faites pas partie de cette alliance.");
        }
        if (!alliance.isActive()) {
            throw new IllegalStateException("Cette alliance est terminée.");
        }
        return alliance;
    }

    private AllianceProposal requirePendingProposal(Long proposalId) {
        AllianceProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposition introuvable : " + proposalId));
        if (!proposal.isPending()) {
            throw new IllegalStateException("Cette proposition est déjà résolue (" + proposal.getStatus() + ").");
        }
        return proposal;
    }

    private Player requirePlayer(Long userId) {
        if (userId == null) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        return playerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun joueur pour l'utilisateur " + userId));
    }

    private void requireHeadquarters(Player player) {
        if (!buildingService.hasOperationalHeadquarters(player.getId())) {
            throw new IllegalStateException("Un quartier général opérationnel est requis pour gérer une alliance.");
        }
    }

    /** Lock pessimiste ordonné par id : évite le deadlock entre deux transactions croisées. */
    private void lockPlayersAscending(Long firstId, Long secondId) {
        long min = Math.min(firstId, secondId);
        long max = Math.max(firstId, secondId);
        playerRepository.findByIdForUpdate(min)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable : " + min));
        playerRepository.findByIdForUpdate(max)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable : " + max));
    }

    private Map<Long, String> namesById(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
    }

    private AllianceProposalDto toProposalDto(AllianceProposal proposal, Map<Long, String> names, String direction) {
        AllianceProposalDto dto = new AllianceProposalDto();
        dto.setId(proposal.getId());
        dto.setKind(proposal.getKind().name());
        dto.setStatus(proposal.getStatus().name());
        dto.setFromPlayerId(proposal.getFromPlayerId());
        dto.setFromPlayerName(names.get(proposal.getFromPlayerId()));
        dto.setToPlayerId(proposal.getToPlayerId());
        dto.setToPlayerName(names.get(proposal.getToPlayerId()));
        dto.setCreatedTurn(proposal.getCreatedTurn());
        dto.setDirection(direction);
        return dto;
    }
}
