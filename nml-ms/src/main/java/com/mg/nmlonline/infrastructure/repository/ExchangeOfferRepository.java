package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeOfferRepository extends JpaRepository<ExchangeOffer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ExchangeOffer o WHERE o.id = :id")
    Optional<ExchangeOffer> findByIdForUpdate(@Param("id") Long id);

    long countBySenderPlayerIdAndStatus(Long senderPlayerId, ExchangeOfferStatus status);

    List<ExchangeOffer> findBySenderPlayerIdOrReceiverPlayerIdOrderByCreatedAtDesc(Long senderPlayerId,
                                                                                   Long receiverPlayerId);

    Page<ExchangeOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ExchangeOffer> findByStatusOrderByCreatedAtDesc(ExchangeOfferStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ExchangeOffer o SET o.status = :expired, o.resolvedTurn = :turn "
            + "WHERE o.status = :pending AND o.expiresTurn <= :turn")
    int expirePendingOffers(@Param("pending") ExchangeOfferStatus pending,
                            @Param("expired") ExchangeOfferStatus expired,
                            @Param("turn") int turn);
}
