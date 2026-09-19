package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PendingCaptureDto;
import com.mg.nmlonline.domain.service.PendingCaptureService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pending-captures")
@PreAuthorize("hasRole('ADMIN')")
public class PendingCaptureController {

    private final PendingCaptureService pendingCaptureService;

    public PendingCaptureController(PendingCaptureService pendingCaptureService) {
        this.pendingCaptureService = pendingCaptureService;
    }

    @GetMapping
    public ResponseEntity<List<PendingCaptureDto>> list() {
        return ResponseEntity.ok(pendingCaptureService.list());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable("id") Long id, @RequestParam("playerId") Long playerId) {
        pendingCaptureService.resolve(id, playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismiss(@PathVariable("id") Long id) {
        pendingCaptureService.dismiss(id);
        return ResponseEntity.ok().build();
    }
}
