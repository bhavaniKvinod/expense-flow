package com.expenseflow.policy;

import com.expenseflow.domain.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalises line item amounts to USD so reports mixing currencies (e.g. USD, INR, EUR, AED)
 * can be totalled and checked against USD-denominated policy thresholds.
 */
public final class CurrencyConverter {

    private CurrencyConverter() {
    }

    public static BigDecimal toUsd(BigDecimal amount, Currency currency, BigDecimal usdToInrRate,
                                    BigDecimal usdToEurRate, BigDecimal usdToAedRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currency == Currency.USD) {
            return amount;
        }
        String rateName;
        BigDecimal rate;
        switch (currency) {
            case EUR -> { rateName = "usdToEurRate"; rate = usdToEurRate; }
            case AED -> { rateName = "usdToAedRate"; rate = usdToAedRate; }
            default -> { rateName = "usdToInrRate"; rate = usdToInrRate; }
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Cannot convert " + currency + " to USD: " + rateName + " must be a positive value, was " + rate);
        }
        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }
}
