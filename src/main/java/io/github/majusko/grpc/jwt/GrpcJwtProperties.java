package io.github.majusko.grpc.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "grpc.jwt")
public class GrpcJwtProperties {
    private String secret;
    private String algorithm;
    private Long expirationSec;
    private Long refreshSec;
}