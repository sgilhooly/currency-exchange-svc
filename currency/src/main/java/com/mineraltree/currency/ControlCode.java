package com.mineraltree.currency;

public enum ControlCode {
  /** Used when a client wants to see the currently cached rates */
  GET_CURRENT,
  /** Used when it's time for the service to update its current rates */
  REFRESH,
  /** Signals that the current active provider should be reset to the default preferred provider */
  PROVIDER_RESET
}
