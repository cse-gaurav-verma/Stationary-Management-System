package com.stationery.inventory.service;

import com.stationery.inventory.model.AuditLog;
import com.stationery.inventory.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog log(String username, String action, String entity, Long entityId, String previous, String updated) {
        AuditLog entry = new AuditLog(username, action, entity, entityId, previous, updated);
        return auditLogRepository.save(entry);
    }
}
