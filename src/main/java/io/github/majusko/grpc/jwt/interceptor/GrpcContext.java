package io.github.majusko.grpc.jwt.interceptor;

public class GrpcContext {

    private static final String CONTEXT_DATA = "context_data";

    public static io.grpc.Context.Key<AuthContextData> CONTEXT_DATA_KEY = io.grpc.Context.key(CONTEXT_DATA);

    public static AuthContextData get() {
        return CONTEXT_DATA_KEY.get();
    }
}
