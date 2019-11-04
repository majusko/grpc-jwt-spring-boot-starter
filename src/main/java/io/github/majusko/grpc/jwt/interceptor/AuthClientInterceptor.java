package io.github.majusko.grpc.jwt.interceptor;

import io.github.majusko.grpc.jwt.service.JwtService;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClientInterceptor implements ClientInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JwtService jwtService;

    public AuthClientInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method,
        CallOptions callOptions,
        Channel next
    ) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, final Metadata headers) {

                final String internalToken = jwtService.getInternal();

                headers.put(AuthServerInterceptor.AUTHORIZATION_METADATA_KEY, internalToken);

                final Listener<RespT> tracingResponseListener =
                    new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        @Override
                        public void onClose(Status status, Metadata metadata) {

                            if (status.getCode().equals(Status.UNAUTHENTICATED.getCode())) {
                                logger.error("Grpc call is unauthenticated.", status.getCause());
                            }

                            if (status.getCode().equals(Status.PERMISSION_DENIED.getCode())) {
                                logger.error("Grpc call is unauthorized.", status.getCause());
                            }

                            super.onClose(status, metadata);
                        }
                    };

                super.start(tracingResponseListener, headers);
            }
        };
    }
}
