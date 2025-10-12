// java
package com.mg.nmlonline.entity.player;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PLAYERS")
@Data
@NoArgsConstructor
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Lob
    @Column(name = "stats", columnDefinition = "BLOB")
    private byte[] stats;

    @Lob
    @Column(name = "equipments", columnDefinition = "BLOB")
    private byte[] equipments;

    @Lob
    @Column(name = "sectors", columnDefinition = "BLOB")
    private byte[] sectors;
}
