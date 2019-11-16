package io.github.majusko.grpc.jwt.data;

import java.util.Optional;

public class GrpcJwtContext {

    private GrpcJwtContext() {
    }

    private static final String CONTEXT_DATA = "context_data";

    public static final io.grpc.Context.Key<JwtContextData> CONTEXT_DATA_KEY = io.grpc.Context.key(CONTEXT_DATA);

    public static Optional<JwtContextData> get() {
        return Optional.ofNullable(CONTEXT_DATA_KEY.get());
    }
}
