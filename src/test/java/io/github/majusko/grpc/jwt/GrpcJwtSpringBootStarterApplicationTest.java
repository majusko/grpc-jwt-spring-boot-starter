package io.github.majusko.grpc.jwt;

import com.google.protobuf.Empty;
import io.github.majusko.grpc.jwt.annotation.Allow;
import io.github.majusko.grpc.jwt.annotation.Exposed;
import io.github.majusko.grpc.jwt.interceptor.*;
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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GrpcJwtSpringBootStarterApplicationTest {

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
		final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(interceptedChannel);
		final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

		Assert.assertNotNull(response);
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

		Assert.assertNotNull(response);
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

		Assert.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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

		Assert.assertEquals(Status.UNAUTHENTICATED.getCode(), status.getCode());
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

		Assert.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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

		Assert.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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

		Assert.assertNotNull(response);
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

		Assert.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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

		Assert.assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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

	private static final String ADMIN = "admin";

	@Override
	@Allow(ownerField = "userId", roles = {GrpcRole.INTERNAL, ADMIN})
	public void getExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {

		AuthContextData authContext = GrpcJwtContext.get().orElseThrow(RuntimeException::new);

		Assert.assertNotNull(authContext.getJwt());
		Assert.assertTrue(authContext.getJwtClaims().size() > 0);

		if(!request.getUserId().equals(authContext.getUserId())) {
			Assert.assertTrue(authContext.getRoles().stream()
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
}