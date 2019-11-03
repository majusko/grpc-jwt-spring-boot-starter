package io.github.majusko.grpc.jwt.interceptor;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class AuthContextData {
    private final String jwt;
    private final String userId;
    private final Set<String> roles;
    private final Claims jwtClaims;
}
