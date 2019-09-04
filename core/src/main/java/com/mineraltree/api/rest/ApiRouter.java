package com.mineraltree.api.rest;

import akka.http.javadsl.server.Route;

/**
 * Defines class which supplies an API server with its routing definitions. A route tells the server
 * what to do with a request. It is organized in a tree of "directives" that can match parts of the
 * request and direct the request to something that can act on it and, ultimately, generate a
 * response.
 */
public interface ApiRouter {

  /**
   * Returns the route which contains all the logic for examining the request, performing an
   * appropriate action, and returning the corresponding response.
   */
  Route getRouter();
}
