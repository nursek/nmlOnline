package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.PlaceFootOrderRequestDto;
import com.mg.nmlonline.api.dto.RemoveEquipmentRequestDto;
import com.mg.nmlonline.api.dto.AssignEquipmentRequestDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.service.UnitService;
import com.mg.nmlonline.mapper.MovementMapper;
import com.mg.nmlonline.mapper.UnitMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints par joueur pour interagir avec ses unités : équipement (depuis
 * l'inventaire du joueur) et ordre de déplacement à pied.
 *
 * <p>Ownership (AGENTS.md) : {@code playerId} n'est jamais lu depuis le corps —
 * il est re-dérivé depuis {@code userId} (JWT) côté service.
 */
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService unitService;
    private final UnitMapper unitMapper;
    private final MovementMapper movementMapper;

    public UnitController(UnitService unitService, UnitMapper unitMapper, MovementMapper movementMapper) {
        this.unitService = unitService;
        this.unitMapper = unitMapper;
        this.movementMapper = movementMapper;
    }

    // ==========================================================
    // === ÉQUIPEMENT D'UNITÉ ====================================
    // ==========================================================

    @PostMapping("/{unitId}/equipment")
    public ResponseEntity<UnitDto> assignEquipment(@PathVariable Long unitId,
                                                    @Valid @RequestBody AssignEquipmentRequestDto request,
                                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        Unit unit = unitService.assignEquipment(unitId, userId, request.getEquipmentName());
        return ResponseEntity.ok(unitMapper.toDto(unit));
    }

    @DeleteMapping("/{unitId}/equipment")
    public ResponseEntity<UnitDto> removeEquipment(@PathVariable Long unitId,
                                                   @Valid @RequestBody RemoveEquipmentRequestDto request,
                                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        Unit unit = unitService.removeEquipment(unitId, userId, request.getEquipmentName());
        return ResponseEntity.ok(unitMapper.toDto(unit));
    }

    // ==========================================================
    // === ORDRES DE DÉPLACEMENT À PIED ==========================
    // ==========================================================

    @PostMapping("/movement/foot")
    public ResponseEntity<MovementOrderDto> placeFootOrder(@Valid @RequestBody PlaceFootOrderRequestDto request,
                                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        MovementOrder order = unitService.placeFootOrder(userId, request.getEntityIds(), request.getRoute());
        return ResponseEntity.ok(movementMapper.toDto(order));
    }

    @GetMapping("/movement")
    public ResponseEntity<List<MovementOrderDto>> getMyPendingOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<MovementOrderDto> orders = unitService.getPlayerPendingOrders(userId).stream()
                .map(movementMapper::toDto)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @DeleteMapping("/movement/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        unitService.cancelOrder(userId, orderId);
        return ResponseEntity.noContent().build();
    }
}