package io.github.majusko.grpc.jwt.interceptor;

import java.util.Optional;

public class GrpcJwtContext {

    private static final String CONTEXT_DATA = "context_data";

    public static io.grpc.Context.Key<AuthContextData> CONTEXT_DATA_KEY = io.grpc.Context.key(CONTEXT_DATA);

    public static Optional<AuthContextData> get() {
        return Optional.ofNullable(CONTEXT_DATA_KEY.get());
    }
}
