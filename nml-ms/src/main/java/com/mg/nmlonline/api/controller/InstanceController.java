package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.InstanceDto;
import com.mg.nmlonline.domain.service.InstanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @GetMapping
    public List<InstanceDto> list(HttpServletRequest request) {
        return instanceService.listForUser((Long) request.getAttribute("userId"),
                request.isUserInRole("ADMIN"));
    }
}
