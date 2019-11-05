package io.github.majusko.grpc.jwt.service.dto;

import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

public class JwtData {

    private final String userId;
    private final Set<String> roles;

    public JwtData(String userId, String role) {
        this(userId, Sets.newHashSet(Objects.requireNonNull(role)));
    }

    public JwtData(String userId, Set<String> roles) {
        this.userId = Objects.requireNonNull(userId);
        this.roles = Objects.requireNonNull(roles);
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
