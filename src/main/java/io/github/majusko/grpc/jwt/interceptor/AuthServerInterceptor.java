package io.github.majusko.grpc.jwt.interceptor;

import com.google.common.collect.Sets;
import io.github.majusko.grpc.jwt.collector.Allowed;
import io.github.majusko.grpc.jwt.collector.AllowedCollector;
import io.github.majusko.grpc.jwt.exception.AuthException;
import io.github.majusko.grpc.jwt.exception.UnauthenticatedException;
import io.github.majusko.grpc.jwt.service.JwtService;
import io.github.majusko.grpc.jwt.service.dto.JwtRoles;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

@GRpcGlobalInterceptor
public class AuthServerInterceptor implements ServerInterceptor {

    private final static String GRPC_FIELD_MODIFIER = "_";
    private final static String BEARER = "Bearer";
    private final static String AUTHORIZATION = "Authorization";
    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    public static Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
        Metadata.Key.of(AUTHORIZATION, ASCII_STRING_MARSHALLER);

    private final AllowedCollector allowedCollector;
    private final JwtService jwtService;
    private final Environment environment;

    public AuthServerInterceptor(
        AllowedCollector allowedCollector,
        JwtService jwtService,
        Environment environment
    ) {
        this.allowedCollector = allowedCollector;
        this.jwtService = jwtService;
        this.environment = environment;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next
    ) {
        try {
            final AuthContextData contextData = parseAuthContextData(metadata);
            final Context context = Context.current().withValue(GrpcContext.CONTEXT_DATA_KEY, contextData);

            return buildListener(call, metadata, next, context, contextData);
        } catch (UnauthenticatedException e) {
            call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e.getCause()), metadata);
            //noinspection unchecked
            return NOOP_LISTENER;
        } catch (Exception e) {
            call.close(Status.INTERNAL.withDescription(e.getMessage()).withCause(e.getCause()), metadata);
            //noinspection unchecked
            return NOOP_LISTENER;
        }
    }

    private <ReqT, RespT> ForwardingServerCallListener<ReqT> buildListener(
        ServerCall<ReqT, RespT> call,
        Metadata metadata,
        ServerCallHandler<ReqT, RespT> next,
        Context context,
        AuthContextData contextData
    ) {
        final ServerCall.Listener<ReqT> customDelegate = Contexts.interceptCall(context, call, metadata, next);

        return new ForwardingServerCallListener<ReqT>() {

            @SuppressWarnings("unchecked")
            ServerCall.Listener<ReqT> delegate = NOOP_LISTENER;

            @Override
            protected ServerCall.Listener<ReqT> delegate() {
                return delegate;
            }

            private void handlingException(Status status, Exception e) {
                call.close(status.withDescription(e.getMessage()).withCause(e.getCause()), metadata);
            }

            @Override
            public void onMessage(ReqT request) {
                try {
                    if (delegate == NOOP_LISTENER) {
                        final String methodName = call.getMethodDescriptor().getFullMethodName().toLowerCase();

                        validateAnnotatedMethods(request, contextData, methodName);

                        delegate = customDelegate;
                    }
                } catch (UnauthenticatedException e) {
                    handlingException(Status.UNAUTHENTICATED, e);
                } catch (AuthException e) {
                    handlingException(Status.PERMISSION_DENIED, e);
                } catch (Exception e) {
                    handlingException(Status.INTERNAL, e);
                }
                super.onMessage(request);
            }
        };
    }

    private <ReqT> void validateAnnotatedMethods(ReqT request, AuthContextData contextData, String methodName) {
        if (!validateExposedAnnotation(contextData, methodName)) {
            validateAllowedAnnotation(request, contextData, methodName);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean validateExposedAnnotation(AuthContextData contextData, String methodName) {
        final Set<String> exposedToEnvironments = allowedCollector.getExposedEnv(methodName).orElse(Sets.newHashSet());
        final boolean methodIsExposed = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(exposedToEnvironments::contains);

        if (methodIsExposed) {
            final List<String> rawEnvironments = (List<String>) contextData
                .getJwtClaims().get(JwtService.TOKEN_ENV, List.class);

            final Set<String> environments = rawEnvironments.stream().map(Object::toString).collect(Collectors.toSet());
            return exposedToEnvironments.stream().anyMatch(environments::contains);
        }

        return false;
    }

    private <ReqT> void validateAllowedAnnotation(ReqT request, AuthContextData contextData, String methodName) {
        allowedCollector.getAllowedAuth(methodName)
            .ifPresent(value -> authorize(request, Objects.requireNonNull(contextData), value));
    }

    private <ReqT> void authorize(ReqT request, AuthContextData contextData, Allowed allowed) {

        //TODO validate expiration

        if (allowed.getUserId() != null && !allowed.getUserId().isEmpty()) {
            try {
                final Field field = request.getClass().getDeclaredField(allowed.getUserId() + GRPC_FIELD_MODIFIER);
                field.setAccessible(true);
                final String userId = String.valueOf(field.get(request));

                authorizeOwner(userId, new HashSet<>(allowed.getRoles()), contextData);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AuthException("Missing field.", e);
            }
        } else {
            validateRoles(new HashSet<>(allowed.getRoles()), contextData.getRoles());
        }
    }

    private void authorizeOwner(String uid, Set<String> required, AuthContextData contextData) {
        if (contextData == null || contextData.getUserId() == null || contextData.getUserId().isEmpty()) {
            throw new AuthException("Owner field is missing.");
        }

        if (!contextData.getUserId().equals(uid)) validateRoles(required, contextData.getRoles());
    }

    private void validateRoles(Set<String> requiredRoles, Set<String> userRoles) {

        if (requiredRoles.isEmpty()) {
            throw new AuthException("Endpoint does not have specified roles.");
        }

        if (userRoles == null) {
            throw new AuthException("User doesn't have any roles.");
        }

        requiredRoles.retainAll(userRoles);

        if (requiredRoles.isEmpty()) {
            throw new AuthException("Missing required permission roles.");
        }
    }

    private AuthContextData parseAuthContextData(Metadata metadata) {
        try {
            final String authHeaderData = metadata.get(AUTHORIZATION_METADATA_KEY);

            if (authHeaderData == null) {
                return null;
            }

            final String token = authHeaderData.replace(BEARER, "").trim();
            final Claims jwtBody = Jwts.parser().setSigningKey(jwtService.getKey()).parseClaimsJws(token).getBody();
            final JwtRoles roles = jwtBody.get(JwtService.JWT_ROLES, JwtRoles.class);

            return new AuthContextData(token, jwtBody.getSubject(), roles.getRoles(), jwtBody);
        } catch (Exception e) {
            throw new UnauthenticatedException(e.getMessage());
        }
    }
}