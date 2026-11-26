package com.example.catalog.repository;

import com.example.catalog.entity.CatalogService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogServiceRepository
        extends JpaRepository<CatalogService, Long> {

    List<CatalogService> findByCategoryId(Long categoryId);
}