package com.mg.nmlonline.domain.model.bank;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "EXCHANGE_OFFERS")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exchange_offer_seq")
    @SequenceGenerator(name = "exchange_offer_seq", sequenceName = "exchange_offers_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "sender_player_id", nullable = false)
    private Long senderPlayerId;

    @Column(name = "receiver_player_id", nullable = false)
    private Long receiverPlayerId;

    @Column(nullable = false)
    private double money;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeOfferStatus status = ExchangeOfferStatus.PENDING;

    @Column(name = "created_turn", nullable = false)
    private int createdTurn;

    @Column(name = "expires_turn", nullable = false)
    private int expiresTurn;

    @Column(name = "resolved_turn")
    private Integer resolvedTurn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExchangeOfferResource> resources = new ArrayList<>();

    public ExchangeOffer(Long senderPlayerId, Long receiverPlayerId, double money,
                         int createdTurn, int expiresTurn) {
        this.senderPlayerId = senderPlayerId;
        this.receiverPlayerId = receiverPlayerId;
        this.money = money;
        this.createdTurn = createdTurn;
        this.expiresTurn = expiresTurn;
    }

    public void addResource(ExchangeOfferResource resource) {
        resource.setOffer(this);
        resources.add(resource);
    }

    /** Une offre ne survit pas au tour qui l'a vue naître. */
    public boolean isExpiredAt(int currentTurn) {
        return currentTurn >= expiresTurn;
    }

    public boolean isPending() {
        return status == ExchangeOfferStatus.PENDING;
    }

    public void accept(int currentTurn) {
        this.status = ExchangeOfferStatus.ACCEPTED;
        this.resolvedTurn = currentTurn;
    }

    public void decline(int currentTurn) {
        this.status = ExchangeOfferStatus.DECLINED;
        this.resolvedTurn = currentTurn;
    }

    public void cancel(int currentTurn) {
        this.status = ExchangeOfferStatus.CANCELLED;
        this.resolvedTurn = currentTurn;
    }

    public void expire(int currentTurn) {
        this.status = ExchangeOfferStatus.EXPIRED;
        this.resolvedTurn = currentTurn;
    }
}
