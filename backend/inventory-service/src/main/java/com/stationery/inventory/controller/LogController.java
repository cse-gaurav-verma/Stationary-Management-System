package com.stationery.inventory.controller;

import com.stationery.inventory.model.AuditLog;
import com.stationery.inventory.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/logs")
public class LogController {

    private final AuditLogRepository auditLogRepository;

    public LogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        List<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(logs);
    }
}
