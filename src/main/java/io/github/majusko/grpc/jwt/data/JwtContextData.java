package io.github.majusko.grpc.jwt.data;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor
public class JwtContextData {
    private final String jwt;
    private final String userId;
    private final Set<String> roles;
    private final Claims jwtClaims;

    public String getJwt() {
        return jwt;
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Claims getJwtClaims() {
        return jwtClaims;
    }
}
