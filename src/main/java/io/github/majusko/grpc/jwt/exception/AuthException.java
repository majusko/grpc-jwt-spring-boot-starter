package io.github.majusko.grpc.jwt.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
