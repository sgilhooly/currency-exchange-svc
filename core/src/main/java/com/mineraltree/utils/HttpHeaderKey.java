package com.mineraltree.utils;

public enum HttpHeaderKey {
  DESTINY("X-MT-Destiny"),
  SESSIONID("X-MT-Session");

  private final String headerName;

  HttpHeaderKey(String headerName) {
    this.headerName = headerName;
  }

  public String getKey() {
    return headerName;
  }
}
