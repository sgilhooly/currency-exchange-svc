package com.mineraltree.currency;

public class GetRateFailedResponse {

  private final String base;

  public GetRateFailedResponse(String base) {
    this.base = base;
  }

  public String getBase() {
    return base;
  }
}
