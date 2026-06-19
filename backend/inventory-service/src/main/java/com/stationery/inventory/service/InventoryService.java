package com.stationery.inventory.service;

import com.stationery.inventory.dto.StationeryItemRequest;
import com.stationery.inventory.dto.StationeryItemResponse;
import com.stationery.inventory.exception.InsufficientStockException;
import com.stationery.inventory.exception.ResourceNotFoundException;
import com.stationery.inventory.model.StationeryItem;
import com.stationery.inventory.repository.StationeryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Core service for inventory CRUD operations and business logic.
// All write operations (create, update, delete) are audit-logged for compliance.
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
        private final StationeryItemRepository stationeryItemRepository;
        private final com.stationery.inventory.service.AuditService auditService;
        private final com.stationery.inventory.service.CategoryService categoryService;

        // Constructor injection - Spring auto-wires repository
        public InventoryService(StationeryItemRepository stationeryItemRepository,
                                                        com.stationery.inventory.service.AuditService auditService,
                                                        com.stationery.inventory.service.CategoryService categoryService) {
                this.stationeryItemRepository = stationeryItemRepository;
                this.auditService = auditService;
                this.categoryService = categoryService;
        }

    // Create new item. Uppercase category to prevent data inconsistency. Security (admin check) is at controller layer.
    @Transactional
    public StationeryItemResponse createItem(StationeryItemRequest request) {
        log.info("AUDIT: Creating new stationery item with name: '{}'", request.getName());

        // Ensure category exists (create if missing) so categories remain consistent across items
        categoryService.createCategory(request.getCategory());

        StationeryItem item = StationeryItem.builder()
                .name(request.getName())
                .category(request.getCategory().toUpperCase())
                .unit(request.getUnit())
                .availableQuantity(request.getAvailableQuantity())
                .minimumQuantity(request.getMinimumQuantity())
                .description(request.getDescription())
                .build();

        StationeryItem savedItem = stationeryItemRepository.save(item);
        log.info("AUDIT: Successfully created stationery item with ID: {}, name: '{}'",
                savedItem.getId(), savedItem.getName());

        auditService.log("SYSTEM", "CREATE", "StationeryItem", savedItem.getId(), null, savedItem.getName());

        return mapToResponse(savedItem);
    }

    // Fetch item by ID. Throw ResourceNotFoundException early if not found - let global handler return 404.
    @Transactional(readOnly = true)
    public StationeryItemResponse getItemById(Long id) {
        log.debug("Fetching stationery item with ID: {}", id);

        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        return mapToResponse(item);
    }

    // Fetch all items with pagination. Pagination prevents memory issues as inventory scales.
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getAllItems(int page, int size, String sortBy) {
        log.debug("Fetching all stationery items - page: {}, size: {}, sortBy: {}", page, size, sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<StationeryItem> itemsPage = stationeryItemRepository.findAll(pageable);

        return itemsPage.map(this::mapToResponse);
    }

    // Fetch items by category with pagination. Normalize category to uppercase for consistency.
    @Transactional(readOnly = true)
    public Page<StationeryItemResponse> getItemsByCategory(String category, int page, int size) {
        log.debug("Fetching stationery items by category: '{}' - page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<StationeryItem> itemsPage = stationeryItemRepository.findByCategory(category.toUpperCase(), pageable);

        return itemsPage.map(this::mapToResponse);
    }

    // Update full item. Compare old/new values to audit-log changes for compliance.
    @Transactional
    public StationeryItemResponse updateItem(Long id, StationeryItemRequest request) {
        log.info("AUDIT: Updating stationery item with ID: {}", id);

        StationeryItem existingItem = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        // Compare old and new values to leave a paper trail of the edits
        if (!existingItem.getName().equals(request.getName())) {
            log.info("AUDIT: Item ID {} - name changed from '{}' to '{}'",
                    id, existingItem.getName(), request.getName());
        }
        if (!existingItem.getCategory().equals(request.getCategory().toUpperCase())) {
            log.info("AUDIT: Item ID {} - category changed from '{}' to '{}'",
                    id, existingItem.getCategory(), request.getCategory().toUpperCase());
        }
        if (!existingItem.getAvailableQuantity().equals(request.getAvailableQuantity())) {
            log.info("AUDIT: Item ID {} - availableQuantity changed from {} to {}",
                    id, existingItem.getAvailableQuantity(), request.getAvailableQuantity());
        }
        if (!existingItem.getMinimumQuantity().equals(request.getMinimumQuantity())) {
            log.info("AUDIT: Item ID {} - minimumQuantity changed from {} to {}",
                    id, existingItem.getMinimumQuantity(), request.getMinimumQuantity());
        }

        existingItem.setName(request.getName());
        existingItem.setCategory(request.getCategory().toUpperCase());
        existingItem.setUnit(request.getUnit());
        existingItem.setAvailableQuantity(request.getAvailableQuantity());
        existingItem.setMinimumQuantity(request.getMinimumQuantity());
        existingItem.setDescription(request.getDescription());

        StationeryItem updatedItem = stationeryItemRepository.save(existingItem);
        log.info("AUDIT: Successfully updated stationery item with ID: {}", id);

        auditService.log("SYSTEM", "UPDATE", "StationeryItem", updatedItem.getId(), existingItem.getName(), updatedItem.getName());

        return mapToResponse(updatedItem);
    }

    // Hard delete. In enterprise apps, consider soft delete (flag instead of removing) to preserve order history.
    @Transactional
    public void deleteItem(Long id) {
        log.info("AUDIT: Attempting to delete stationery item with ID: {}", id);

        StationeryItem item = stationeryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + id));

        stationeryItemRepository.delete(item);
        log.info("AUDIT: Successfully deleted stationery item with ID: {}, name: '{}'",
                id, item.getName());

        auditService.log("SYSTEM", "DELETE", "StationeryItem", id, item.getName(), null);
    }

    // Find items below minimum threshold. Filter in memory for now; use DB query if this becomes a bottleneck.
    @Transactional(readOnly = true)
    public List<StationeryItemResponse> getLowStockItems() {
        log.debug("Fetching low stock items");

        List<StationeryItem> allItems = stationeryItemRepository.findAll();

        return allItems.stream()
                .filter(item -> item.getAvailableQuantity() <= item.getMinimumQuantity())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Deduct stock when request is approved. Check balance first - throw exception if insufficient (rollback on error).
    @Transactional
    public boolean deductQuantity(Long itemId, Integer quantity) {
        log.info("AUDIT: Deducting quantity {} from item ID: {}", quantity, itemId);

        StationeryItem item = stationeryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stationery item not found with ID: " + itemId));

        if (item.getAvailableQuantity() < quantity) {
            log.warn("AUDIT: Insufficient stock for item ID: {}. Available: {}, Requested: {}",
                    itemId, item.getAvailableQuantity(), quantity);
            throw new InsufficientStockException(
                    String.format("Insufficient stock for item '%s'. Available: %d, Requested: %d",
                            item.getName(), item.getAvailableQuantity(), quantity));
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        stationeryItemRepository.save(item);

        log.info("AUDIT: Successfully deducted {} from item ID: {}. New available quantity: {}",
                quantity, itemId, item.getAvailableQuantity());

        return true;
    }

    // Simple keyword search (case-insensitive). Upgrade to Elasticsearch if search becomes a bottleneck.
    @Transactional(readOnly = true)
    public List<StationeryItemResponse> searchItems(String keyword) {
        log.debug("Searching stationery items with keyword: '{}'", keyword);

        List<StationeryItem> items = stationeryItemRepository.findByNameContainingIgnoreCase(keyword);

        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Utility to map our database entity to the DTO we expose to the client.
    // We calculate the 'lowStock' boolean dynamically on the fly rather than storing it in the DB 
    // since it's just derived from the current and minimum quantities.
    public StationeryItemResponse mapToResponse(StationeryItem item) {
        return StationeryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .availableQuantity(item.getAvailableQuantity())
                .minimumQuantity(item.getMinimumQuantity())
                .description(item.getDescription())
                .lowStock(item.getAvailableQuantity() <= item.getMinimumQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
