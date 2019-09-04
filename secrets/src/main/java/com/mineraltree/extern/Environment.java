package com.mineraltree.extern;

public class Environment {

  public String getenv(String name) {
    return System.getenv(name);
  }
}
