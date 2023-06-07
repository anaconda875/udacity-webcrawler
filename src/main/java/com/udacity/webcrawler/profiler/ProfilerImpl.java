package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.service.ServiceLocator;
import com.udacity.webcrawler.service.ServiceMetadata;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;

import javax.inject.Inject;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {
    private final Map<Class<?>, ServiceMetadata<?>> serviceMetadata;
    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock, boolean includeTest) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
        ServiceLocator<Class<?>> serviceLocator = ServiceLocator.webCrawlerLocator(includeTest);
        serviceMetadata = serviceLocator.parse(serviceLocator.locateService());
    }

    private ServiceMetadata<?> profiledClass(Class<?> klass) {
        for (Class<?> clazz : serviceMetadata.keySet()) {
            if (klass.isAssignableFrom(clazz)) {
                return serviceMetadata.get(clazz);
            }
        }
        return null;
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass);

        ServiceMetadata<?> profiledClass = profiledClass(klass);
        if (profiledClass == null) {
            throw new IllegalArgumentException(klass.getName() + "doesn't have profiled methods.");
        }

        ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, delegate, state, startTime, profiledClass);

        Object proxy = Proxy.newProxyInstance(
                ProfilerImpl.class.getClassLoader(),
                new Class[]{klass},
                interceptor
        );

        return (T) proxy;
    }

    @Override
    public void writeData(Path path) {
        Objects.requireNonNull(path);

        try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writeData(writer);
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
