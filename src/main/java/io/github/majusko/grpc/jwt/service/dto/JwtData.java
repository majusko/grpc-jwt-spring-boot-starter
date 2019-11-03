package io.github.majusko.grpc.jwt.service.dto;

import java.util.Objects;
import java.util.Set;

public class JwtData {

    private final String accountId;
    private final Set<String> roles;

    public JwtData(String accountId, Set<String> roles) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(roles);
        this.accountId = accountId;
        this.roles = roles;
    }

    public String getAccountId() {
        return accountId;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
