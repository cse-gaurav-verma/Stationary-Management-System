package com.stationery.inventory.service;

import com.stationery.inventory.model.ItemCategory;
import com.stationery.inventory.repository.ItemCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final ItemCategoryRepository categoryRepository;

    public CategoryService(ItemCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<ItemCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public ItemCategory createCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(new ItemCategory(null, name.toUpperCase())));
    }

    public void deleteCategory(Long id) { categoryRepository.deleteById(id); }
}
