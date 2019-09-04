package com.mineraltree.currency;

public class RatesUnavailable extends RuntimeException {

  public RatesUnavailable(Throwable cause) {
    super(
        "Cannot determine current exchange rates. Failed to retrieve rates from all configured providers",
        cause);
  }
}
