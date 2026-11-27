package com.example.QuickFix.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceNamesTest {

    @ParameterizedTest
    @CsvSource({
            "Bengaluru Urban, bengaluru",
            "Bangalore, bengaluru",
            "Bengaluru GPO, bengaluru",
            "Koramangala S.O, koramangala",
            "Mumbai City, mumbai",
            "  NEW   DELHI , new delhi",
            "Bombay, mumbai",
    })
    void comparesNamesTheWayPeopleWriteThem(String written, String expected) {
        assertThat(PlaceNames.canonical(written)).isEqualTo(expected);
    }

    @Test
    void nullIsEmpty() {
        assertThat(PlaceNames.canonical(null)).isEmpty();
    }

    @Test
    void differentPlacesStayDifferent() {
        assertThat(PlaceNames.canonical("Chennai")).isNotEqualTo(PlaceNames.canonical("Bengaluru"));
    }
}
