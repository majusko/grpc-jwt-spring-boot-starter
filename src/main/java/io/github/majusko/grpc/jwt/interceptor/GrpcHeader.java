package io.github.majusko.grpc.jwt.interceptor;

import io.grpc.Metadata;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class GrpcHeader {

    private final static String AUTHORIZATION_KEY = "Authorization";
    public static Metadata.Key<String> AUTHORIZATION =
        Metadata.Key.of(AUTHORIZATION_KEY, ASCII_STRING_MARSHALLER);
}