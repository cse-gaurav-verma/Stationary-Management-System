package com.stationery.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.stationery.inventory.service.CategoryService;


/**
 * Main entry point for the Inventory Service microservice.
 * Registers with Eureka for service discovery and manages
 * stationery item inventory operations.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedCategories(CategoryService categoryService) {
        return args -> {
            String[] defaults = {"PAPER","PEN","PENCIL","NOTEBOOK","ERASER","MARKER","FOLDER","STAPLER","OTHER"};
            for (String c : defaults) {
                categoryService.createCategory(c);
            }
        };
    }
}
