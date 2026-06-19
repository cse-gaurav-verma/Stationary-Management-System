package com.stationery.inventory.controller;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST controller for inventory operations. Auth is delegated to API Gateway - it forwards
// user info via headers (X-User-Role, X-User-Name). This keeps the service focused on inventory logic.
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    // Constructor injection - Spring automatically wires in the service
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Create new item - only admins allowed. @Valid runs DTO validations automatically before execution.
    // Checks role from header, logs for audit trail, returns 201 Created on success
    @PostMapping
    public ResponseEntity<StationeryItemResponse> createItem(
            @Valid @RequestBody StationeryItemRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized create attempt by user '{}' with role '{}'", userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) creating new stationery item: '{}'",
                userName, userRole, request.getName());

        StationeryItemResponse response = inventoryService.createItem(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Fetch all items with pagination. Default: page 0, size 20, sort by name.
    // Pagination is critical for performance - prevents huge payloads as inventory grows.
    @GetMapping
    public ResponseEntity<Page<StationeryItemResponse>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {

        Page<StationeryItemResponse> items = inventoryService.getAllItems(page, size, sortBy);
        return ResponseEntity.ok(items);
    }

    // Fetch single item by ID. Straightforward delegation to service layer.
    @GetMapping("/{id}")
    public ResponseEntity<StationeryItemResponse> getItemById(@PathVariable Long id) {
        StationeryItemResponse response = inventoryService.getItemById(id);
        return ResponseEntity.ok(response);
    }

    // Filter items by category with pagination. Same reason we paginate here - can have many items per category.
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<StationeryItemResponse>> getItemsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<StationeryItemResponse> items = inventoryService.getItemsByCategory(category, page, size);
        return ResponseEntity.ok(items);
    }

    // Update item - admin only. ID from URL path, new data from request body.
    // @Valid ensures all field constraints pass before method runs.
    @PutMapping("/{id}")
    public ResponseEntity<StationeryItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody StationeryItemRequest request,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized update attempt on item ID {} by user '{}' with role '{}'",
                    id, userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) updating stationery item ID: {}", userName, userRole, id);

        StationeryItemResponse response = inventoryService.updateItem(id, request);
        return ResponseEntity.ok(response);
    }

    // Delete item - admin only. Returns 204 No Content (standard REST: success but no body to return).
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String userName) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized delete attempt on item ID {} by user '{}' with role '{}'",
                    id, userName, userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("AUDIT: User '{}' (role: {}) deleting stationery item ID: {}", userName, userRole, id);

        inventoryService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // Admin-only endpoint to identify items needing reorder. Regular users don't need this detail.
    @GetMapping("/low-stock")
    public ResponseEntity<List<StationeryItemResponse>> getLowStockItems(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            log.warn("AUDIT: Unauthorized low-stock access attempt with role '{}'", userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<StationeryItemResponse> lowStockItems = inventoryService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }

    // Internal endpoint called by Request Service to deduct stock when items are requested.
    // Atomic operation prevents race conditions when multiple requests hit simultaneously.
    @PutMapping("/{id}/deduct")
    public ResponseEntity<Boolean> deductQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        log.info("AUDIT: Internal service call - deducting {} units from item ID: {}", quantity, id);

        boolean result = inventoryService.deductQuantity(id, quantity);
        return ResponseEntity.ok(result);
    }

    // Basic search by keyword - case-insensitive name match. Works well for MVP; 
    // could upgrade to Elasticsearch later if search performance becomes an issue.
    @GetMapping("/search")
    public ResponseEntity<List<StationeryItemResponse>> searchItems(
            @RequestParam String keyword) {

        List<StationeryItemResponse> items = inventoryService.searchItems(keyword);
        return ResponseEntity.ok(items);
    }
}
