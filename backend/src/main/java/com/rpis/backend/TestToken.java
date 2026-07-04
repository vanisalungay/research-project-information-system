package com.rpis.backend;

import com.rpis.backend.service.TokenService;

public class TestToken {
    public static void main(String[] args) {
        TokenService ts = new TokenService();
        String token = ts.generateToken(1L, "PROPONENT");
        System.out.println("Token: " + token);
        boolean valid = ts.validateToken(token);
        System.out.println("Valid: " + valid);
    }
}
