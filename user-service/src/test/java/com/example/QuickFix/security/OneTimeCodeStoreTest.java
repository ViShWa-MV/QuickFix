package com.example.QuickFix.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimeCodeStoreTest {

    @Test
    void aCodeRedeemsForItsTokenExactlyOnce() {
        OneTimeCodeStore store = new OneTimeCodeStore();
        String code = store.issue("jwt-token");

        assertThat(store.redeem(code)).contains("jwt-token");
        assertThat(store.redeem(code)).isEmpty();
    }

    @Test
    void anUnknownOrMissingCodeRedeemsForNothing() {
        OneTimeCodeStore store = new OneTimeCodeStore();
        assertThat(store.redeem("not-a-code")).isEmpty();
        assertThat(store.redeem(null)).isEmpty();
    }

    @Test
    void codesAreUniqueAndUnguessableLength() {
        OneTimeCodeStore store = new OneTimeCodeStore();
        String a = store.issue("a");
        String b = store.issue("b");

        assertThat(a).isNotEqualTo(b);
        assertThat(a.length()).isGreaterThanOrEqualTo(40); // 32 random bytes, base64url
        assertThat(store.redeem(a)).contains("a");
        assertThat(store.redeem(b)).contains("b");
    }
}
