package com.example.QuickFix.repository;

import com.example.QuickFix.entity.PincodeOffice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PincodeRepo extends JpaRepository<PincodeOffice, Long> {

    List<PincodeOffice> findByPincodeOrderByOfficeAsc(String pincode);
}
