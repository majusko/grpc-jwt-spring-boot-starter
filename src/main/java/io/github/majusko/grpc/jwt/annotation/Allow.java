package io.github.majusko.grpc.jwt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Allow {

    /**
     * List of roles that will by checked. One of the roles must be presented in JWT token.
     */
    String[] roles() default {};

    /**
     * Optional field. Ownership of entity will be checked first by getting owners id from payload by
     * field specified in annotation. If the id does not match and data are owned by other authority,
     * specified roles will be checked then.
     */
    String ownerField() default "";
}
