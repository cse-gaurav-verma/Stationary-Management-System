package com.stationery.request.controller;

import com.stationery.request.dto.ApproveRejectDto;
import com.stationery.request.dto.CreateRequestDto;
import com.stationery.request.dto.RequestResponse;
import com.stationery.request.service.RequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST controller for request management. Auth validation delegated to API Gateway via headers.
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);
    private final RequestService requestService;

    // Constructor injection - Spring wires in the service dependency
    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // Create new request - students only. @Valid validates DTO constraints automatically.
    // Role check ensures only students can create requests.
    @PostMapping
    public ResponseEntity<RequestResponse> createRequest(
            @RequestHeader("X-User-Name") String username,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateRequestDto createRequestDto) {

        log.info("AUDIT: POST /api/requests - User: {}, Role: {}", username, role);
        validateRole(role, "STUDENT");

        RequestResponse response = requestService.createRequest(username, createRequestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get student's own requests. Optional status filter to reduce payload (e.g., show only PENDING or APPROVED).
    @GetMapping("/my")
    public ResponseEntity<List<RequestResponse>> getMyRequests(
            @RequestHeader("X-User-Name") String username,
            @RequestParam(value = "status", required = false) String status) {

        log.info("AUDIT: GET /api/requests/my - User: {}, Status filter: {}", username, status);

        List<RequestResponse> responses;
        if (status != null && !status.isEmpty()) {
            responses = requestService.getRequestsByStudentAndStatus(username, status);
        } else {
            responses = requestService.getRequestsByStudent(username);
        }
        return ResponseEntity.ok(responses);
    }

    // Fetch request by internal database ID. Used for admin dashboards and direct links.
    @GetMapping("/{id}")
    public ResponseEntity<RequestResponse> getRequestById(@PathVariable Long id) {
        log.info("AUDIT: GET /api/requests/{} - Fetching request by ID", id);
        RequestResponse response = requestService.getRequestById(id);
        return ResponseEntity.ok(response);
    }

    // Track request by public UUID (not exposing internal DB IDs to frontend).
    @GetMapping("/track/{requestId}")
    public ResponseEntity<RequestResponse> getRequestByRequestId(@PathVariable String requestId) {
        log.info("AUDIT: GET /api/requests/track/{} - Tracking request", requestId);
        RequestResponse response = requestService.getRequestByRequestId(requestId);
        return ResponseEntity.ok(response);
    }

    // Admin-only: view all requests. Status filter helps admins focus on what needs attention.
    @GetMapping
    public ResponseEntity<List<RequestResponse>> getAllRequests(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(value = "status", required = false) String status) {

        log.info("AUDIT: GET /api/requests - Role: {}, Status filter: {}", role, status);
        validateRole(role, "ADMIN");

        List<RequestResponse> responses;
        if (status != null && !status.isEmpty()) {
            responses = requestService.getAllRequestsByStatus(status);
        } else {
            responses = requestService.getAllRequests();
        }
        return ResponseEntity.ok(responses);
    }

    // Approve request - admin only. Log admin name for audit trail.
    @PutMapping("/{id}/approve")
    public ResponseEntity<RequestResponse> approveRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String adminUsername,
            @RequestHeader("X-User-Role") String role) {

        log.info("AUDIT: PUT /api/requests/{}/approve - Admin: {}, Role: {}", id, adminUsername, role);
        validateRole(role, "ADMIN");

        RequestResponse response = requestService.approveRequest(id, adminUsername);
        return ResponseEntity.ok(response);
    }

    // Reject request with optional reason. Uses PUT (not DELETE) to keep audit history.
    @PutMapping("/{id}/reject")
    public ResponseEntity<RequestResponse> rejectRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String adminUsername,
            @RequestHeader("X-User-Role") String role,
            @RequestBody(required = false) ApproveRejectDto approveRejectDto) {

        log.info("AUDIT: PUT /api/requests/{}/reject - Admin: {}, Role: {}", id, adminUsername, role);
        validateRole(role, "ADMIN");

        String reason = (approveRejectDto != null) ? approveRejectDto.getRejectionReason() : null;
        RequestResponse response = requestService.rejectRequest(id, adminUsername, reason);
        return ResponseEntity.ok(response);
    }

    // Mark request as fulfilled (items handed out). Completes the request lifecycle.
    @PutMapping("/{id}/fulfill")
    public ResponseEntity<RequestResponse> fulfillRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {

        log.info("AUDIT: PUT /api/requests/{}/fulfill - Role: {}", id, role);
        validateRole(role, "ADMIN");

        RequestResponse response = requestService.fulfillRequest(id);
        return ResponseEntity.ok(response);
    }

    // A quick-and-dirty programmatic role check.
    // In a real production environment we'd probably use Spring Security and @PreAuthorize,
    // but this gets the job done for now by just checking headers passed from our API gateway.
    private void validateRole(String actualRole, String requiredRole) {
        if (actualRole == null || !actualRole.equalsIgnoreCase(requiredRole)) {
            throw new IllegalArgumentException(
                    "Access denied. Required role: " + requiredRole + ", but got: " + actualRole);
        }
    }
}
