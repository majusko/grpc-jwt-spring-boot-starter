package io.github.majusko.grpc.jwt.service.dto;

import java.util.Objects;
import java.util.Set;

public class JwtData {

    private final String userId;
    private final Set<String> roles;

    public JwtData(String userId, Set<String> roles) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(roles);
        this.userId = userId;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
