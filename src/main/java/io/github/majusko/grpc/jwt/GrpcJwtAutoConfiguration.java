package io.github.majusko.grpc.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JwtAuthInterceptor.class)
@EnableConfigurationProperties(GrpcJwtProperties.class)
public class GrpcJwtAutoConfiguration {

    private final GrpcJwtProperties grpcJwtProperties;

    public GrpcJwtAutoConfiguration(GrpcJwtProperties grpcJwtProperties) {
        this.grpcJwtProperties = grpcJwtProperties;
    }

    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor();
    }
}
