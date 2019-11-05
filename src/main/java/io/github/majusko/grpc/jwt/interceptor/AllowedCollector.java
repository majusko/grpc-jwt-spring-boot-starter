package io.github.majusko.grpc.jwt.interceptor;

import com.google.common.collect.Sets;
import io.github.majusko.grpc.jwt.annotation.Allow;
import io.github.majusko.grpc.jwt.annotation.Exposed;
import io.github.majusko.grpc.jwt.collector.Allowed;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AllowedCollector implements BeanPostProcessor {

    private static final String GRPC_BASE_CLASS_NAME_EXT = "ImplBase";
    private Map<String, Allowed> allowedAuth;
    private Map<String, Set<String>> exposedEnv;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        addToStorage(bean.getClass());

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    Optional<Allowed> getAllowedAuth(String methodName) {
        return Optional.ofNullable(allowedAuth.get(methodName));
    }

    Optional<Set<String>> getExposedEnv(String methodName) {
        return Optional.ofNullable(exposedEnv.get(methodName));
    }

    private void addToStorage(Class<?> beanClass) {
        if (beanClass.isAnnotationPresent(GRpcService.class)) {
            this.allowedAuth = Arrays.stream(beanClass.getMethods())
                .filter(method -> method.isAnnotationPresent(Allow.class))
                .map(method -> buildAllowed(beanClass, method))
                .collect(Collectors.toMap(Allowed::getMethod, allowed -> allowed));

            this.exposedEnv = Arrays.stream(beanClass.getMethods())
                .filter(method -> method.isAnnotationPresent(Exposed.class))
                .collect(Collectors.toMap(method -> getGrpcServiceCallDescriptor(beanClass, method), this::buildEnv));
        }
    }

    private Set<String> buildEnv(Method method) {
        final Exposed annotation = method.getAnnotation(Exposed.class);
        return Arrays.stream(annotation.environments()).collect(Collectors.toSet());
    }

    private Allowed buildAllowed(Class<?> gRpcServiceClass, Method method) {
        final Allow annotation = method.getAnnotation(Allow.class);
        final Set<String> roles = Sets.newHashSet(Arrays.asList(annotation.roles()));

        return new Allowed(getGrpcServiceCallDescriptor(gRpcServiceClass, method), annotation.ownerField(), roles);
    }

    private String getGrpcServiceCallDescriptor(Class<?> gRpcServiceClass, Method method) {
        final Class<?> superClass = gRpcServiceClass.getSuperclass();

        return (superClass.getPackage().getName() +
            "." +
            superClass.getSimpleName().replace(GRPC_BASE_CLASS_NAME_EXT, "") +
            "/" +
            method.getName()).toLowerCase();
    }
}