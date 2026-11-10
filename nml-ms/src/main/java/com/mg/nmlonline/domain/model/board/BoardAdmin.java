package com.mg.nmlonline.domain.model.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Délégation d'administration d'une partie (MJ), indépendante d'une participation joueur. */
@Entity
@Table(name = "BOARD_ADMINS")
@IdClass(BoardAdmin.BoardAdminId.class)
@Getter
@Setter
@NoArgsConstructor
public class BoardAdmin {

    @Id
    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    public BoardAdmin(Long boardId, Long userId) {
        this.boardId = boardId;
        this.userId = userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class BoardAdminId implements Serializable {
        private Long boardId;
        private Long userId;
    }
}
