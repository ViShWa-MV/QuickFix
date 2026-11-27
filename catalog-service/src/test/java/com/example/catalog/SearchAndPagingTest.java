package com.example.catalog;

import com.example.catalog.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Search runs in one query: it only returns approved technicians with a free
 * slot in the window, ranked by rating. Lists are paged.
 *
 * <p>Each test uses its own pincode, so results from other tests never mix in.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchAndPagingTest {

    private static final String INTERNAL_KEY = "test-internal-key";
    private static final String ADMIN = TestTokens.of(1L, "ADMIN");
    private static final AtomicLong USER_IDS = new AtomicLong(50_000);
    private static final AtomicLong PINCODES = new AtomicLong(100_000);

    @Autowired
    private MockMvc mvc;

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header("Authorization", token);
    }

    private long createCategory() throws Exception {
        String json = mvc.perform(withAuth(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cat-" + USER_IDS.incrementAndGet() + "\"}"), ADMIN))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private static long idOf(String json) {
        return Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    /** A technician who offers the category in the pincode, auto-approved, with an open slot. */
    private long technician(long categoryId, String pincode, String name, String day) throws Exception {
        long userId = USER_IDS.incrementAndGet();
        String token = TestTokens.of(userId, "TECHNICIAN");
        String profile = mvc.perform(withAuth(post("/api/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + name + "\",\"skills\":[" + categoryId
                                + "],\"serviceAreas\":[\"" + pincode + "\"],\"yearsExperience\":4}"), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value(name))
                .andReturn().getResponse().getContentAsString();
        long profileId = idOf(profile);
        mvc.perform(withAuth(post("/api/technicians/" + profileId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startAt\":\"" + day + "T10:00:00\",\"endAt\":\"" + day + "T11:00:00\"}"), token))
                .andExpect(status().isCreated());
        return profileId;
    }

    private void rate(long profileId, int rating) throws Exception {
        mvc.perform(post("/internal/technicians/" + profileId + "/ratings")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":" + rating + "}"))
                .andExpect(status().isNoContent());
    }

    private static String day(int offset) {
        return java.time.LocalDate.now().plusDays(offset).toString();
    }

    @Test
    void searchReturnsOnlyTechniciansWithAFreeSlotInTheWindow() throws Exception {
        long category = createCategory();
        String pincode = "P" + PINCODES.incrementAndGet();
        String d = day(400);
        long free = technician(category, pincode, "Free Tech", d);

        mvc.perform(get("/api/technicians/search")
                        .param("category", String.valueOf(category))
                        .param("pincode", pincode)
                        .param("from", d + "T10:30:00")
                        .param("to", d + "T11:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(free));

        // Same technician, a window with no overlap: not returned.
        mvc.perform(get("/api/technicians/search")
                        .param("category", String.valueOf(category))
                        .param("pincode", pincode)
                        .param("from", d + "T12:00:00")
                        .param("to", d + "T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchRanksByRatingFirst() throws Exception {
        long category = createCategory();
        String pincode = "P" + PINCODES.incrementAndGet();
        String d = day(410);
        long lower = technician(category, pincode, "Lower", d);
        long higher = technician(category, pincode, "Higher", d);
        rate(lower, 3);
        rate(higher, 5);

        mvc.perform(get("/api/technicians/search")
                        .param("category", String.valueOf(category))
                        .param("pincode", pincode)
                        .param("from", d + "T10:00:00")
                        .param("to", d + "T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(higher))
                .andExpect(jsonPath("$[1].id").value(lower));
    }

    @Test
    void searchRejectsAnEmptyOrInvertedWindow() throws Exception {
        String d = day(420);
        mvc.perform(get("/api/technicians/search")
                        .param("category", "1")
                        .param("pincode", "560001")
                        .param("from", d + "T11:00:00")
                        .param("to", d + "T10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsHonourThePageSize() throws Exception {
        createCategory();
        createCategory();
        mvc.perform(get("/api/categories").param("size", "1").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void aTechnicianProfileKeepsItsDisplayName() throws Exception {
        long category = createCategory();
        long profile = technician(category, "P" + PINCODES.incrementAndGet(), "Ravi K", day(430));
        mvc.perform(get("/api/technicians/" + profile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ravi K"));
    }

    @Test
    void displayNameIsRequired() throws Exception {
        long userId = USER_IDS.incrementAndGet();
        mvc.perform(withAuth(post("/api/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[1],\"serviceAreas\":[\"560001\"],\"yearsExperience\":1}"),
                        TestTokens.of(userId, "TECHNICIAN")))
                .andExpect(status().isBadRequest());
    }
}
