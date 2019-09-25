package com.mineraltree.currency;

public class RetryRatesRequest {
    private final String base;
    private final Integer attemptNum;

    public RetryRatesRequest(String base, Integer attemptNum) {
        this.base = base;
        this.attemptNum = attemptNum;
    }

    public String getBase() {
        return this.base;
    }

    public Integer getAttemptNum() {
        return this.attemptNum;
    }
}
