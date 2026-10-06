package com.takabridge.service;

import com.takabridge.model.Currency;
import com.takabridge.model.Remittance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Works out how many taka a family in Bangladesh actually receives.
 *
 * Every channel takes money in two ways: a flat fee, and a rate a little worse
 * than the market rate. On top of what arrives, the Government of Bangladesh
 * pays a 2.5% cash incentive on remittances sent through banks and other
 * legal channels. The calculator applies all three and compares the channels.
 *
 * The fees are typical estimates for study, not quotes from a real provider.
 */
public class RemittanceCalculator {

    /** The government's cash incentive on legal remittances: 2.5%. */
    public static final BigDecimal GOVERNMENT_BONUS = new BigDecimal("0.025");

    /** Above this many US Dollars per transfer, the bank asks for documents. */
    public static final BigDecimal NO_DOCUMENTS_LIMIT_USD = new BigDecimal("5000");

    /** The three common ways of sending money home, with typical costs. */
    public enum Channel {
        BANK("Bank transfer", "15", "0.010"),
        EXCHANGE_HOUSE("Exchange house", "5", "0.015"),
        ONLINE_APP("Online transfer app", "3", "0.008");

        private final String label;
        private final BigDecimal feeUsd;       // flat fee, in US Dollars
        private final BigDecimal rateMargin;   // how much worse than the market rate

        Channel(String label, String feeUsd, String rateMargin) {
            this.label = label;
            this.feeUsd = new BigDecimal(feeUsd);
            this.rateMargin = new BigDecimal(rateMargin);
        }

        public String getLabel() {
            return label;
        }

        public BigDecimal getFeeUsd() {
            return feeUsd;
        }

        public BigDecimal getRateMargin() {
            return rateMargin;
        }
    }

    /** Every channel, the one that gives the family the most taka first. */
    public List<Remittance> compare(BigDecimal amount, Currency from, Currency bdt, Currency usd) {
        List<Remittance> results = new ArrayList<>();
        for (Channel channel : Channel.values()) {
            results.add(calculate(channel, amount, from, bdt, usd));
        }
        results.sort(Comparator.comparing(Remittance::getFamilyGets).reversed());
        return results;
    }

    /** True when this transfer is above the no-documents limit. */
    public boolean needsDocuments(BigDecimal amount, Currency from, Currency usd) {
        BigDecimal inUsd = amount.multiply(ConversionService.crossRate(from, usd));
        return inUsd.compareTo(NO_DOCUMENTS_LIMIT_USD) > 0;
    }

    private Remittance calculate(Channel channel, BigDecimal amount,
                                 Currency from, Currency bdt, Currency usd) {

        // The fee is set in dollars, so turn it into the sending currency.
        BigDecimal fee = channel.getFeeUsd()
                .multiply(ConversionService.crossRate(usd, from))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal exchanged = amount.subtract(fee).max(BigDecimal.ZERO);

        BigDecimal marketRate = ConversionService.crossRate(from, bdt);
        BigDecimal channelRate = marketRate.multiply(BigDecimal.ONE.subtract(channel.getRateMargin()));

        BigDecimal taka = exchanged.multiply(channelRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = taka.multiply(GOVERNMENT_BONUS).setScale(2, RoundingMode.HALF_UP);

        return new Remittance(
                channel.getLabel(),
                from.getCode(),
                from.getSymbol(),
                amount.setScale(2, RoundingMode.HALF_UP),
                fee,
                exchanged.setScale(2, RoundingMode.HALF_UP),
                marketRate.setScale(4, RoundingMode.HALF_UP),
                channelRate.setScale(4, RoundingMode.HALF_UP),
                taka,
                bonus,
                taka.add(bonus));
    }
}
