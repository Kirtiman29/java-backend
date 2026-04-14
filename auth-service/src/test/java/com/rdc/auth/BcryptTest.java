package com.rdc.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    @Test
    public void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("HASH_2020: " + encoder.encode("Admin@Rdc2020"));
        System.out.println("HASH_2026: " + encoder.encode("Admin@Rdc2026"));
    }
}
