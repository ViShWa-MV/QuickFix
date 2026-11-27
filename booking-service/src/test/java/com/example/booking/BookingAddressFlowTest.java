package com.example.booking;

import com.example.booking.client.CatalogClient;
import com.example.booking.client.UserServiceClient;
import com.example.booking.exception.AddressUnavailableException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Booking at a verified address, and who may see it. user-service is a mock.
 * Customer 5 owns address 100 (verified); address 201 is unverified; address 202
 * belongs to customer 6; 999 does not exist; 998 cannot be reached.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingAddressFlowTest {

    private static final String CUSTOMER = TestTokens.of(5L, "CUSTOMER");
    private static final String OTHER_CUSTOMER = TestTokens.of(6L, "CUSTOMER");
    private static final String TECHNICIAN = TestTokens.of(4L, "TECHNICIAN");       // profile 7
    private static final String OTHER_TECHNICIAN = TestTokens.of(8L, "TECHNICIAN"); // no profile
    private static final String ADMIN = TestTokens.of(1L, "ADMIN");
    private static final long PROFILE = 7L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogClient catalog;

    @MockitoBean
    private UserServiceClient userService;

    private long nextSlot = 7_000;

    private static UserServiceClient.AddressSnapshot address(long id, long owner, String status) {
        return new UserServiceClient.AddressSnapshot(id, owner, "Home", "Flat 4B", "Central Bengaluru",
                "MG Road", "Bengaluru", "Karnataka", "560001", 12.9756, 77.6066, status);
    }

    @BeforeEach
    void stub() {
        when(catalog.getServicePrice(anyLong())).thenReturn(499.0);
        when(catalog.getTechnician(PROFILE))
                .thenReturn(new CatalogClient.TechnicianView(PROFILE, 4L, "APPROVED"));
        when(catalog.technicianProfileIdFor(4L)).thenReturn(Optional.of(PROFILE));
        when(catalog.technicianProfileIdFor(8L)).thenReturn(Optional.empty());

        when(userService.getAddress(anyLong())).thenReturn(address(100L, 5L, "VERIFIED"));
        when(userService.getAddress(201L)).thenReturn(address(201L, 5L, "UNVERIFIED"));
        when(userService.getAddress(202L)).thenReturn(address(202L, 6L, "VERIFIED"));
        when(userService.getAddress(999L)).thenThrow(new ResourceNotFoundException("Address not found"));
        when(userService.getAddress(998L)).thenThrow(new AddressUnavailableException("down", null));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header("Authorization", token));
    }

    private ResultActions book(String token, long addressId) throws Exception {
        return send(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + ",\"technicianProfileId\":" + PROFILE
                        + ",\"serviceId\":3,\"slotId\":" + (nextSlot++) + "}"), token);
    }

    private String bookVerified() throws Exception {
        String json = book(CUSTOMER, 100L).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*?\"id\":(\\d+).*", "$1");
    }

    @Test
    void anUnverifiedAddressCannotBeBooked() throws Exception {
        book(CUSTOMER, 201L).andExpect(status().isConflict());
        verify(catalog, never()).holdSlot(any(), any());
    }

    @Test
    void anAddressBelongingToSomeoneElseCannotBeBooked() throws Exception {
        book(CUSTOMER, 202L).andExpect(status().isForbidden());
        verify(catalog, never()).holdSlot(any(), any());
    }

    @Test
    void aMissingAddressIsNotFound() throws Exception {
        book(CUSTOMER, 999L).andExpect(status().isNotFound());
    }

    @Test
    void userServiceDownMeansTheBookingIsNotMade() throws Exception {
        book(CUSTOMER, 998L).andExpect(status().isBadGateway());
        verify(catalog, never()).holdSlot(any(), any());
    }

    @Test
    void theCustomerSeesTheFullAddressAtAnyStage() throws Exception {
        String id = bookVerified();
        send(get("/api/bookings/" + id + "/address"), CUSTOMER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("FULL"))
                .andExpect(jsonPath("$.streetName").value("MG Road"))
                .andExpect(jsonPath("$.addressLine").value("Flat 4B"))
                .andExpect(jsonPath("$.latitude").value(12.9756));
    }

    @Test
    void theTechnicianSeesOnlyTheAreaBeforeAccepting() throws Exception {
        String id = bookVerified();
        send(get("/api/bookings/" + id + "/address"), TECHNICIAN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("AREA"))
                .andExpect(jsonPath("$.city").value("Bengaluru"))
                .andExpect(jsonPath("$.pincode").value("560001"))
                .andExpect(jsonPath("$.streetName").doesNotExist())
                .andExpect(jsonPath("$.addressLine").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist());
    }

    @Test
    void theTechnicianSeesTheFullAddressOnceAccepted() throws Exception {
        String id = bookVerified();
        send(post("/api/bookings/" + id + "/accept"), TECHNICIAN).andExpect(status().isOk());
        send(get("/api/bookings/" + id + "/address"), TECHNICIAN)
                .andExpect(jsonPath("$.visibility").value("FULL"))
                .andExpect(jsonPath("$.streetName").value("MG Road"));
    }

    @Test
    void anAdminSeesOnlyTheArea() throws Exception {
        String id = bookVerified();
        send(post("/api/bookings/" + id + "/accept"), TECHNICIAN).andExpect(status().isOk());
        send(get("/api/bookings/" + id + "/address"), ADMIN)
                .andExpect(jsonPath("$.visibility").value("AREA"))
                .andExpect(jsonPath("$.streetName").doesNotExist());
    }

    @Test
    void otherPeopleCannotReadTheAddressAtAll() throws Exception {
        String id = bookVerified();
        send(get("/api/bookings/" + id + "/address"), OTHER_CUSTOMER).andExpect(status().isForbidden());
        send(get("/api/bookings/" + id + "/address"), OTHER_TECHNICIAN).andExpect(status().isForbidden());
    }

    @Test
    void theTechnicianJobListShowsTheAreaBeforeAcceptAndTheFullAddressAfter() throws Exception {
        String id = bookVerified();
        send(get("/api/bookings/technician/" + PROFILE + "/summary"), TECHNICIAN)
                .andExpect(jsonPath("$[0].address.visibility").value("AREA"));
        send(post("/api/bookings/" + id + "/accept"), TECHNICIAN).andExpect(status().isOk());
        send(get("/api/bookings/technician/" + PROFILE + "/summary"), TECHNICIAN)
                .andExpect(jsonPath("$[0].address.visibility").value("FULL"))
                .andExpect(jsonPath("$[0].address.streetName").value("MG Road"));
    }
}
