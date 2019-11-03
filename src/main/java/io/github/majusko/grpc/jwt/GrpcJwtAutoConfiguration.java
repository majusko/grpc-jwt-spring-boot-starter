package io.github.majusko.grpc.jwt;

import io.github.majusko.grpc.jwt.collector.AllowedCollector;
import io.github.majusko.grpc.jwt.service.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(GrpcJwtProperties.class)
public class GrpcJwtAutoConfiguration {

    private final Environment environment;
    private final GrpcJwtProperties grpcJwtProperties;

    public GrpcJwtAutoConfiguration(
        Environment environment,
        GrpcJwtProperties grpcJwtProperties,
        AllowedCollector allowedCollector
    ) {
        this.environment = environment;
        this.grpcJwtProperties = grpcJwtProperties;
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService(environment, grpcJwtProperties);
    }
}
