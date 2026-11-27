package com.example.catalog.controller;

import com.example.catalog.dto.ServiceRequest;
import com.example.catalog.entity.CatalogService;
import com.example.catalog.service.CatalogServiceManager;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Services and prices are public to read; only admins change them. */
@RestController
@RequestMapping("/api/services")
public class CatalogServiceController {

    private final CatalogServiceManager catalogServiceManager;

    public CatalogServiceController(CatalogServiceManager catalogServiceManager) {
        this.catalogServiceManager = catalogServiceManager;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogService createService(@Valid @RequestBody ServiceRequest request) {
        return catalogServiceManager.createService(toEntity(request));
    }

    @GetMapping
    public List<CatalogService> getAllServices(@PageableDefault(size = 50) Pageable pageable) {
        return catalogServiceManager.getAllServices(pageable);
    }

    @GetMapping("/{id}")
    public CatalogService getServiceById(@PathVariable Long id) {
        return catalogServiceManager.getServiceById(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<CatalogService> getServicesByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 50) Pageable pageable) {
        return catalogServiceManager.getServicesByCategory(categoryId, pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogService updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return catalogServiceManager.updateService(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteService(@PathVariable Long id) {
        catalogServiceManager.deleteService(id);
        return "Service deleted successfully";
    }

    private static CatalogService toEntity(ServiceRequest r) {
        return CatalogService.builder()
                .categoryId(r.getCategoryId())
                .name(r.getName())
                .description(r.getDescription())
                .price(r.getPrice())
                .active(r.isActive())
                .build();
    }
}
