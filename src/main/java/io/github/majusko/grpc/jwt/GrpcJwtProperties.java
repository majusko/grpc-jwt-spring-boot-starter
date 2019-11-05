package io.github.majusko.grpc.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "grpc.jwt")
public class GrpcJwtProperties {
    private String secret = "default";
    private String algorithm = "HmacSHA256";
    private Long expirationSec = 3600L;
}