package com.mineraltree.api.marshal;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.mineraltree.api.dto.ApiDto;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles JSON serialization and deserialization for API needs */
public class Marshal {
  /** Configures Jackson to parse JSON as we need for the API requirements */
  public static final ObjectMapper MAPPER =
      new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .enable(JsonParser.Feature.ALLOW_MISSING_VALUES)
          .registerModule(new GuavaModule())
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .setVisibility(
              VisibilityChecker.Std.defaultInstance()
                  .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                  .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                  .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                  .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

  private static final Logger log = LoggerFactory.getLogger(Marshal.class);

  /**
   * Returns an unmarshaller used by Akka HTTP to convert incoming JSON strings into java objects.
   */
  public static <T extends ApiDto> Unmarshaller<HttpEntity, T> unmarshaller(Class<T> clazz) {
    return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
        .thenApply(s -> fromJSON(s, clazz));
  }

  public static <T> Unmarshaller<HttpEntity, T> jacksonUnmarshaller(Class<T> clazz) {
    return Jackson.unmarshaller(clazz);
  }

  public static Unmarshaller<HttpEntity, Map<String, Object>> mapUnmarshaller() {
    return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
        .thenApply(
            s -> {
              try {
                return MAPPER.readValue(s, new TypeReference<Map<String, Object>>() {});
              } catch (IOException e) {
                throw new IllegalArgumentException("Exception mapping JSON to map", e);
              }
            });
  }

  /**
   * Returns a marshaller used by Akka HTTP to convert java objects into JSON string
   * representations.
   */
  public static <T extends ApiDto> Marshaller<T, RequestEntity> marshaller() {
    return Marshaller.wrapEntity(
        u -> toJSON(MAPPER, u), Marshaller.stringToEntity(), MediaTypes.APPLICATION_JSON);
  }

  public static <T> Marshaller<T, RequestEntity> jacksonMarshaller() {
    return Jackson.marshaller();
  }

  /**
   * Performs the actual conversion of an object into a string
   *
   * @param mapper the Jackson object mapper definition to use
   * @param object the java object to convert
   * @return the JSON representation of {@code object}
   */
  private static String toJSON(ObjectMapper mapper, Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot marshal to JSON: " + object, e);
    }
  }

  /**
   * Performs the actual conversion of a JSON string into the corresponding java object.
   *
   * @param json the string containing the JSON to convert
   * @param expectedType the class declaring the java object type to convert the json into
   * @param <T> the class used to convert into
   * @return the converted object
   */
  private static <T extends ApiDto> T fromJSON(String json, Class<T> expectedType) {
    try {
      T parsed = Marshal.MAPPER.readerFor(expectedType).readValue(json);
      parsed.validate();
      return parsed;
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Cannot unmarshal JSON as " + expectedType.getSimpleName(), e);
    }
  }
}
