package io.github.majusko.grpc.jwt.collector;

import java.util.Set;

public class Allowed {
    private final String method;
    private final String userId;
    private final Set<String> roles;

    public Allowed(String method, String userId, Set<String> roles) {
        this.method = method;
        this.userId = userId;
        this.roles = roles;
    }

    public String getMethod() {
        return method;
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
