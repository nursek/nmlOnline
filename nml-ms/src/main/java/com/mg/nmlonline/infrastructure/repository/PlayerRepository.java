package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.player.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByName(String name);
    Optional<Player> findByUserId(Long userId);

    Page<Player> findAllByOrderByNameAsc(Pageable pageable);

    /**
     * Variante pessimistic-write pour les flux d'achat : pose SELECT ... FOR UPDATE sur le row
     * Player avant le check + débit money, empêchant le lost-update double-spend.
     * ponytail: ceiling = un seul row lock par achat ; si throughput devient un pb, passer à
     * @Version sur Player avec retry sur OptimisticLockException.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Player p WHERE p.id = :id")
    Optional<Player> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Player p WHERE p.userId = :userId")
    Optional<Player> findByUserIdForUpdate(@Param("userId") Long userId);
}
