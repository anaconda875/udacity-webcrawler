package com.udacity.webcrawler.service;

import com.google.common.reflect.ClassPath;
import com.udacity.webcrawler.profiler.Wrapped;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class WebCrawlerServiceLocator implements ServiceLocator<Class<?>> {
    private static final Class<? extends Annotation> MARKER = Wrapped.class;
    private static final WebCrawlerServiceLocator INSTANCE;

    static {
        INSTANCE = new WebCrawlerServiceLocator(false);
    }

    private final Map<Class<?>, ServiceMetadata<?>> cache = new ConcurrentHashMap<>();

    private WebCrawlerServiceLocator(boolean includeTest) {
        if (!includeTest) {
            Iterator<Class<?>> services = new HashSet<>(locateService()).iterator();
            List<Class<?>> expectedServices = new ArrayList<>();
            while (services.hasNext()) {
                Class<?> clazz = services.next();
                if (clazz.getName().endsWith("Test") || clazz.getName().endsWith("Tests") || clazz.getName().contains("Test$")) {
                    services.remove();
                } else {
                    expectedServices.add(clazz);
                }
            }
            Map<Class<?>, ServiceMetadata<?>> metadataMap = parse(expectedServices);
            this.cache.putAll(metadataMap);
        }
    }

    @Override
    public Collection<Class<?>> locateService() {
        try {
            return new HashSet<>(ClassPath.from(ClassLoader.getSystemClassLoader())
                    .getAllClasses()
                    .stream()
                    .filter(classInfo -> !classInfo.getName().equals("module-info") || !classInfo.getName().equals("package-info"))
                    .map(info -> {
                        try {
                            return info.load();
                        } catch (NoClassDefFoundError e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(clazz -> clazz.isAnnotationPresent(MARKER))
                    .toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Map<Class<?>, ServiceMetadata<?>> parse(Collection<Class<?>> serviceClasses) {
        Map<Class<?>, ServiceMetadata<?>> metadataMap = new LinkedHashMap<>();
        if (cache.isEmpty()) {
            serviceClasses.stream()
                    .map(ServiceMetadata::getDefaultInstance)
                    .filter(ServiceMetadata::isReady)
                    .toList()
                    .forEach(serviceMetadata -> metadataMap.put(serviceMetadata.getDelegate(), serviceMetadata));
            cache.putAll(metadataMap);
        } else {
            metadataMap.putAll(cache);
        }
        return metadataMap;
    }

    static ServiceLocator<Class<?>> getInstance(boolean includeTest) {
        if (!includeTest) {
            return INSTANCE;
        } else {
            return new WebCrawlerServiceLocator(true);
        }
    }
}
