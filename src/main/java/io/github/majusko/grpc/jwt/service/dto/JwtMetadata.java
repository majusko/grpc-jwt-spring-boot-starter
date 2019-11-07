package io.github.majusko.grpc.jwt.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.crypto.SecretKey;
import java.util.List;

@Builder
@AllArgsConstructor
public class JwtMetadata {
    private Long expirationSec;
    private SecretKey key;
    private List<String> env;

    public Long getExpirationSec() {
        return expirationSec;
    }

    public SecretKey getKey() {
        return key;
    }

    public List<String> getEnv() {
        return env;
    }
}
