package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.service.ServiceMetadata;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;
    private final ZonedDateTime startTime;
    private final ServiceMetadata<?> metadata;

    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state, ZonedDateTime startTime, ServiceMetadata<?> metadata) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = delegate;
        this.state = state;
        this.startTime = startTime;
        this.metadata = Objects.requireNonNull(metadata, "Metadata can not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        makeAccessible(method);
        ServiceMetadata.ProfiledMetadata descriptor = metadata.getMetadata(method.getName());
        if (descriptor == null) {
            return method.invoke(delegate, args);
        }
        Class<?> methodReturnType = descriptor.getReturnType();
        Object invoked;
        Instant start = clock.instant();
        try {
            invoked = method.invoke(delegate, args);
            if (!methodReturnType.equals(method.getReturnType())) {
                throw new IllegalAccessException("Wrong expected return type");
            }
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            Duration duration = Duration.between(start, clock.instant());
            state.record(delegate.getClass(), method, duration);
        }

        return methodReturnType.cast(invoked);
    }

    private void makeAccessible(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setAccessible(true);
        }
    }
}
