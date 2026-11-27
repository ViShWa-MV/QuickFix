package com.example.QuickFix.service;

import com.example.QuickFix.entity.PincodeOffice;
import com.example.QuickFix.repository.PincodeRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Fixtures are written for these tests; they are not taken from the official list.
 * 560001 has two post offices in one district; 999999 is not in the list.
 */
@ExtendWith(MockitoExtension.class)
class AddressVerifierTest {

    @Mock PincodeRepo pincodes;

    private AddressVerifier verifier;

    private static PincodeOffice row(String pincode, String office, String district, String state) {
        PincodeOffice o = new PincodeOffice();
        o.setPincode(pincode);
        o.setOffice(office);
        o.setDistrict(district);
        o.setState(state);
        return o;
    }

    @BeforeEach
    void setUp() {
        verifier = new AddressVerifier(pincodes);
        lenient().when(pincodes.findByPincodeOrderByOfficeAsc("560001")).thenReturn(List.of(
                row("560001", "Bengaluru GPO", "Bengaluru Urban", "Karnataka"),
                row("560001", "Malleswaram S.O", "Bengaluru Urban", "Karnataka")));
        lenient().when(pincodes.findByPincodeOrderByOfficeAsc("999999")).thenReturn(List.of());
    }

    @Test
    void acceptsAPincodeWhoseStateAndCityMatchTheOfficialList() {
        AddressVerifier.VerifiedPlace v = verifier.verify("560001", "Karnataka", "Bengaluru Urban");
        assertThat(v.state()).isEqualTo("Karnataka");
        assertThat(v.district()).isEqualTo("Bengaluru Urban");
    }

    @Test
    void acceptsTheCityAsWritten_inAnotherSpelling() {
        // The official district is "Bengaluru Urban"; "Bangalore" is the old name.
        assertThat(verifier.verify("560001", "Karnataka", "Bangalore").district()).isEqualTo("Bengaluru Urban");
    }

    @Test
    void acceptsAPostOfficeNameAsTheCity() {
        assertThat(verifier.verify("560001", "Karnataka", "Malleswaram S.O").district()).isEqualTo("Bengaluru Urban");
        assertThat(verifier.verify("560001", "Karnataka", "Bengaluru GPO").district()).isEqualTo("Bengaluru Urban");
    }

    @Test
    void ignoresCaseAndSpacingInTheState() {
        assertThat(verifier.verify("560001", "  KARNATAKA ", "Bengaluru").state()).isEqualTo("Karnataka");
    }

    @Test
    void refusesAPincodeThatIsNotInTheOfficialList() {
        assertThatThrownBy(() -> verifier.verify("999999", "Karnataka", "Bengaluru"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("pincode:");
    }

    @Test
    void refusesAStateThatDoesNotMatchThePincode() {
        assertThatThrownBy(() -> verifier.verify("560001", "Tamil Nadu", "Bengaluru"))
                .hasMessageStartingWith("state:");
    }

    @Test
    void refusesACityThatIsNotTheDistrictOrAnOfficeOfThePincode() {
        assertThatThrownBy(() -> verifier.verify("560001", "Karnataka", "Chennai"))
                .hasMessageStartingWith("city:");
    }

    @Test
    void checksEveryPostOfficeOfAMultiOfficePincode() {
        when(pincodes.findByPincodeOrderByOfficeAsc(anyString())).thenReturn(List.of(
                row("110001", "Connaught Place S.O", "New Delhi", "Delhi"),
                row("110001", "Janpath S.O", "New Delhi", "Delhi")));
        assertThat(verifier.verify("110001", "Delhi", "Janpath").district()).isEqualTo("New Delhi");
    }
}
