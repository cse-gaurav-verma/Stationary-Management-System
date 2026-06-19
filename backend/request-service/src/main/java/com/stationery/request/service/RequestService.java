package com.stationery.request.service;

import com.stationery.request.client.InventoryClient;
import com.stationery.request.dto.CreateRequestDto;
import com.stationery.request.dto.RequestItemDto;
import com.stationery.request.dto.RequestResponse;
import com.stationery.request.exception.InsufficientStockException;
import com.stationery.request.exception.ResourceNotFoundException;
import com.stationery.request.model.RequestItem;
import com.stationery.request.model.RequestStatus;
import com.stationery.request.model.StationeryRequest;
import com.stationery.request.repository.RequestRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;
    private final InventoryClient inventoryClient;

    public RequestService(RequestRepository requestRepository, InventoryClient inventoryClient) {
        this.requestRepository = requestRepository;
        this.inventoryClient = inventoryClient;
    }

    // Creates a new request. By default, it goes into PENDING state so admins can review it later.
    @Transactional
    public RequestResponse createRequest(String username, CreateRequestDto createRequestDto) {
        log.info("AUDIT: Creating new stationery request for student: {}", username);

        StationeryRequest request = StationeryRequest.builder()
                .studentUsername(username)
                .status(RequestStatus.PENDING)
                .build();

        // Tie each requested item back to the main request entity before saving.
        // The cascading setup in the entity handles saving these child records.
        for (RequestItemDto itemDto : createRequestDto.getItems()) {
            RequestItem item = RequestItem.builder()
                    .itemId(itemDto.getItemId())
                    .itemName(itemDto.getItemName())
                    .quantity(itemDto.getQuantity())
                    .build();
            request.addItem(item);
        }

        StationeryRequest savedRequest = requestRepository.save(request);
        log.info("AUDIT: Stationery request created successfully. RequestId: {}, Student: {}, Items: {}",
                savedRequest.getRequestId(), username, createRequestDto.getItems().size());

        return mapToResponse(savedRequest);
    }

    // Grabbing a request by its internal DB ID. We throw a standard 404-ish exception if it's missing.
    @Transactional(readOnly = true)
    public RequestResponse getRequestById(Long id) {
        log.debug("Fetching request by ID: {}", id);
        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));
        return mapToResponse(request);
    }

    // Some clients prefer looking up requests by the public UUID to avoid guessing DB sequences.
    @Transactional(readOnly = true)
    public RequestResponse getRequestByRequestId(String requestId) {
        log.debug("Fetching request by requestId: {}", requestId);
        StationeryRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "requestId", requestId));
        return mapToResponse(request);
    }

    // Fetch all requests for a given student so they can see their own history.
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudent(String username) {
        log.debug("Fetching requests for student: {}", username);
        List<StationeryRequest> requests = requestRepository.findByStudentUsername(username);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // For when a student just wants to see their PENDING or APPROVED requests, for example.
    @Transactional(readOnly = true)
    public List<RequestResponse> getRequestsByStudentAndStatus(String username, String status) {
        log.debug("Fetching requests for student: {} with status: {}", username, status);
        RequestStatus requestStatus = parseStatus(status);
        List<StationeryRequest> requests = requestRepository.findByStudentUsernameAndStatus(username, requestStatus);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Full dump of all requests across the system. Usually only called by admin dashboard screens.
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequests() {
        log.debug("Fetching all requests (admin)");
        List<StationeryRequest> requests = requestRepository.findAll();
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helps admins filter the master list, like when they want to bulk-process everything that's still PENDING.
    @Transactional(readOnly = true)
    public List<RequestResponse> getAllRequestsByStatus(String status) {
        log.debug("Fetching all requests with status: {} (admin)", status);
        RequestStatus requestStatus = parseStatus(status);
        List<StationeryRequest> requests = requestRepository.findByStatus(requestStatus);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // The core approval logic. We check if there's enough stock first, then deduct it if everything looks good.
    @Transactional
    public RequestResponse approveRequest(Long id, String adminUsername) {
        log.info("AUDIT: Admin '{}' approving request ID: {}", adminUsername, id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be approved when in PENDING status. Current status: " + request.getStatus());
        }

        // We're talking to an external inventory service here.
        // We do a two-phase approach: first check if we have enough stock for *all* items.
        // If we just deducted as we went and item #3 failed, we'd have to figure out how to roll back items #1 and #2.
        for (RequestItem item : request.getItems()) {
            try {
                log.info("AUDIT: Validating stock for item '{}' (ID: {}) - needed: {}",
                        item.getItemName(), item.getItemId(), item.getQuantity());
                var resp = inventoryClient.getInventoryItem(item.getItemId());
                if (resp == null || resp.getBody() == null) {
                    log.error("AUDIT: Inventory service returned empty response for item ID: {}", item.getItemId());
                    throw new RuntimeException("Inventory check failed for item: " + item.getItemName());
                }
                int available = resp.getBody().getAvailableQuantity();
                if (available < item.getQuantity()) {
                    log.error("AUDIT: Insufficient stock for item '{}' (ID: {}). Available: {}, Needed: {}",
                            item.getItemName(), item.getItemId(), available, item.getQuantity());
                    throw new InsufficientStockException(item.getItemName(), item.getQuantity());
                }
            } catch (FeignException.BadRequest e) {
                log.error("AUDIT: Inventory service reported bad request for item ID: {}", item.getItemId());
                throw new InsufficientStockException(item.getItemName(), item.getQuantity());
            } catch (FeignException e) {
                log.error("AUDIT: Failed to validate inventory for item '{}' (ID: {}): {}",
                        item.getItemName(), item.getItemId(), e.getMessage());
                throw new RuntimeException("Failed to validate inventory for item: " + item.getItemName(), e);
            }
        }

        // Okay, we've verified stock is sufficient. Now we actually deduct the quantities.
        // Note: In a highly concurrent system, stock could potentially drop between our check and deduction,
        // but handling that usually requires something like a saga pattern or distributed locks.
        for (RequestItem item : request.getItems()) {
            try {
                log.info("AUDIT: Deducting {} units of item '{}' (ID: {}) from inventory",
                        item.getQuantity(), item.getItemName(), item.getItemId());
                inventoryClient.deductItemQuantity(item.getItemId(), item.getQuantity());
            } catch (FeignException.BadRequest e) {
                log.error("AUDIT: Insufficient stock during deduction for item '{}' (ID: {}). Approval failed.",
                        item.getItemName(), item.getItemId());
                throw new InsufficientStockException(item.getItemName(), item.getQuantity());
            } catch (FeignException e) {
                log.error("AUDIT: Failed to deduct inventory for item '{}' (ID: {}): {}",
                        item.getItemName(), item.getItemId(), e.getMessage());
                throw new RuntimeException("Failed to deduct inventory for item: " + item.getItemName(), e);
            }
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);
        
        // Hibernate lazy loading quirk: we touch the items collection here to initialize it
        // so we don't get a LazyInitializationException when we try to map the response later outside the transaction context.
        savedRequest.getItems().size();

        log.info("AUDIT: Request ID: {} approved by admin '{}'. All inventory deductions successful.",
                id, adminUsername);

        return mapToResponse(savedRequest);
    }

    // Rejection just updates the status and reason. No inventory side-effects to worry about here.
    @Transactional
    public RequestResponse rejectRequest(Long id, String adminUsername, String reason) {
        log.info("AUDIT: Admin '{}' rejecting request ID: {} with reason: '{}'", adminUsername, id, reason);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Request can only be rejected when in PENDING status. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        request.setAdminUsername(adminUsername);
        StationeryRequest savedRequest = requestRepository.save(request);
        
        // Hibernate lazy loading quirk: we touch the items collection here to initialize it
        // so we don't get a LazyInitializationException when we try to map the response later outside the transaction context.
        savedRequest.getItems().size();

        log.info("AUDIT: Request ID: {} rejected by admin '{}'.", id, adminUsername);

        return mapToResponse(savedRequest);
    }

    // Marks the request as physically handed over to the student.
    // It has to be APPROVED first, otherwise they haven't secured the inventory.
    @Transactional
    public RequestResponse fulfillRequest(Long id) {
        log.info("AUDIT: Fulfilling request ID: {}", id);

        StationeryRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Request can only be fulfilled when in APPROVED status. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.FULFILLED);
        StationeryRequest savedRequest = requestRepository.save(request);
        
        // Hibernate lazy loading quirk: we touch the items collection here to initialize it
        // so we don't get a LazyInitializationException when we try to map the response later outside the transaction context.
        savedRequest.getItems().size();

        log.info("AUDIT: Request ID: {} fulfilled successfully.", id);

        return mapToResponse(savedRequest);
    }

    // --- Helper utilities below ---

    private RequestStatus parseStatus(String status) {
        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request status: " + status
                    + ". Valid values are: PENDING, APPROVED, REJECTED, FULFILLED");
        }
    }

    private RequestResponse mapToResponse(StationeryRequest request) {
        List<RequestItemDto> itemDtos = request.getItems().stream()
                .map(item -> RequestItemDto.builder()
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return RequestResponse.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .studentUsername(request.getStudentUsername())
                .items(itemDtos)
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .adminUsername(request.getAdminUsername())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
