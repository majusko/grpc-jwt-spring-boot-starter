package io.github.majusko.grpc.jwt.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class JwtToken {
    private String token;
    private LocalDateTime expiration;
}
