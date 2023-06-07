package com.udacity.webcrawler.service;

import java.util.Collection;
import java.util.Map;

public interface ServiceLocator<T> {

    Collection<T> locateService();

    Map<Class<?>, ServiceMetadata<?>> parse(Collection<T> serviceClasses);

    static ServiceLocator<Class<?>> webCrawlerLocator(boolean includeTest) {
        return WebCrawlerServiceLocator.getInstance(includeTest);
    }
}
