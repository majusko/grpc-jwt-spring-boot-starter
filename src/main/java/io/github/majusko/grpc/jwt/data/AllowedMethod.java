package io.github.majusko.grpc.jwt.data;

import java.util.Objects;
import java.util.Set;

public class AllowedMethod {
    private final String method;
    private final String ownerField;
    private final Set<String> roles;

    public AllowedMethod(String method, String ownerField, Set<String> roles) {
        this.method = Objects.requireNonNull(method);
        this.ownerField = Objects.requireNonNull(ownerField);
        this.roles = Objects.requireNonNull(roles);
    }

    public String getMethod() {
        return method;
    }

    public String getOwnerField() {
        return ownerField;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
