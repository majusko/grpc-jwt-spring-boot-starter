package io.github.majusko.grpc.jwt.service;

import com.google.common.collect.Sets;
import io.github.majusko.grpc.jwt.GrpcJwtProperties;
import io.github.majusko.grpc.jwt.service.dto.JwtData;
import io.github.majusko.grpc.jwt.service.dto.JwtMetadata;
import io.github.majusko.grpc.jwt.service.dto.JwtRoles;
import io.github.majusko.grpc.jwt.service.dto.JwtToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

public class JwtService {

    public static final String TOKEN_ENV = "token_env";
    public static final String JWT_ROLES = "jwt_roles";

    private static final String INTERNAL_ACCOUNT = "internal_account";
    private static final String INTERNAL_ROLE = "internal_role";

    private final Environment env;
    private final GrpcJwtProperties properties;

    private JwtMetadata metadata;
    private JwtToken internal;

    public JwtService(Environment env, GrpcJwtProperties properties) {
        this.env = env;
        this.properties = properties;
        this.metadata = JwtMetadata.builder()
            .env(Arrays.stream(env.getActiveProfiles()).collect(Collectors.toList()))
            .expirationSec(properties.getExpirationSec())
            .key(generateKey(properties.getSecret(), properties.getAlgorithm()))
            .build();
        this.internal = new JwtToken(
            generateJwt(new JwtData(INTERNAL_ACCOUNT, Sets.newHashSet(INTERNAL_ROLE)), metadata),
            LocalDateTime.now().plusSeconds(properties.getExpirationSec())
        );
    }

    public String generate(JwtData data) {
        return generateJwt(data, metadata);
    }

    public String getInternal(JwtData data) {
        if (LocalDateTime.now().minusSeconds(properties.getRefreshSec()).isAfter(internal.getExpiration())) {
            internal = new JwtToken(
                generateJwt(new JwtData(INTERNAL_ACCOUNT, Sets.newHashSet(INTERNAL_ROLE)), metadata),
                LocalDateTime.now().plusSeconds(properties.getExpirationSec())
            );
        }

        return internal.getToken();
    }

    public SecretKey getKey() {
        return metadata.getKey();
    }

    private SecretKeySpec generateKey(String signingSecret, String signAlgorithm) {
        final String sha256hex = DigestUtils.sha256Hex(signingSecret);
        final byte[] decodedKey = Base64.getDecoder().decode(sha256hex);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, signAlgorithm);
    }

    private String generateJwt(JwtData data, JwtMetadata metadata) {
        final LocalDateTime future = LocalDateTime.now().plusSeconds(metadata.getExpirationSec());
        final Claims ourClaims = Jwts.claims();

        ourClaims.put(JWT_ROLES, new JwtRoles(data.getRoles()));
        ourClaims.put(TOKEN_ENV, metadata.getEnv());

        return Jwts.builder()
            .setClaims(ourClaims)
            .setSubject(data.getAccountId())
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(future.atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(metadata.getKey()).compact();
    }
}
