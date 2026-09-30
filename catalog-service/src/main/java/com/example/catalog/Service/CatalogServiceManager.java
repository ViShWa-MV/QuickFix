package com.example.catalog.service;

import com.example.catalog.entity.CatalogService;
import com.example.catalog.repository.CatalogServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogServiceManager {

    private final CatalogServiceRepository catalogServiceRepository;

    public CatalogServiceManager(
            CatalogServiceRepository catalogServiceRepository) {

        this.catalogServiceRepository = catalogServiceRepository;
    }

    // Create
    public CatalogService createService(CatalogService service) {

        return catalogServiceRepository.save(service);
    }

    // Get all services
    public List<CatalogService> getAllServices() {

        return catalogServiceRepository.findAll();
    }

    // Get service by ID
    public CatalogService getServiceById(Long id) {

        return catalogServiceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Service not found"));
    }

    // Get services by category
    public List<CatalogService> getServicesByCategory(Long categoryId) {

        return catalogServiceRepository.findByCategoryId(categoryId);
    }

    // Update
    public CatalogService updateService(
            Long id,
            CatalogService service) {

        CatalogService existingService =
                catalogServiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Service not found"));

        existingService.setCategoryId(service.getCategoryId());
        existingService.setName(service.getName());
        existingService.setDescription(service.getDescription());
        existingService.setPrice(service.getPrice());
        existingService.setActive(service.isActive());

        return catalogServiceRepository.save(existingService);
    }

    // Delete
    public void deleteService(Long id) {

        CatalogService existingService =
                catalogServiceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Service not found"));

        catalogServiceRepository.delete(existingService);
    }
}