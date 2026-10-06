package com.takabridge.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Past daily exchange rates, for the trend charts.
 *
 * The source is the free fawazahmed0 currency-api, which keeps one file per
 * day and needs no key. A past day never changes, so every day is downloaded
 * once and then kept in memory - nothing is written to the database.
 *
 * One shared instance is created by DailyRateUpdater when TakaBridge starts
 * and is closed again when it stops.
 */
public class RateHistoryClient implements AutoCloseable {

    /** Name under which the shared instance is kept in the ServletContext. */
    public static final String CONTEXT_KEY = "takabridge.rateHistory";

    private static final String PRIMARY_URL =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@%s/v1/currencies/usd.json";
    private static final String FALLBACK_URL =
            "https://%s.currency-api.pages.dev/v1/currencies/usd.json";

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    /** A day that could not be fetched is not asked for again for an hour. */
    private static final Duration RETRY_AFTER = Duration.ofHours(1);

    /** Recent days may not be published yet, so a little extra is requested. */
    private static final int EXTRA_DAYS = 3;

    /** The rates object of the reply: "usd": { "aed": 3.67, "bdt": 123.4, ... } */
    private static final Pattern USD_BLOCK = Pattern.compile("\"usd\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern ONE_RATE = Pattern.compile("\"([a-z]{3})\"\\s*:\\s*([0-9.eE+-]+)");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Map<LocalDate, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
    private final Map<LocalDate, Instant> failedAt = new ConcurrentHashMap<>();

    /**
     * The newest {@code days} days of rates per US Dollar, oldest first.
     * Days not in memory yet are downloaded, all at the same time.
     */
    public SortedMap<LocalDate, Map<String, BigDecimal>> lastDays(int days) {
        List<LocalDate> wanted = window(days);

        List<CompletableFuture<Void>> downloads = new ArrayList<>();
        for (LocalDate day : wanted) {
            if (!cache.containsKey(day) && !recentlyFailed(day)) {
                downloads.add(fetchDay(day)
                        .thenAccept(rates -> cache.put(day, rates))
                        .exceptionally(error -> {
                            failedAt.put(day, Instant.now());
                            return null;
                        }));
            }
        }

        try {
            CompletableFuture.allOf(downloads.toArray(new CompletableFuture[0]))
                    .get(TIMEOUT.toSeconds() * 2, TimeUnit.SECONDS);
        } catch (Exception slowOrInterrupted) {
            // Whatever arrived in time is used; the rest is tried again later.
            if (slowOrInterrupted instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return lastDaysCached(days);
    }

    /**
     * The same, but only from memory, so it never waits on the network.
     * The home page uses this to stay fast.
     */
    public SortedMap<LocalDate, Map<String, BigDecimal>> lastDaysCached(int days) {
        TreeMap<LocalDate, Map<String, BigDecimal>> found = new TreeMap<>();
        for (LocalDate day : window(days)) {
            Map<String, BigDecimal> rates = cache.get(day);
            if (rates != null) {
                found.put(day, rates);
            }
        }
        // Keep only the newest `days` entries.
        while (found.size() > days) {
            found.pollFirstEntry();
        }
        return found;
    }

    @Override
    public void close() {
        http.close();
    }

    /** Today and the days before it, oldest first. */
    private List<LocalDate> window(int days) {
        LocalDate today = LocalDate.now();
        List<LocalDate> list = new ArrayList<>();
        for (int i = days + EXTRA_DAYS - 1; i >= 0; i--) {
            list.add(today.minusDays(i));
        }
        return list;
    }

    private boolean recentlyFailed(LocalDate day) {
        Instant when = failedAt.get(day);
        return when != null && when.plus(RETRY_AFTER).isAfter(Instant.now());
    }

    /** Tries the main address first and the mirror second. */
    private CompletableFuture<Map<String, BigDecimal>> fetchDay(LocalDate day) {
        return get(String.format(PRIMARY_URL, day))
                .exceptionallyCompose(error -> get(String.format(FALLBACK_URL, day)));
    }

    private CompletableFuture<Map<String, BigDecimal>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(
                                new IOException("HTTP " + response.statusCode() + " for " + url));
                    }
                    return parse(response.body());
                });
    }

    /** Pulls every three-letter code and its rate out of the reply. */
    private Map<String, BigDecimal> parse(String body) {
        Matcher block = USD_BLOCK.matcher(body);
        if (!block.find()) {
            throw new CompletionException(new IOException("No rates in the reply"));
        }

        Map<String, BigDecimal> rates = new HashMap<>();
        Matcher entry = ONE_RATE.matcher(block.group(1));
        while (entry.find()) {
            try {
                BigDecimal rate = new BigDecimal(entry.group(2));
                if (rate.signum() > 0) {
                    rates.put(entry.group(1).toUpperCase(Locale.ROOT), rate);
                }
            } catch (NumberFormatException ignored) {
                // skip a malformed figure
            }
        }
        if (rates.isEmpty()) {
            throw new CompletionException(new IOException("No usable rates in the reply"));
        }
        return rates;
    }
}
