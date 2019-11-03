package io.github.majusko.grpc.jwt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Exposed {

    /**
     * List of environments where you can access the endpoint without role or owner authorization.
     */
    String[] environments() default {};
}
