package com.example.catalog.controller;

import com.example.catalog.entity.Technician;
import com.example.catalog.service.TechnicianService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @PostMapping
    public Technician createTechnician(
            @RequestBody Technician technician) {

        return technicianService.createTechnician(technician);
    }

    @GetMapping
    public List<Technician> getAllTechnicians() {

        return technicianService.getAllTechnicians();
    }

    @GetMapping("/{id}")
    public Technician getTechnicianById(
            @PathVariable Long id) {

        return technicianService.getTechnicianById(id);
    }

    @PutMapping("/{id}")
    public Technician updateTechnician(
            @PathVariable Long id,
            @RequestBody Technician technician) {

        return technicianService.updateTechnician(id, technician);
    }

    @DeleteMapping("/{id}")
    public String deleteTechnician(
            @PathVariable Long id) {

        technicianService.deleteTechnician(id);

        return "Technician deleted successfully";
    }
}