package io.github.majusko.grpc.jwt.exception;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
    }

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
