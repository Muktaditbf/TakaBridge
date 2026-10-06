package com.takabridge.listener;

import com.takabridge.dao.CurrencyDAO;
import com.takabridge.service.LiveRateClient;
import com.takabridge.service.RateHistoryClient;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps the exchange rates fresh, with no button to press.
 *
 * Tomcat calls contextInitialized() once, when TakaBridge starts. From then on
 * a background thread wakes up every hour and, if the rates are more than a day
 * old, downloads new ones and saves them into Oracle.
 *
 * Checking hourly instead of sleeping a full day means a failed download (for
 * example, no internet at startup) is retried an hour later, not tomorrow.
 * If the internet stays down, the last saved rates simply keep being used.
 *
 * The same thread also keeps the last 30 days of past rates in memory, for the
 * trend charts, so those pages open without waiting on the network.
 */
@WebListener
public class DailyRateUpdater implements ServletContextListener {

    /** How old the rates may get before new ones are fetched. */
    private static final Duration MAX_AGE = Duration.ofHours(24);

    /** How often the background thread wakes up to check. */
    private static final long CHECK_EVERY_MINUTES = 60;

    /** How many past days the trend charts can show. */
    public static final int HISTORY_DAYS = 30;

    private final LiveRateClient client = new LiveRateClient();
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final RateHistoryClient history = new RateHistoryClient();

    private ScheduledExecutorService timer;
    private ServletContext context;

    /** Time of the last successful update; null until the first one. */
    private volatile Instant lastSuccess;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        context = event.getServletContext();

        // Shared with TrendsServlet and HomeServlet through the context.
        context.setAttribute(RateHistoryClient.CONTEXT_KEY, history);

        // A daemon thread never stops Tomcat from shutting down.
        timer = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "takabridge-rate-updater");
            thread.setDaemon(true);
            return thread;
        });

        // First run straight away, then every hour after that.
        timer.scheduleWithFixedDelay(this::runChecks, 0, CHECK_EVERY_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Stop the thread, or it would keep running after TakaBridge is undeployed.
        if (timer != null) {
            timer.shutdownNow();
        }
        context.removeAttribute(RateHistoryClient.CONTEXT_KEY);
        history.close();
    }

    /** One hourly wake-up: today's rates first, then the past days for the charts. */
    private void runChecks() {
        updateIfDue();
        try {
            int days = history.lastDays(HISTORY_DAYS).size();
            context.log("TakaBridge: " + days + " days of rate history ready for the charts");
        } catch (RuntimeException e) {
            context.log("TakaBridge: could not load rate history. Reason: " + e.getMessage());
        }
    }

    /** Runs on the background thread. It must never throw, or the timer stops for good. */
    private void updateIfDue() {
        if (lastSuccess != null && lastSuccess.plus(MAX_AGE).isAfter(Instant.now())) {
            return;   // still fresh
        }
        try {
            Map<String, BigDecimal> rates = client.fetchRatesPerUsd();
            int updated = currencyDAO.updateRates(rates);
            lastSuccess = Instant.now();
            context.log("TakaBridge: live rates saved for " + updated + " currencies");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // Tomcat is shutting down
        } catch (Exception e) {
            context.log("TakaBridge: could not update live rates, keeping the saved ones. "
                      + "Will try again in " + CHECK_EVERY_MINUTES + " minutes. Reason: " + e.getMessage());
        }
    }
}
