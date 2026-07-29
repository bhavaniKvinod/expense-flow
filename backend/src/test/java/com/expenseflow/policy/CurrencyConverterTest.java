package com.expenseflow.policy;

import com.expenseflow.domain.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyConverterTest {

    @Test
    void usdAmountPassesThroughWithoutNeedingRate() {
        BigDecimal result = CurrencyConverter.toUsd(new BigDecimal("10.00"), Currency.USD, null, null);
        assertThat(result).isEqualByComparingTo("10.00");
    }

    @Test
    void nullAmountReturnsZero() {
        BigDecimal result = CurrencyConverter.toUsd(null, Currency.INR, new BigDecimal("83.00"), null);
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nonUsdAmountConvertsUsingRate() {
        BigDecimal result = CurrencyConverter.toUsd(new BigDecimal("830.00"), Currency.INR, new BigDecimal("83.00"), null);
        assertThat(result).isEqualByComparingTo("10.00");
    }

    @Test
    void nullRateForNonUsdAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.INR, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToInrRate");
    }

    @Test
    void zeroRateForNonUsdAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.INR, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToInrRate");
    }

    @Test
    void negativeRateForNonUsdAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.INR, new BigDecimal("-1"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToInrRate");
    }

    @Test
    void eurAmountConvertsUsingEurRate() {
        BigDecimal result = CurrencyConverter.toUsd(new BigDecimal("92.00"), Currency.EUR, null, new BigDecimal("0.9200"));
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void nullEurRateForEurAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.EUR, new BigDecimal("83.00"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToEurRate");
    }

    @Test
    void zeroEurRateForEurAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.EUR, null, BigDecimal.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToEurRate");
    }

    @Test
    void negativeEurRateForEurAmountThrows() {
        assertThatThrownBy(() -> CurrencyConverter.toUsd(new BigDecimal("100.00"), Currency.EUR, null, new BigDecimal("-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usdToEurRate");
    }
}
