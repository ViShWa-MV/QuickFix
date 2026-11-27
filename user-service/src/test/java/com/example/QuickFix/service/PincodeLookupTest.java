package com.example.QuickFix.service;

import com.example.QuickFix.DTO.PincodeInfo;
import com.example.QuickFix.entity.PincodeOffice;
import com.example.QuickFix.exception.ResourceNotFoundException;
import com.example.QuickFix.repository.PincodeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PincodeLookupTest {

    @Mock PincodeRepo pincodes;

    private static PincodeOffice row(String office) {
        PincodeOffice o = new PincodeOffice();
        o.setPincode("560001");
        o.setOffice(office);
        o.setDistrict("Bengaluru Urban");
        o.setState("Karnataka");
        return o;
    }

    @Test
    void returnsTheStateDistrictAndEveryOffice() {
        when(pincodes.count()).thenReturn(2L);
        when(pincodes.findByPincodeOrderByOfficeAsc("560001"))
                .thenReturn(List.of(row("Bengaluru GPO"), row("Malleswaram S.O")));

        PincodeInfo info = new PincodeLookup(pincodes).lookup("560001");

        assertThat(info.state()).isEqualTo("Karnataka");
        assertThat(info.district()).isEqualTo("Bengaluru Urban");
        assertThat(info.offices()).containsExactly("Bengaluru GPO", "Malleswaram S.O");
    }

    @Test
    void whenTheListWasNeverLoadedTheAnswerSaysSoInsteadOfCallingThePincodeInvalid() {
        when(pincodes.count()).thenReturn(0L);
        assertThatThrownBy(() -> new PincodeLookup(pincodes).lookup("560001"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("has not been loaded");
    }

    @Test
    void anUnknownPincodeIsNotFound() {
        when(pincodes.count()).thenReturn(2L);
        when(pincodes.findByPincodeOrderByOfficeAsc("999999")).thenReturn(List.of());
        assertThatThrownBy(() -> new PincodeLookup(pincodes).lookup("999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
