package com.example.catalog;

import com.example.catalog.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authorization and ownership rules of catalog-service, over HTTP.
 *
 * <p>Each test uses its own user ids and slot dates, so the tests can run in any
 * order against the shared in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogSecurityTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mvc;

    // Static: each test method gets a new instance, but the database is shared.
    private static final java.util.concurrent.atomic.AtomicLong NEXT_USER = new java.util.concurrent.atomic.AtomicLong(1_000);

    // ---------- helpers ----------

    private long newUserId() {
        return NEXT_USER.getAndIncrement();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", token);
        }
        return mvc.perform(request);
    }

    private ResultActions postJson(String url, String token, String json) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private ResultActions patchJson(String url, String token, String json) throws Exception {
        return send(patch(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private ResultActions putJson(String url, String token, String json) throws Exception {
        return send(put(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    /** Registers a technician profile for this user (auto-approved in tests) and returns its id. */
    private long technicianProfile(long userId) throws Exception {
        String token = TestTokens.of(userId, "TECHNICIAN");
        postJson("/api/technicians", token,
                "{\"displayName\":\"Test Tech\",\"skills\":[1],\"serviceAreas\":[\"560001\"],\"yearsExperience\":3}");
        String body = send(get("/api/technicians/by-user/" + userId), token)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    /** A slot far in the future, unique per call so slots never clash. */
    private String freshSlotJson() {
        LocalDate day = LocalDate.now().plusDays(10 + (long) (Math.random() * 3000));
        return "{\"startAt\":\"" + day + "T08:00:00\",\"endAt\":\"" + day + "T09:00:00\"}";
    }

    private static final String ADMIN = TestTokens.of(1L, "ADMIN");
    private static final String CUSTOMER = TestTokens.of(5L, "CUSTOMER");

    // ---------- anonymous and role gates ----------

    @Test
    void anonymousCannotCreateACategory() throws Exception {
        postJson("/api/categories", null, "{\"name\":\"x\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotCreateACategory() throws Exception {
        postJson("/api/categories", CUSTOMER, "{\"name\":\"x\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateACategory() throws Exception {
        postJson("/api/categories", ADMIN, "{\"name\":\"Cat " + UUID.randomUUID() + "\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void anyoneCanReadServices() throws Exception {
        send(get("/api/services"), null).andExpect(status().isOk());
    }

    @Test
    void aGarbageTokenIsTreatedAsAnonymous() throws Exception {
        send(get("/api/technicians/pending"), "Bearer not-a-real-token")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotRegisterATechnicianProfile() throws Exception {
        postJson("/api/technicians", CUSTOMER,
                "{\"displayName\":\"Test Tech\",\"skills\":[1],\"serviceAreas\":[\"560001\"],\"yearsExperience\":1}")
                .andExpect(status().isForbidden());
    }

    @Test
    void technicianRegistersOnlyForThemselvesAndTwiceIsRejected() throws Exception {
        long userId = newUserId();
        String token = TestTokens.of(userId, "TECHNICIAN");
        String body = "{\"displayName\":\"Test Tech\",\"skills\":[1],\"serviceAreas\":[\"560001\"],\"yearsExperience\":3}";

        postJson("/api/technicians", token, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId));

        postJson("/api/technicians", token, body)
                .andExpect(status().isConflict());
    }

    @Test
    void technicianCannotApproveTheirOwnProfile() throws Exception {
        long userId = newUserId();
        long profileId = technicianProfile(userId);

        patchJson("/api/technicians/" + profileId + "/verification",
                TestTokens.of(userId, "TECHNICIAN"), "{\"status\":\"APPROVED\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminsReadThePendingQueue() throws Exception {
        long userId = newUserId();
        technicianProfile(userId);

        send(get("/api/technicians/pending"), ADMIN).andExpect(status().isOk());
        send(get("/api/technicians/pending"), TestTokens.of(userId, "TECHNICIAN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRejectAProfile() throws Exception {
        long profileId = technicianProfile(newUserId());

        patchJson("/api/technicians/" + profileId + "/verification", ADMIN,
                "{\"status\":\"REJECTED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));
    }

    // ---------- ownership ----------

    @Test
    void technicianCannotEditAnotherTechniciansProfile() throws Exception {
        long owner = newUserId();
        long other = newUserId();
        long profileId = technicianProfile(owner);

        putJson("/api/technicians/" + profileId, TestTokens.of(other, "TECHNICIAN"),
                "{\"displayName\":\"Test Tech\",\"skills\":[1],\"serviceAreas\":[\"560001\"],\"yearsExperience\":9}")
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanEditTheirProfile() throws Exception {
        long owner = newUserId();
        long profileId = technicianProfile(owner);

        putJson("/api/technicians/" + profileId, TestTokens.of(owner, "TECHNICIAN"),
                "{\"displayName\":\"Test Tech\",\"skills\":[1],\"serviceAreas\":[\"560002\"],\"yearsExperience\":9}")
                .andExpect(status().isOk());
    }

    @Test
    void technicianCannotReadAnotherUsersProfileByUserId() throws Exception {
        long owner = newUserId();
        long other = newUserId();
        technicianProfile(owner);

        send(get("/api/technicians/by-user/" + owner), TestTokens.of(other, "TECHNICIAN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void errorBodiesNeverContainAStackTrace() throws Exception {
        send(get("/api/technicians/pending"), TestTokens.of(newUserId(), "TECHNICIAN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- slots: who publishes, and the internal hold ----------

    @Test
    void ownerPublishesSlotsAndOthersCannot() throws Exception {
        long owner = newUserId();
        long other = newUserId();
        long profileId = technicianProfile(owner);

        postJson("/api/technicians/" + profileId + "/slots", TestTokens.of(owner, "TECHNICIAN"), freshSlotJson())
                .andExpect(status().isCreated());

        postJson("/api/technicians/" + profileId + "/slots", TestTokens.of(other, "TECHNICIAN"), freshSlotJson())
                .andExpect(status().isForbidden());
    }

    @Test
    void publicCanReadASlotsList() throws Exception {
        long profileId = technicianProfile(newUserId());
        send(get("/api/technicians/" + profileId + "/slots"), null).andExpect(status().isOk());
    }

    @Test
    void internalEndpointsRequireTheInternalKey() throws Exception {
        mvc.perform(post("/internal/slots/1/hold?profileId=1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/slots/1/hold?profileId=1").header("X-Internal-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalHoldOnlyWorksForTheProfileThatPublishedTheSlot() throws Exception {
        long owner = newUserId();
        long profileId = technicianProfile(owner);
        String slotBody = postJson("/api/technicians/" + profileId + "/slots",
                TestTokens.of(owner, "TECHNICIAN"), freshSlotJson())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long slotId = Long.parseLong(slotBody.replaceAll(".*\"id\":(\\d+).*", "$1"));

        long wrongProfile = profileId + 100_000;
        mvc.perform(post("/internal/slots/" + slotId + "/hold?profileId=" + wrongProfile)
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isNotFound());

        mvc.perform(post("/internal/slots/" + slotId + "/hold?profileId=" + profileId)
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk());

        // Already held: a second hold must fail, not double-book.
        mvc.perform(post("/internal/slots/" + slotId + "/hold?profileId=" + profileId)
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isConflict());
    }
}
