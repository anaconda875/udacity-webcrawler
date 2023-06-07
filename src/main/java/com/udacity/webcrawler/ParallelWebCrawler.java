package com.udacity.webcrawler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.profiler.Wrapped;

/**
 * Use {@link ExecutorService} for perform the web crawler with multiple thread.
 * */
@Wrapped
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ExecutorService threadPool;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;
    private final PageParserFactory parserFactory;

    @Inject
    ParallelWebCrawler(Clock clock, @Timeout Duration timeout, @PopularWordCount int popularWordCount, @TargetParallelism int threadCount, @IgnoredUrls List<Pattern> ignoredUrls, @MaxDepth int maxDepth, PageParserFactory parserFactory) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.ignoredUrls = ignoredUrls;
        this.maxDepth = maxDepth;
        this.parserFactory = parserFactory;
        this.threadPool = Executors.newFixedThreadPool(Math.min(threadCount, getMaxParallelism()));
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
        Map<String, Boolean> report = new ConcurrentHashMap<>();
        for (String url : startingUrls) {
            threadPool.execute(() -> {
                Callable<Boolean> task = new CallableCrawler(threadPool, url, deadline, maxDepth, counts, visitedUrls);
                try {
                    report.put(url, task.call());
                } catch (Exception e) {
                    System.err.println("Craw url [" + url + "] failure");
                    e.printStackTrace();
                    report.put(url, false);
                }
            });
        }

        CrawlResult.Builder resultBuilder = CrawlResult.builder();
        resultBuilder.setUrlsFailure(report.entrySet().stream().filter(target -> !target.getValue()).map(Map.Entry::getKey).toList());
        resultBuilder.setUrlsVisited(visitedUrls.size());

        if (counts.isEmpty()) {
            resultBuilder.setWordCounts(counts);
        } else {
            resultBuilder.setWordCounts(WordCounts.sort(counts, popularWordCount));
        }

        return resultBuilder.build();
    }

    private class CallableCrawler implements Callable<Boolean> {
        private final ExecutorService executor;
        private final String url;
        private final Instant deadline;
        private final int maxDepth;
        private final ConcurrentMap<String, Integer> counts;
        private final ConcurrentSkipListSet<String> visitedUrls;

        CallableCrawler(ExecutorService executor, String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
            this.executor = executor;
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
        }

        @Override
        public Boolean call() throws Exception {
            if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
                return false;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return false;
                }
            }
            if (visitedUrls.contains(url)) {
                return false;
            }
            visitedUrls.add(url);
            PageParser.Result result = parserFactory.get(url).parse();
            result.getWordCounts().forEach((key, value) -> counts.compute(key, (innerKey, innerValue) -> (innerValue == null) ? value : value + innerValue));
            List<Callable<Boolean>> callTasks = new ArrayList<>();
            result.getLinks().forEach(link -> callTasks.add(new CallableCrawler(executor, link, deadline, maxDepth - 1, counts, visitedUrls)));
            List<Future<Boolean>> results = executor.invokeAll(callTasks);
            AtomicBoolean finalResult = new AtomicBoolean(true);
            results.forEach(r -> {
                try {
                    finalResult.set(Boolean.logicalAnd(finalResult.get(), r.get()));
                } catch (Exception e) {
                    finalResult.set(false);
                }
            });
            return finalResult.get();
        }
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
