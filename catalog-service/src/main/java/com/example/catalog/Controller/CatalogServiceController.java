package com.example.catalog.controller;

import com.example.catalog.entity.CatalogService;
import com.example.catalog.service.CatalogServiceManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class CatalogServiceController {

    private final CatalogServiceManager catalogServiceManager;

    public CatalogServiceController(
            CatalogServiceManager catalogServiceManager) {

        this.catalogServiceManager = catalogServiceManager;
    }

    // Create service
    @PostMapping
    public CatalogService createService(
            @RequestBody CatalogService service) {

        return catalogServiceManager.createService(service);
    }

    // Get all services
    @GetMapping
    public List<CatalogService> getAllServices() {

        return catalogServiceManager.getAllServices();
    }

    // Get service by ID
    @GetMapping("/{id}")
    public CatalogService getServiceById(
            @PathVariable Long id) {

        return catalogServiceManager.getServiceById(id);
    }

    // Get services belonging to a category
    @GetMapping("/category/{categoryId}")
    public List<CatalogService> getServicesByCategory(
            @PathVariable Long categoryId) {

        return catalogServiceManager
                .getServicesByCategory(categoryId);
    }

    // Update service
    @PutMapping("/{id}")
    public CatalogService updateService(
            @PathVariable Long id,
            @RequestBody CatalogService service) {

        return catalogServiceManager
                .updateService(id, service);
    }

    // Delete service
    @DeleteMapping("/{id}")
    public String deleteService(
            @PathVariable Long id) {

        catalogServiceManager.deleteService(id);

        return "Service deleted successfully";
    }
}