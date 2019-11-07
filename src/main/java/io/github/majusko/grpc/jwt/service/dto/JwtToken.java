package io.github.majusko.grpc.jwt.service.dto;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class JwtToken {
    private String token;
    private LocalDateTime expiration;

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }
}
