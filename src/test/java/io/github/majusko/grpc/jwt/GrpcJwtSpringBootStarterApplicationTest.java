package io.github.majusko.grpc.jwt;

import com.google.common.collect.Sets;
import com.google.protobuf.Empty;
import io.github.majusko.grpc.jwt.annotation.Allow;
import io.github.majusko.grpc.jwt.annotation.Exposed;
import io.github.majusko.grpc.jwt.data.GrpcHeader;
import io.github.majusko.grpc.jwt.data.GrpcJwtContext;
import io.github.majusko.grpc.jwt.data.JwtContextData;
import io.github.majusko.grpc.jwt.interceptor.AllowedCollector;
import io.github.majusko.grpc.jwt.interceptor.AuthClientInterceptor;
import io.github.majusko.grpc.jwt.interceptor.AuthServerInterceptor;
import io.github.majusko.grpc.jwt.interceptor.proto.Example;
import io.github.majusko.grpc.jwt.interceptor.proto.ExampleServiceGrpc;
import io.github.majusko.grpc.jwt.service.GrpcRole;
import io.github.majusko.grpc.jwt.service.JwtService;
import io.github.majusko.grpc.jwt.service.dto.JwtData;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SpringBootTest
@ActiveProfiles("test")
public class GrpcJwtSpringBootStarterApplicationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AllowedCollector allowedCollector;

    @Autowired
    private AuthServerInterceptor authServerInterceptor;

    @Autowired
    private AuthClientInterceptor authClientInterceptor;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void testSuccessInternalToken() throws IOException {

        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);
        final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);
    }

    @Test
    public void testSuccessCustomAdminToken() throws IOException {

        final String token = jwtService.generate(new JwtData("some-user-id", "admin"));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Empty response = injectedStub.getExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);
    }

    @Test
    public void testCustomTokenWithWrongRole() throws IOException {

        final String token = jwtService.generate(new JwtData("some-user-id", "non-existing-role"));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);

        Status status = Status.OK;

        try {
            final Empty ignored = injectedStub.getExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testWrongToken() throws IOException {

        String token = jwtService.generate(new JwtData("some-user-id", "non-existing-role"));

        token += "crwvvef";
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);

        Status status = Status.OK;

        try {
            final Empty ignored = injectedStub.getExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.UNAUTHENTICATED.getCode(), status.getCode());
    }

    @Test
    public void testMissingAuth() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.getExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testMissingAuthForSimpleAllow() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.someAction(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testMissingCredentials() throws IOException {

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.getExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testCustomTokenWithWrongRoleButMatchingOwner() throws IOException {
        final String ownerUserId = "matching-user-id";
        final String token = jwtService.generate(new JwtData(ownerUserId, "non-existing-role"));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId(ownerUserId).build();
        final Empty response = injectedStub.getExample(request);

        Assertions.assertNotNull(response);
    }

    @Test
    public void testAllowAnnotationWithMissingInterceptor() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.getExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testExposeAnnotationWithMissingInterceptor() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.listExample(Example.GetExampleRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testSuccessExposeToTestEnvAnnotation() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);

        final Empty response = stub.listExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);
    }

    @Test
    public void testNonExistingFieldInPayload() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.saveExample(Empty.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testDiffUserIdAndNonExistingRole() throws IOException {
        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);

        Status status = Status.OK;

        try {
            final Empty ignored = stub.deleteExample(Example.GetExampleRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testCustomTokenWithEmptyUserIdAndEmptyRoles() throws IOException {
        final String token = jwtService.generate(new JwtData("random-user-id", Sets.newHashSet()));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId("other-user-id").build();

        Status status = Status.OK;

        try {
            final Empty ignore = injectedStub.getExample(request);
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testEmptyUserIdInToken() throws IOException {
        final String token = jwtService.generate(new JwtData("", Sets.newHashSet("some-other-role")));
        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);
        final Metadata header = new Metadata();

        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId("other-user-id").build();

        Status status = Status.OK;

        try {
            final Empty ignore = injectedStub.getExample(request);
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    @Test
    public void testExpiredInternalToken() throws IOException, NoSuchFieldException, IllegalAccessException,
        NoSuchMethodException, InvocationTargetException, InterruptedException {

        final GrpcJwtProperties customProperties = new GrpcJwtProperties();
        final Field field = customProperties.getClass().getDeclaredField("expirationSec");
        field.setAccessible(true);
        field.set(customProperties, 1L);

        final Field propertyField = jwtService.getClass().getDeclaredField("properties");
        propertyField.setAccessible(true);
        final GrpcJwtProperties existingProperties = (GrpcJwtProperties) propertyField.get(jwtService);
        propertyField.set(jwtService, customProperties);

        final Method refreshMethod = jwtService.getClass().getDeclaredMethod("refreshInternalToken");
        refreshMethod.setAccessible(true);

        refreshMethod.invoke(jwtService);

        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId("other-user-id").build();

        Thread.sleep(2000);

        final Empty response = stub.getExample(request);

        Assertions.assertNotNull(response);

        propertyField.set(jwtService, existingProperties);
        refreshMethod.invoke(jwtService);
    }

    @Test
    public void testExpiredToken() throws IOException, NoSuchFieldException, IllegalAccessException {

        final GrpcJwtProperties customProperties = new GrpcJwtProperties();
        final Field field = customProperties.getClass().getDeclaredField("expirationSec");
        field.setAccessible(true);
        field.set(customProperties, -10L);

        final JwtService customJwtService = new JwtService(environment, customProperties);
        final String token = customJwtService.generate(new JwtData("lala", Sets.newHashSet(ExampleService.ADMIN)));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, authClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub =
            ExampleServiceGrpc.newBlockingStub(interceptedChannel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId("other-user-id").build();

        Status status = Status.OK;

        try {
            final Empty ignore = injectedStub.getExample(request);
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.UNAUTHENTICATED.getCode(), status.getCode());
    }

    @Test
    public void testMissingOwnerFieldInAnnotationSoRolesAreValidated() throws IOException {
        final String token = jwtService
            .generate(new JwtData("random-user-id", Sets.newHashSet(ExampleService.ADMIN)));

        final ManagedChannel channel = initTestServer(new ExampleService());
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, token);

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);
        final Example.GetExampleRequest request = Example.GetExampleRequest.newBuilder()
            .setUserId("other-user-id").build();

        final Empty response = injectedStub.someAction(request);

        Assertions.assertNotNull(response);
    }

    private ManagedChannel initTestServer(BindableService service) throws IOException {

        final String serverName = InProcessServerBuilder.generateName();
        final Server server = InProcessServerBuilder
            .forName(serverName).directExecutor()
            .addService(service)
            .intercept(authServerInterceptor)
            .build().start();

        allowedCollector.postProcessBeforeInitialization(service, "exampleService");

        grpcCleanup.register(server);

        return grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    }
}

@GRpcService
class ExampleService extends ExampleServiceGrpc.ExampleServiceImplBase {

    public static final String ADMIN = "admin";

    @Override
    @Allow(ownerField = "userId", roles = {GrpcRole.INTERNAL, ADMIN})
    public void getExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {

        JwtContextData authContext = GrpcJwtContext.get().orElseThrow(RuntimeException::new);

        Assertions.assertNotNull(authContext.getJwt());
        Assertions.assertTrue(authContext.getJwtClaims().size() > 0);

        if (!request.getUserId().equals(authContext.getUserId())) {
            Assertions.assertTrue(authContext.getRoles().stream()
                .anyMatch($ -> $.equals(GrpcRole.INTERNAL) || $.equals(ADMIN)));
        }

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    @Exposed(environments = "test")
    public void listExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    @Allow(ownerField = "nonExistingField")
    public void saveExample(Empty request, StreamObserver<Empty> response) {

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    @Allow(ownerField = "userId")
    public void deleteExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    @Allow(roles = {ADMIN})
    public void someAction(Example.GetExampleRequest request, StreamObserver<Empty> response) {

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }
}