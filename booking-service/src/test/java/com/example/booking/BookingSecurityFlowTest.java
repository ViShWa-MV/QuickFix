package com.example.booking;

import com.example.booking.client.CatalogClient;
import com.example.booking.client.UserServiceClient;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Booking rules over HTTP: who may do each step, identity taken from the token,
 * the OTP visibility rule, and the review rules.
 *
 * <p>catalog-service is replaced by a mock. Each test books its own slot, so the
 * tests can run in any order against the shared in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingSecurityFlowTest {

    private static final String ADMIN = TestTokens.of(1L, "ADMIN");
    private static final String CUSTOMER = TestTokens.of(5L, "CUSTOMER");
    private static final String OTHER_CUSTOMER = TestTokens.of(6L, "CUSTOMER");
    private static final String TECHNICIAN = TestTokens.of(4L, "TECHNICIAN");      // profile 7
    private static final String OTHER_TECHNICIAN = TestTokens.of(8L, "TECHNICIAN"); // no profile
    private static final long PROFILE = 7L;
    private static final long PENDING_PROFILE = 9L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogClient catalog;

    @MockitoBean
    private UserServiceClient userService;
    /** user-service: address 100 belongs to customer 5 and is verified. */
    private static final UserServiceClient.AddressSnapshot VERIFIED_ADDRESS =
            new UserServiceClient.AddressSnapshot(100L, 5L, "Home", "Flat 4B", "Central Bengaluru",
                    "MG Road", "Bengaluru", "Karnataka", "560001", 12.9756, 77.6066, "VERIFIED");


    private long nextSlot = 500;

    @BeforeEach
    void stubCatalog() {
        when(userService.getAddress(anyLong())).thenReturn(VERIFIED_ADDRESS);
        when(catalog.getServicePrice(anyLong())).thenReturn(499.0);
        when(catalog.getTechnician(PROFILE))
                .thenReturn(new CatalogClient.TechnicianView(PROFILE, 4L, "APPROVED"));
        when(catalog.getTechnician(PENDING_PROFILE))
                .thenReturn(new CatalogClient.TechnicianView(PENDING_PROFILE, 7L, "PENDING"));
        when(catalog.technicianProfileIdFor(4L)).thenReturn(Optional.of(PROFILE));
        when(catalog.technicianProfileIdFor(8L)).thenReturn(Optional.empty());
    }

    // ---------- helpers ----------

    private ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", token);
        }
        return mvc.perform(request);
    }

    private ResultActions postJson(String url, String token, String json) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private static String idOf(String json) {
        return json.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private static String field(String json, String name) {
        return json.replaceAll(".*\"" + name + "\":\"?([^\",}]+)\"?.*", "$1");
    }

    private String book(long profile) throws Exception {
        long slot = nextSlot++;
        String body = postJson("/api/bookings", CUSTOMER,
                "{\"addressId\":100,\"technicianProfileId\":" + profile + ",\"serviceId\":3,\"slotId\":" + slot + "}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    /** Drives a booking to IN_PROGRESS through the arrival code. */
    private void startWork(String bookingId) throws Exception {
        postJson("/api/bookings/" + bookingId + "/accept", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + bookingId + "/en-route", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + bookingId + "/arrive", TECHNICIAN, "").andExpect(status().isOk());
        String code = field(send(get("/api/bookings/" + bookingId + "/otp?purpose=START"), CUSTOMER)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "code");
        postJson("/api/bookings/" + bookingId + "/start", TECHNICIAN, "{\"otp\":\"" + code + "\"}")
                .andExpect(status().isOk());
    }

    // ---------- anonymous and role gates ----------

    @Test
    void anonymousCannotBook() throws Exception {
        postJson("/api/bookings", null, "{\"addressId\":100,\"technicianProfileId\":7,\"serviceId\":3,\"slotId\":1}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void technicianCannotBook() throws Exception {
        postJson("/api/bookings", TECHNICIAN, "{\"addressId\":100,\"technicianProfileId\":7,\"serviceId\":3,\"slotId\":1}")
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingUsesTheTokenCustomerNotTheBodyClaim() throws Exception {
        long slot = nextSlot++;
        postJson("/api/bookings", CUSTOMER,
                "{\"customerId\":999,\"addressId\":100,\"technicianProfileId\":7,\"serviceId\":3,\"slotId\":" + slot + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(5));
        verify(catalog).holdSlot(slot, PROFILE);
    }

    @Test
    void aTechnicianWhoIsNotApprovedCannotBeBooked() throws Exception {
        postJson("/api/bookings", CUSTOMER,
                "{\"addressId\":100,\"technicianProfileId\":" + PENDING_PROFILE + ",\"serviceId\":3,\"slotId\":" + nextSlot++ + "}")
                .andExpect(status().isConflict());
    }

    // ---------- ownership of a booking ----------

    @Test
    void anotherCustomerCannotReadTheBooking() throws Exception {
        String id = book(PROFILE);
        send(get("/api/bookings/" + id), OTHER_CUSTOMER).andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyBookingButCannotActOnIt() throws Exception {
        String id = book(PROFILE);
        send(get("/api/bookings/" + id), ADMIN).andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/accept", ADMIN, "").andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAcceptTheirOwnBooking() throws Exception {
        String id = book(PROFILE);
        postJson("/api/bookings/" + id + "/accept", CUSTOMER, "").andExpect(status().isForbidden());
    }

    @Test
    void anotherTechnicianCannotAcceptTheBooking() throws Exception {
        String id = book(PROFILE);
        postJson("/api/bookings/" + id + "/accept", OTHER_TECHNICIAN, "").andExpect(status().isForbidden());
    }

    @Test
    void technicianCannotReadAnotherTechniciansJobList() throws Exception {
        send(get("/api/bookings/technician/" + PROFILE), OTHER_TECHNICIAN).andExpect(status().isForbidden());
    }

    @Test
    void bookingErrorsNeverContainAStackTrace() throws Exception {
        String id = book(PROFILE);
        send(get("/api/bookings/" + id), OTHER_CUSTOMER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- OTP visibility ----------

    @Test
    void onlyTheCustomerCanReadTheArrivalCode() throws Exception {
        String id = book(PROFILE);
        postJson("/api/bookings/" + id + "/accept", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/en-route", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/arrive", TECHNICIAN, "").andExpect(status().isOk());

        send(get("/api/bookings/" + id + "/otp?purpose=START"), CUSTOMER).andExpect(status().isOk());
        send(get("/api/bookings/" + id + "/otp?purpose=START"), TECHNICIAN).andExpect(status().isForbidden());
        send(get("/api/bookings/" + id + "/otp?purpose=START"), ADMIN).andExpect(status().isForbidden());
    }

    @Test
    void aWrongArrivalCodeIsRefused() throws Exception {
        String id = book(PROFILE);
        postJson("/api/bookings/" + id + "/accept", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/en-route", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/arrive", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/start", TECHNICIAN, "{\"otp\":\"000000\"}")
                .andExpect(status().isBadRequest());
    }

    // ---------- full flow ----------

    @Test
    void theWholeJobFlowCompletesWithBothCodes() throws Exception {
        String id = book(PROFILE);
        startWork(id);

        postJson("/api/bookings/" + id + "/request-completion", TECHNICIAN, "").andExpect(status().isOk());
        String code = field(send(get("/api/bookings/" + id + "/otp?purpose=COMPLETION"), CUSTOMER)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "code");
        postJson("/api/bookings/" + id + "/complete", TECHNICIAN, "{\"otp\":\"" + code + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void aPendingQuoteBlocksCompletion() throws Exception {
        String id = book(PROFILE);
        startWork(id);
        postJson("/api/bookings/" + id + "/quote", TECHNICIAN,
                "{\"lineItems\":[{\"description\":\"capacitor\",\"amount\":150}]}")
                .andExpect(status().isCreated());
        postJson("/api/bookings/" + id + "/request-completion", TECHNICIAN, "").andExpect(status().isOk());
        postJson("/api/bookings/" + id + "/complete", TECHNICIAN, "{\"otp\":\"000000\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheCustomerCanApproveAQuote() throws Exception {
        String id = book(PROFILE);
        startWork(id);
        postJson("/api/bookings/" + id + "/quote", TECHNICIAN,
                "{\"lineItems\":[{\"description\":\"capacitor\",\"amount\":150}]}")
                .andExpect(status().isCreated());
        postJson("/api/bookings/" + id + "/quote/approve", TECHNICIAN, "").andExpect(status().isForbidden());
        postJson("/api/bookings/" + id + "/quote/approve", CUSTOMER, "").andExpect(status().isOk());
    }

    // ---------- reviews ----------

    @Test
    void reviewIsRefusedBeforeTheJobIsCompleted() throws Exception {
        String id = book(PROFILE);
        postJson("/api/bookings/" + id + "/review", CUSTOMER, "{\"rating\":5}")
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheCustomerReviewsAndTheRatingReachesCatalog() throws Exception {
        String id = book(PROFILE);
        startWork(id);
        postJson("/api/bookings/" + id + "/request-completion", TECHNICIAN, "").andExpect(status().isOk());
        String code = field(send(get("/api/bookings/" + id + "/otp?purpose=COMPLETION"), CUSTOMER)
                .andReturn().getResponse().getContentAsString(), "code");
        postJson("/api/bookings/" + id + "/complete", TECHNICIAN, "{\"otp\":\"" + code + "\"}")
                .andExpect(status().isOk());

        postJson("/api/bookings/" + id + "/review", TECHNICIAN, "{\"rating\":5}")
                .andExpect(status().isForbidden());
        postJson("/api/bookings/" + id + "/review", OTHER_CUSTOMER, "{\"rating\":5}")
                .andExpect(status().isForbidden());

        postJson("/api/bookings/" + id + "/review", CUSTOMER,
                "{\"customerId\":999,\"rating\":4,\"comment\":\"Good\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(5));
        verify(catalog).addRating(PROFILE, 4);

        postJson("/api/bookings/" + id + "/review", CUSTOMER, "{\"rating\":3}")
                .andExpect(status().isConflict());
    }

    @Test
    void publicReviewListNeedsNoToken() throws Exception {
        send(get("/api/reviews/technician/" + PROFILE), null).andExpect(status().isOk());
    }
}
