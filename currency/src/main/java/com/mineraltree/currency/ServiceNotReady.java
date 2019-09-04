package com.mineraltree.currency;

public class ServiceNotReady extends RuntimeException {

  public ServiceNotReady() {
    super("The service has not finished initialization. Wait a moment and retry the request.");
  }
}
