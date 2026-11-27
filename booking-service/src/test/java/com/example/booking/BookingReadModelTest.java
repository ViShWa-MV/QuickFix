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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The read endpoints the pages use: one summary per list (quote, code, review
 * together), and batched reviews for a results page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingReadModelTest {

    private static final String CUSTOMER = TestTokens.of(5L, "CUSTOMER");
    private static final String TECHNICIAN = TestTokens.of(4L, "TECHNICIAN");
    private static final String OTHER_CUSTOMER = TestTokens.of(6L, "CUSTOMER");
    private static final long PROFILE = 7L;

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


    private long nextSlot = 9_000;

    @BeforeEach
    void stubCatalog() {
        when(userService.getAddress(anyLong())).thenReturn(VERIFIED_ADDRESS);
        when(catalog.getServicePrice(anyLong())).thenReturn(300.0);
        when(catalog.getTechnician(PROFILE))
                .thenReturn(new CatalogClient.TechnicianView(PROFILE, 4L, "APPROVED"));
        when(catalog.technicianProfileIdFor(4L)).thenReturn(Optional.of(PROFILE));
        when(catalog.technicianProfileIdFor(6L)).thenReturn(Optional.empty());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", token);
    }

    private String book() throws Exception {
        String json = mvc.perform(auth(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":100,\"technicianProfileId\":" + PROFILE + ",\"serviceId\":3,\"slotId\":"
                                + (nextSlot++) + "}"), CUSTOMER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*?\"id\":(\\d+).*", "$1");
    }

    private void act(String id, String token, String action) throws Exception {
        mvc.perform(auth(post("/api/bookings/" + id + "/" + action), token)).andExpect(status().isOk());
    }

    @Test
    void customerSummaryIncludesTheCodeForTheCurrentMilestoneOnly() throws Exception {
        String id = book();
        act(id, TECHNICIAN, "accept");

        // Confirmed: no code yet.
        mvc.perform(auth(get("/api/bookings/customer/5/summary"), CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$[0].activeCode").doesNotExist());

        // On the way: the arrival code appears in the same response.
        act(id, TECHNICIAN, "en-route");
        act(id, TECHNICIAN, "arrive");
        mvc.perform(auth(get("/api/bookings/customer/5/summary"), CUSTOMER))
                .andExpect(jsonPath("$[0].activeCode.purpose").value("START"))
                .andExpect(jsonPath("$[0].activeCode.code").exists());
    }

    @Test
    void technicianSummaryNeverIncludesCodes() throws Exception {
        String id = book();
        act(id, TECHNICIAN, "accept");
        act(id, TECHNICIAN, "en-route");
        act(id, TECHNICIAN, "arrive");

        mvc.perform(auth(get("/api/bookings/technician/" + PROFILE + "/summary"), TECHNICIAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$[0].activeCode").doesNotExist());
    }

    @Test
    void summariesAreOnlyForTheirOwner() throws Exception {
        book();
        mvc.perform(auth(get("/api/bookings/customer/5/summary"), OTHER_CUSTOMER))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/bookings/technician/" + PROFILE + "/summary"), OTHER_CUSTOMER))
                .andExpect(status().isForbidden());
    }

    @Test
    void summaryCarriesTheReviewOnceLeft() throws Exception {
        String id = book();
        act(id, TECHNICIAN, "accept");
        act(id, TECHNICIAN, "en-route");
        act(id, TECHNICIAN, "arrive");
        String start = mvc.perform(auth(get("/api/bookings/" + id + "/otp?purpose=START"), CUSTOMER))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*?\"code\":\"(\\d+)\".*", "$1");
        mvc.perform(auth(post("/api/bookings/" + id + "/start")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + start + "\"}"), TECHNICIAN))
                .andExpect(status().isOk());
        act(id, TECHNICIAN, "request-completion");
        String done = mvc.perform(auth(get("/api/bookings/" + id + "/otp?purpose=COMPLETION"), CUSTOMER))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*?\"code\":\"(\\d+)\".*", "$1");
        mvc.perform(auth(post("/api/bookings/" + id + "/complete")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + done + "\"}"), TECHNICIAN))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/bookings/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"Neat\"}"), CUSTOMER))
                .andExpect(status().isCreated());

        mvc.perform(auth(get("/api/bookings/customer/5/summary"), CUSTOMER))
                .andExpect(jsonPath("$[0].review.rating").value(4))
                .andExpect(jsonPath("$[0].review.comment").value("Neat"))
                .andExpect(jsonPath("$[0].activeCode").doesNotExist());
    }

    @Test
    void recentReviewsForManyTechniciansComeInOneResponse() throws Exception {
        mvc.perform(get("/api/reviews/technicians/recent").param("ids", "7", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].technicianProfileId").value(7))
                .andExpect(jsonPath("$[0].reviews").isArray())
                .andExpect(jsonPath("$[1].technicianProfileId").value(8));
    }

    @Test
    void asksForTooManyTechniciansAreRefused() throws Exception {
        String ids = String.join(",", java.util.stream.LongStream.rangeClosed(1, 51)
                .mapToObj(String::valueOf).toList());
        mvc.perform(get("/api/reviews/technicians/recent").param("ids", ids))
                .andExpect(status().isBadRequest());
    }
}
