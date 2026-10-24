package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnitRecoveryService {

    private final UnitRepository unitRepository;

    public UnitRecoveryService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Transactional
    public void recoverWoundedUnits(int turnEnding) {
        List<Unit> recovered = unitRepository.findAll().stream()
                .filter(unit -> unit.shouldRecover(turnEnding))
                .toList();
        recovered.forEach(Unit::recover);
        recovered.stream()
                .map(Unit::getSector)
                .filter(sector -> sector != null)
                .distinct()
                .forEach(Sector::recalculateMilitaryPower);
    }
}
