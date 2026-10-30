package com.example.catalog.service;

import com.example.catalog.entity.Technician;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public TechnicianService(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    public Technician createTechnician(Technician technician) {
        return technicianRepository.save(technician);
    }

    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    public Technician getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Technician not found"));
    }

    public Technician updateTechnician(
            Long id,
            Technician technician) {

        Technician existingTechnician =
                technicianRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Technician not found"));

        existingTechnician.setName(technician.getName());
        existingTechnician.setPhone(technician.getPhone());
        existingTechnician.setEmail(technician.getEmail());
        existingTechnician.setExperience(technician.getExperience());
        existingTechnician.setRating(technician.getRating());

        return technicianRepository.save(existingTechnician);
    }

    public void deleteTechnician(Long id) {

        Technician existingTechnician =
                technicianRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Technician not found"));

        technicianRepository.delete(existingTechnician);
    }
}