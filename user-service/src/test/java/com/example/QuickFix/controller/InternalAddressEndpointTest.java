package com.example.QuickFix.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The internal address endpoint is reachable only with the shared service key. */
@SpringBootTest
@AutoConfigureMockMvc
class InternalAddressEndpointTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void withoutTheKeyTheRequestIsRefused() throws Exception {
        mvc.perform(get("/internal/addresses/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongKeyIsRefused() throws Exception {
        mvc.perform(get("/internal/addresses/1").header("X-Internal-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withTheKeyAMissingAddressIsNotFound() throws Exception {
        mvc.perform(get("/internal/addresses/999999").header("X-Internal-Key", "test-internal-key"))
                .andExpect(status().isNotFound());
    }
}
