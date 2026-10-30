package com.example.catalog.repository;

import com.example.catalog.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository
        extends JpaRepository<Technician, Long> {
}