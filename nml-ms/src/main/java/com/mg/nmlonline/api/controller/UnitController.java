package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.PlaceFootOrderRequestDto;
import com.mg.nmlonline.api.dto.RemoveEquipmentRequestDto;
import com.mg.nmlonline.api.dto.AssignEquipmentRequestDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.service.UnitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ownership : playerId jamais lu depuis le corps — re-dérivé depuis userId (JWT) côté service.
 */
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @PostMapping("/{unitId}/equipment")
    public ResponseEntity<UnitDto> assignEquipment(@PathVariable Long unitId,
                                                    @Valid @RequestBody AssignEquipmentRequestDto request,
                                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UnitDto unit = unitService.assignEquipmentDto(unitId, userId, request.getEquipmentName());
        return ResponseEntity.ok(unit);
    }

    @DeleteMapping("/{unitId}/equipment")
    public ResponseEntity<UnitDto> removeEquipment(@PathVariable Long unitId,
                                                   @Valid @RequestBody RemoveEquipmentRequestDto request,
                                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UnitDto unit = unitService.removeEquipmentDto(unitId, userId, request.getEquipmentName());
        return ResponseEntity.ok(unit);
    }

    @PostMapping("/movement/foot")
    public ResponseEntity<MovementOrderDto> placeFootOrder(@Valid @RequestBody PlaceFootOrderRequestDto request,
                                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        MovementOrderDto order = unitService.placeFootOrderDto(userId, request.getEntityIds(), request.getRoute());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/movement")
    public ResponseEntity<List<MovementOrderDto>> getMyPendingOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<MovementOrderDto> orders = unitService.getPlayerPendingOrdersDto(userId);
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