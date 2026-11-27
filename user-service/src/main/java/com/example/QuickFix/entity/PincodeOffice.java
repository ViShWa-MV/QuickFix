package com.example.QuickFix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One post office of one pincode, from the official India Post list. */
@Entity
@Table(name = "pincode_offices")
@Getter
@Setter
@NoArgsConstructor
public class PincodeOffice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(nullable = false, length = 160)
    private String office;

    @Column(nullable = false, length = 120)
    private String district;

    @Column(nullable = false, length = 120)
    private String state;
}
