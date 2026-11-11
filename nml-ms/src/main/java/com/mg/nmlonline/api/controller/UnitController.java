package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuyUnitBatchRequestDto;
import com.mg.nmlonline.api.dto.CreateSniperOrderRequestDto;
import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.PlaceFootOrderRequestDto;
import com.mg.nmlonline.api.dto.PlaceUnitRequestDto;
import com.mg.nmlonline.api.dto.RemoveEquipmentRequestDto;
import com.mg.nmlonline.api.dto.AssignEquipmentRequestDto;
import com.mg.nmlonline.api.dto.SniperOrderDto;
import com.mg.nmlonline.api.dto.UnitCatalogDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.service.SniperOrderService;
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
    private final SniperOrderService sniperOrderService;

    public UnitController(UnitService unitService, SniperOrderService sniperOrderService) {
        this.unitService = unitService;
        this.sniperOrderService = sniperOrderService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<UnitCatalogDto> getCatalog(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(unitService.getCatalog(userId));
    }

    @GetMapping("/reserve")
    public ResponseEntity<List<UnitDto>> getReserveUnits(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(unitService.getReserveUnitsDto(userId));
    }

    @PostMapping("/buy-batch")
    public ResponseEntity<List<UnitDto>> buyUnitsBatch(@Valid @RequestBody BuyUnitBatchRequestDto request,
                                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(unitService.buyUnitsDto(userId, request.getItems()));
    }

    @PostMapping("/{unitId}/place")
    public ResponseEntity<UnitDto> placeUnit(@PathVariable Long unitId,
                                             @Valid @RequestBody PlaceUnitRequestDto request,
                                             HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(unitService.placeUnitDto(userId, unitId,
                request.getBoardId(), request.getSectorNumber()));
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

    @PostMapping("/{unitId}/sniper-orders")
    public ResponseEntity<SniperOrderDto> placeSniperOrder(@PathVariable Long unitId,
                                                           @Valid @RequestBody CreateSniperOrderRequestDto request,
                                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(
                sniperOrderService.place(userId, unitId, request.getTargetSectorNumber()));
    }

    @GetMapping("/sniper-orders")
    public ResponseEntity<List<SniperOrderDto>> getMySniperOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(sniperOrderService.currentPending(userId));
    }

    @DeleteMapping("/sniper-orders/{orderId}")
    public ResponseEntity<Void> cancelSniperOrder(@PathVariable Long orderId,
                                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        sniperOrderService.cancel(userId, orderId);
        return ResponseEntity.noContent().build();
    }
}