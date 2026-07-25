package com.expenseflow.policy;

import com.expenseflow.domain.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalises line item amounts to USD so reports mixing currencies (e.g. USD and INR)
 * can be totalled and checked against USD-denominated policy thresholds.
 */
public final class CurrencyConverter {

    private CurrencyConverter() {
    }

    public static BigDecimal toUsd(BigDecimal amount, Currency currency, BigDecimal usdToInrRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currency == Currency.USD) {
            return amount;
        }
        if (usdToInrRate == null || usdToInrRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Cannot convert " + currency + " to USD: usdToInrRate must be a positive value, was " + usdToInrRate);
        }
        return amount.divide(usdToInrRate, 2, RoundingMode.HALF_UP);
    }
}
