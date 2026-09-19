package com.mg.nmlonline.domain.model.bank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "EXCHANGE_OFFER_RESOURCES")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeOfferResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exchange_offer_resource_seq")
    @SequenceGenerator(name = "exchange_offer_resource_seq", sequenceName = "exchange_offer_resources_id_seq", allocationSize = 50)
    private Long id;

    // FK NOT NULL portée par la ligne enfant : le côté enfant doit être propriétaire de l'association.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private ExchangeOffer offer;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(nullable = false)
    private int quantity;

    public ExchangeOfferResource(String resourceName, int quantity) {
        this.resourceName = resourceName;
        this.quantity = quantity;
    }
}
