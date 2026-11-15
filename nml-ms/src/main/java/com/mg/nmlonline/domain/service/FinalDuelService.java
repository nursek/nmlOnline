package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.alliance.AnnouncementType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FinalDuelService {

    private final AllianceRepository allianceRepository;
    private final AnnouncementRepository announcementRepository;

    public FinalDuelService(AllianceRepository allianceRepository,
                            AnnouncementRepository announcementRepository) {
        this.allianceRepository = allianceRepository;
        this.announcementRepository = announcementRepository;
    }

    public void breakLastAllianceIfNeeded(Board board, List<Player> players, int turn) {
        List<Player> active = players.stream().filter(player -> !player.isEliminated()).toList();
        if (active.size() != 2) {
            return;
        }
        Player first = active.get(0);
        Player second = active.get(1);
        Alliance alliance = allianceRepository.findActiveBetween(first.getId(), second.getId())
                .orElse(null);
        if (alliance == null) {
            return;
        }
        alliance.end(turn, null, false);
        allianceRepository.save(alliance);
        announcementRepository.save(Announcement.create(AnnouncementType.FINAL_DUEL, board.getId(),
                first.getId(), second.getId(), turn));
    }
}
