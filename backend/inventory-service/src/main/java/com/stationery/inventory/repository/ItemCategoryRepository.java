package com.stationery.inventory.repository;

import com.stationery.inventory.model.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    Optional<ItemCategory> findByNameIgnoreCase(String name);
}
