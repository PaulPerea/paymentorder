package com.example.entrevista_payment.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
    private final BigDecimal amount;
    private static final int SCALE = 2;

    public Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount no puede ser null");
        }
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public Money(Double amount) {
        this(BigDecimal.valueOf(amount));
    }

    public BigDecimal getAmount() { return amount; }

    public boolean isNegativeOrZero() {
        return amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
