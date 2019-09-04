package com.mineraltree.utils;

// import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Methods for ensuring preconditions are met. A collection of static utility methods for verifying
 * variable values meet required conditions. This similar to Guava's Preconditions and Apache Verify
 * libraries except that it uses Java-8 functional constructs to allow for greater flexibility.
 */
public class Ensure {

  /**
   * Verifies the state of a parameter. Used in cases where a variable is checked at some
   * intermediate point in its lifecycle (as opposed to initialization).
   *
   * @param parameter the parameter value to check
   * @param check a test to run against the value to determine is validity
   * @param message a message to display as part of the error message if the value is not valid
   * @return returns {@code parameter} to allow for one-liner verification + assignment
   * @throws IllegalStateException if {@code check} returns false for {@code parameter}
   */
  public static <T> T verifyState(T parameter, Predicate<T> check, String message) {
    if (!check.test(parameter)) {
      throw new IllegalStateException("Invalid state with value '" + parameter + "': " + message);
    }
    return parameter;
  }

  /**
   * Verifies the value of a parameter meets a given validation check. Used to check a parameter,
   * usually when it is initialized, to ensure that it is created with a valid value.
   *
   * @param parameter the parameter to check
   * @param check a verification test to run against the parameter to determine if it is valid or
   *     not
   * @param parameterName the name of the variable supplied in the {@code parameter} argument. Used
   *     in error messages to indicate what parameter is invalid
   * @param message a message to include in the error message if the value is not valid
   * @return passes through {@code parameter} so this check can be used during assignment
   * @throws IllegalArgumentException if the verification check returns {@code false} for the {@code
   *     parameter} value
   */
  public static <T> T verify(
      T parameter, Predicate<T> check, String parameterName, String message) {

    if (!check.test(parameter)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid value [%s] for parameter named '%s': %s",
              parameter, parameterName, message));
    }
    return parameter;
  }

  /**
   * Shorthand method to verify a parameter is not null
   *
   * @see #verify(Object, Predicate, String, String)
   */
  // @Contract("null, _ -> fail; _, _ -> param1")
  public static <T> T verifyNotNull(T argument, String argumentName) {
    return verify(argument, Objects::nonNull, argumentName, "Value cannot be 'null'");
  }

  /**
   * Shorthand method to verify a parameter is not empty. Empty means {@code null} or a string with
   * nothing or only whitespace, or a collection with no items.
   *
   * @see #verify(Object, Predicate, String, String)
   */
  // @Contract("null, _ -> fail; _, _ -> param1")
  public static <T> T verifyNotEmpty(T argument, String argumentName) {
    verify(argument, Objects::nonNull, argumentName, "Value cannot be 'null'");
    if (argument instanceof String) {
      String stringArgument = (String) argument;
      verify(stringArgument, s -> !s.trim().isEmpty(), argumentName, "Value cannot be empty");
    } else if (argument instanceof Collection) {
      Collection<?> collectionArgument = (Collection<?>) argument;
      verify(
          collectionArgument,
          c -> !collectionArgument.isEmpty(),
          argumentName,
          "Value cannot be empty");
    }
    return argument;
  }
}
