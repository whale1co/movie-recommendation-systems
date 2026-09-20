package com.movierec.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {
    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 900000));
    }

    @Test
    void rejectsLongPlaceholderSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil("replace-with-a-random-secret-at-least-32-bytes-long", 900000));
    }

    @Test
    void acceptsStrongExternalSecret() {
        assertDoesNotThrow(() -> new JwtUtil("M7!qP2#vN9@xR4$kT8&zC5*wH3_yL6+f", 900000));
    }
}
