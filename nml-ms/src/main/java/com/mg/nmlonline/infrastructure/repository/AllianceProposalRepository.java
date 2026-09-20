package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.alliance.AllianceProposal;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.alliance.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllianceProposalRepository extends JpaRepository<AllianceProposal, Long> {

    List<AllianceProposal> findByToPlayerIdAndStatus(Long toPlayerId, ProposalStatus status);

    List<AllianceProposal> findByFromPlayerIdAndStatus(Long fromPlayerId, ProposalStatus status);

    List<AllianceProposal> findByFromPlayerIdAndToPlayerIdAndKindAndStatus(
            Long fromPlayerId, Long toPlayerId, ProposalKind kind, ProposalStatus status);
}
