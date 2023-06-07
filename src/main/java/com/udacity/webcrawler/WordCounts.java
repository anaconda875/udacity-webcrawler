package com.udacity.webcrawler;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 * <p>TODO: Reimplement the sort() method using only the Stream API and lambdas and/or method
 * references.
 */
final class WordCounts {

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided {@link WordCountComparator}, and includes only the top
     * {@param popluarWordCount} words and counts.
     *
     * <p>TODO: Reimplement this method using only the Stream API and lambdas and/or method
     * references.
     *
     * @param wordCounts       the unsorted map of word counts.
     * @param popularWordCount the number of popular words to include in the result map.
     * @return a map containing the top {@param popularWordCount} words and counts in the right order.
     */
    static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

        return wordCounts.entrySet()
                .stream()
                .sorted(Comparator.comparing(keyExtractor -> keyExtractor, (a, b) -> {
                    if (!a.getValue().equals(b.getValue())) {
                        return b.getValue() - a.getValue();
                    }
                    if (a.getKey().length() != b.getKey().length()) {
                        return b.getKey().length() - a.getKey().length();
                    }
                    return a.getKey().compareTo(b.getKey());
                }))
                .limit(Math.min(popularWordCount, wordCounts.size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private WordCounts() {
        // This class cannot be instantiated
    }
}