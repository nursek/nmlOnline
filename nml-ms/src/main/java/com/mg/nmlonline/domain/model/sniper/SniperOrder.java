package com.mg.nmlonline.domain.model.sniper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "SNIPER_ORDERS")
@Getter
@Setter
@NoArgsConstructor
public class SniperOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sniper_order_seq")
    @SequenceGenerator(name = "sniper_order_seq", sequenceName = "sniper_orders_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int turn;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "from_sector", nullable = false)
    private int fromSectorNumber;

    @Column(name = "target_sector", nullable = false)
    private int targetSectorNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SniperOrderStatus status = SniperOrderStatus.PENDING;

    @Column(name = "status_message", length = 500)
    private String statusMessage;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public static SniperOrder create(Long boardId, Long playerId, int turn, Long unitId, int fromSectorNumber,
                                     int targetSectorNumber) {
        SniperOrder order = new SniperOrder();
        order.setBoardId(boardId);
        order.setPlayerId(playerId);
        order.setTurn(turn);
        order.setUnitId(unitId);
        order.setFromSectorNumber(fromSectorNumber);
        order.setTargetSectorNumber(targetSectorNumber);
        order.setStatus(SniperOrderStatus.PENDING);
        order.setSubmittedAt(Instant.now());
        return order;
    }

    public boolean isPending() {
        return status == SniperOrderStatus.PENDING;
    }

    public void cancel() {
        this.status = SniperOrderStatus.CANCELLED;
    }

    public void resolve() {
        this.status = SniperOrderStatus.RESOLVED;
    }
}
