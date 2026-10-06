package com.takabridge.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads today's exchange rates from the internet.
 *
 * The source is the free ExchangeRate-API feed. It needs no account and no
 * key, refreshes once a day, and lists every rate per 1 US Dollar - exactly
 * the way the CURRENCIES table already stores them, so no conversion is needed.
 *
 * The HTTP client is the one built into Java, and the reply is read with a
 * regular expression, so this feature adds no new library to the project.
 */
public class LiveRateClient {

    /** Free, keyless feed with USD as the base currency. */
    private static final String SOURCE_URL = "https://open.er-api.com/v6/latest/USD";

    /** A slow network must never hang the background thread. */
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /** The "rates" object in the reply, e.g.  "rates":{"USD":1,"BDT":123.13,...}  */
    private static final Pattern RATES_BLOCK = Pattern.compile("\"rates\"\\s*:\\s*\\{([^}]*)\\}");

    /** One entry inside it, e.g.  "BDT":123.136994  */
    private static final Pattern ONE_RATE = Pattern.compile("\"([A-Z]{3})\"\\s*:\\s*([0-9.eE+-]+)");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Fetches the latest rates.
     *
     * @return currency code to units per 1 US Dollar, e.g. "BDT" to 123.136994
     * @throws IOException when the site cannot be reached or the reply is not usable
     */
    public Map<String, BigDecimal> fetchRatesPerUsd() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(SOURCE_URL))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("The rate service answered with HTTP " + response.statusCode());
        }

        String body = response.body();
        if (!body.contains("\"result\":\"success\"")) {
            throw new IOException("The rate service did not report success");
        }
        return parseRates(body);
    }

    /** Pulls every "CODE":number pair out of the "rates" object. */
    private Map<String, BigDecimal> parseRates(String body) throws IOException {
        Matcher block = RATES_BLOCK.matcher(body);
        if (!block.find()) {
            throw new IOException("The reply contained no rates");
        }

        Map<String, BigDecimal> rates = new HashMap<>();
        Matcher entry = ONE_RATE.matcher(block.group(1));
        while (entry.find()) {
            try {
                BigDecimal rate = new BigDecimal(entry.group(2));
                if (rate.signum() > 0) {
                    rates.put(entry.group(1), rate);
                }
            } catch (NumberFormatException ignored) {
                // A malformed figure is skipped; that currency keeps its old rate.
            }
        }

        if (rates.isEmpty()) {
            throw new IOException("The reply contained no usable rates");
        }
        return rates;
    }
}
