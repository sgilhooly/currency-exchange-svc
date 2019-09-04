package com.mineraltree.currency;

public class GetRatesRequest {

  private final String base;
  private final boolean expectResponse;

  public GetRatesRequest(String base) {
    this(base, true);
  }

  public GetRatesRequest(String base, boolean expectResponse) {
    this.base = base;
    this.expectResponse = expectResponse;
  }

  public String getBase() {
    return this.base;
  }

  public boolean responseExpected() {
    return this.expectResponse;
  }
}
