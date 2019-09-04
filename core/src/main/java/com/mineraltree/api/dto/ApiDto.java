package com.mineraltree.api.dto;

/**
 * Base class for all API DTO objects. Any object representing a request or response payload must
 * implement this interface.
 */
public interface ApiDto {

  /**
   * Ensures the object is valid and consistent. This method is called after the object has been
   * constructed to ensure that it contains all the required attributes and that they have valid
   * values. This ensures that the JSON used to construct this object contained a valid set of
   * properties to describe it.
   *
   * @throws RuntimeException implementations should throw an exception (usually {@code
   *     IllegalArgumentException}) if any of the properties are invalid
   */
  void validate();
}
