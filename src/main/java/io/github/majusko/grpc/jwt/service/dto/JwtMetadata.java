package io.github.majusko.grpc.jwt.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtMetadata {
    private Long expirationSec;
    private SecretKey key;
    private List<String> env;
}
