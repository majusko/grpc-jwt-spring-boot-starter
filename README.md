# Spring boot starter for [gRPC framework](https://grpc.io/) with [JWT authorization](https://jwt.io/) - gRPC Java JWT

[![Release](https://jitpack.io/v/majusko/grpc-jwt-spring-boot-starter.svg)](https://jitpack.io/#majusko/grpc-jwt-spring-boot-starter)
[![Build Status](https://travis-ci.com/majusko/grpc-jwt-spring-boot-starter.svg?branch=master)](https://travis-ci.com/majusko/grpc-jwt-spring-boot-starter)
[![Test Coverage](https://codecov.io/gh/majusko/grpc-jwt-spring-boot-starter/branch/master/graph/badge.svg)](https://codecov.io/gh/majusko/grpc-jwt-spring-boot-starter/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Join the chat at https://gitter.im/grpc-jwt-spring-boot-starter/community](https://badges.gitter.im/grpc-jwt-spring-boot-starter/community.svg)](https://gitter.im/grpc-jwt-spring-boot-starter/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Extending great [gRPC library](https://github.com/LogNet/grpc-spring-boot-starter) with Auth module. Easy implementation using a simple annotations similar to ones used in Spring Security module.

## Quick Start

Simple start consist only from 3 simple steps.

(If you never used [gRPC library](https://github.com/LogNet/grpc-spring-boot-starter) before, have a look on this [basic setup](https://github.com/LogNet/grpc-spring-boot-starter#4-show-case) first.)

#### 1. Add Maven dependency

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
  <groupId>io.github.majusko</groupId>
  <artifactId>grpc-jwt-spring-boot-starter</artifactId>
  <version>${version}</version>
</dependency>
```

#### 2. Add `@Allow` annotation to your service method

All you need to do is to annotate your method in the service implementation.

```java
@GRpcService
public class ExampleServiceImpl extends ExampleServiceGrpc.ExampleServiceImplBase {

    @Allow(roles = GrpcRole.INTERNAL)
    public void getExample(GetExample request, StreamObserver<Empty> response) {
        //...
    }
}
```

#### 3. Add interceptor to client

Just autowire already prepared `AuthClientInterceptor` bean and intercept your client. It will inject the internal token to every request by default.

```java
@Service
public class ExampleClient {

    @Autowired
    private AuthClientInterceptor authClientInterceptor;

    public void exampleRequest() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        final Channel interceptedChannel = ClientInterceptors.intercept(channel,authClientInterceptor);
        final ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(interceptedChannel);
        
        stub.getExample(GetExample.newBuilder().build());
    }
}
```

## Documentation

### 0. Basic setup of gRPC
Useful only in case you never heard about [gRPC library from LogNet](https://github.com/LogNet/grpc-spring-boot-starter).
You can find there a nice [show case](https://github.com/LogNet/grpc-spring-boot-starter#4-show-case) too.

#### 0.1 Service implementation

The service definition from .proto file looks like this 

```proto
service ExampleService {
    rpc GetExample (GetExample) returns (Empty) {};
}

message Empty {}
message GetExample {
    string ownerField = 1;
}
```

#### 0.2 Service implementation

All you need to do is to annotate your service implementation with `GRpcService`

```java
@GRpcService
public class ExampleServiceImpl extends ExampleServiceGrpc.ExampleServiceImplBase {

    public void getExample(GetExample request, StreamObserver<Empty> response) {
        response.onNext(Empty.newBuilder().build());
        response.onCompleted();
    }
}
```

### 1. Configuration

You can use `application.properties` to override the default configuration. 

* `grpc.jwt.algorithm` -> Algorithm used for signing the JWT token. Default: `HmacSHA256`
* `grpc.jwt.secret` -> String used as a secret to sign the JWT token. Default: `default`
* `grpc.jwt.expirationSec` -> Number of seconds needed to token becoming expired. Default: `3600`

```
grpc.jwt.algorithm=HmacSHA256
grpc.jwt.secret=secret
grpc.jwt.expirationSec=3600
```

### 2. Annotations

We know 2 types of annotation: `@Allow` and `@Expose`

#### `@Allow` 
* `roles` -> Algorithm used for signing the JWT token. Default: `HmacSHA256`
* `ownerField` -> Example: `ownerField`. _Optional field_. Your request will be parsed and if the mentioned field is found, it will compare equality with JWT token subject(e.g.: ownerField). By this comparison, you can be sure that any operation with that field is made by the owner of the token. If the fields don't match and data are owned by another user, specified roles will be checked after. 
 
 
 _**Example use case of `ownerField`**: Imagine, you want to list purchased orders of some user. 
 You might want to reuse the exact same API for back-office and also for that particular user who created the orders.
 With `ownerField` you can check for the owner and also for some role if owner ownerField in JWT token is different._

#### `@Exposed` 
* `environments` List of environments (Spring Profiles) where you can access the gRPC without checking for owner or roles.
Use case: Debug endpoint for the client/front-end development team.

```java
@GRpcService
public class ExampleServiceImpl extends ExampleServiceGrpc.ExampleServiceImplBase {

    @Allow(ownerField="ownerField", roles = GrpcRole.INTERNAL)
    @Exposed(environments={"dev","qa"})
    public void getExample(GetExample request, StreamObserver<Empty> response) {
        //...
    }
}
```

### Token generation

You will need to generate tokens for your users or clients. You might want to specify special roles for each user and also service method. You can use the `JwtService` for simple and performing usage.

```java
@Service
public class SomeClass {

    private final static String ADMIN = "admin";

    @Autowired
    private JwtService jwtService;

    public void someMethod() {
        final JwtData data = new JwtData("user-id-12345", new HashSet<>(ADMIN));

        final String token = jwtService.generate(data);
    }
}
```

### Making requests

We have two types of usages for client.

1. Inter-service communication.
2. User communication.

#### 1. Client for inter-service communication only.

* Autowire `AuthClientInterceptor` to your service.
* Register your interceptor with native `ClientInterceptors` class.
* Call request from gRPC generated stub.

```java
@Service
public class SomeClass {

    @Autowired
    private AuthClientInterceptor authClientInterceptor;

    public void customTokenRequest() {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        final Channel interceptedChannel = ClientInterceptors.intercept(channel,authClientInterceptor);
        final ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(interceptedChannel);

        final Empty response = stub.getExample(GetExample.newBuilder().setUserId("user-id-jr834fh").build());
    }
}
```

#### 2. Client for custom token communication.

* Add your token generated with `JwtService` to gRPC header with `GrpcHeader.AUTHORIZATION`
* Call request from gRPC generated stub.

```java
@Service
public class SomeClass {

    public void customTokenRequest() {

        final Metadata header = new Metadata();
        header.put(GrpcHeader.AUTHORIZATION, "jwt-token-r348hf34hf43f93");

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        final ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);
        final ExampleServiceBlockingStub stubWithHeaders = MetadataUtils.attachHeaders(stub, header);

        final Empty response = stub.getExample(GetExample.newBuilder().setUserId("user-id-jr834fh").build());
    }
}
```

### Tests

The library is fully covered with integration tests which are also very useful as a usage example.

`GrpcJwtSpringBootStarterApplicationTest`

## Contributing

All contributors are welcome. If you never contributed to the open-source, start with reading the [Github Flow](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/github-flow).

1. Create an [issue](https://help.github.com/en/github/managing-your-work-on-github/about-issues)
2. Create a [pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/about-pull-requests) with reference to the issue
3. Rest and enjoy the great feeling of being a contributor.
