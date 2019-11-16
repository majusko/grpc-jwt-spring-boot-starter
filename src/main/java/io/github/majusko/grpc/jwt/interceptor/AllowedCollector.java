package io.github.majusko.grpc.jwt.interceptor;

import com.google.common.collect.Sets;
import io.github.majusko.grpc.jwt.annotation.Allow;
import io.github.majusko.grpc.jwt.annotation.Exposed;
import io.github.majusko.grpc.jwt.data.AllowedMethod;
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
    private static final String PACKAGE_CLASS_DELIMITER = ".";
    private static final String CLASS_METHOD_DELIMITER = "/";
    private static final String EMPTY_STRING = "";

    private Map<String, AllowedMethod> allowedMethods;
    private Map<String, Set<String>> exposedMethods;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        processGrpcServices(bean.getClass());

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    Optional<AllowedMethod> getAllowedAuth(String methodName) {
        return Optional.ofNullable(allowedMethods.get(methodName));
    }

    Optional<Set<String>> getExposedEnv(String methodName) {
        return Optional.ofNullable(exposedMethods.get(methodName));
    }

    private void processGrpcServices(Class<?> beanClass) {
        if (beanClass.isAnnotationPresent(GRpcService.class)) {
            this.allowedMethods = findAllowedMethods(beanClass);
            this.exposedMethods = findExposedMethods(beanClass);
        }
    }

    private Map<String, AllowedMethod> findAllowedMethods(Class<?> beanClass) {
        return Arrays.stream(beanClass.getMethods())
            .filter(method -> method.isAnnotationPresent(Allow.class))
            .map(method -> buildAllowed(beanClass, method))
            .collect(Collectors.toMap(AllowedMethod::getMethod, allowedMethod -> allowedMethod));
    }

    private Map<String, Set<String>> findExposedMethods(Class<?> beanClass) {
        return Arrays.stream(beanClass.getMethods())
            .filter(method -> method.isAnnotationPresent(Exposed.class))
            .collect(Collectors.toMap(method -> getGrpcServiceDescriptor(beanClass, method), this::buildEnv));
    }

    private Set<String> buildEnv(Method method) {
        final Exposed annotation = method.getAnnotation(Exposed.class);
        return Arrays.stream(annotation.environments()).collect(Collectors.toSet());
    }

    private AllowedMethod buildAllowed(Class<?> gRpcServiceClass, Method method) {
        final Allow annotation = method.getAnnotation(Allow.class);
        final Set<String> roles = Sets.newHashSet(Arrays.asList(annotation.roles()));

        return new AllowedMethod(getGrpcServiceDescriptor(gRpcServiceClass, method), annotation.ownerField(), roles);
    }

    private String getGrpcServiceDescriptor(Class<?> gRpcServiceClass, Method method) {
        final Class<?> superClass = gRpcServiceClass.getSuperclass();

        return (superClass.getPackage().getName() +
            PACKAGE_CLASS_DELIMITER +
            superClass.getSimpleName().replace(GRPC_BASE_CLASS_NAME_EXT, EMPTY_STRING) +
            CLASS_METHOD_DELIMITER +
            method.getName()).toLowerCase();
    }
}