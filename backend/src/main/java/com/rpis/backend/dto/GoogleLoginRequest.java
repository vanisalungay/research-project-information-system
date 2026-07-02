package com.rpis.backend.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String token;
    private String role;
}
