package com.udacity.webcrawler.service;

import com.udacity.webcrawler.profiler.Profiled;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ServiceMetadata<T> {
    private final Class<T> delegate;
    private Map<String, ProfiledMetadata> methodMetadata = new LinkedHashMap<>();

    /**
     * Load method metadata of given class, find all method annotated by {@link Profiled}. Recommend use {@link Profiled}
     * on subclass for better performance.
     * */
    protected ServiceMetadata(Class<T> delegate) {
        this.delegate = delegate;
        Class<?>[] interfaces = delegate.getInterfaces();

        for (Method method : delegate.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Profiled.class)) {
                methodMetadata.put(method.getName(), new ProfiledMetadata(method));
            }
        }

        if (methodMetadata.isEmpty()) {
            for (Class<?> proxy : interfaces) {
                for (Method method : proxy.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Profiled.class)) {
                        methodMetadata.put(method.getName(), new ProfiledMetadata(method));
                    }
                }
            }
        }
    }

    public Class<T> getDelegate() {
        return delegate;
    }

    public ProfiledMetadata getMetadata(String methodName) {
        return methodMetadata.get(methodName);
    }

    boolean isReady() {
        return !methodMetadata.isEmpty();
    }

    public static ServiceMetadata<?> getDefaultInstance(Class<?> delegate) {
        return new DefaultServiceMetadata(delegate);
    }

    private static class DefaultServiceMetadata<T> extends ServiceMetadata<T> {

        DefaultServiceMetadata(Class<T> delegate) {
            super(delegate);
        }
    }

    public static class ProfiledMetadata {
        private final String name;
        private final Class<?>[] paramTypes;
        private final Class<?> returnType;

        public ProfiledMetadata(Method method) {
            this.name = method.getName();
            this.paramTypes = method.getParameterTypes();
            this.returnType = method.getReturnType();
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public String getName() {
            return name;
        }
    }
}
