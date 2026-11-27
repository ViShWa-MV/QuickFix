package com.example.QuickFix.DTO;

import java.util.List;

/**
 * The official place names for a pincode. The form fills state and district from
 * this; offices are the names a customer may pick as their city.
 */
public record PincodeInfo(String pincode, String state, String district, List<String> offices) {
}
