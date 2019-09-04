package com.mineraltree.api.marshal;

import static com.mineraltree.utils.Ensure.verify;
import static com.mineraltree.utils.Ensure.verifyNotEmpty;
import static com.mineraltree.utils.Ensure.verifyNotNull;

import akka.actor.ActorSystem;
import akka.http.scaladsl.marshalling.Marshalling;
import com.mineraltree.api.dto.ApiDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class MarshalTest {

  private static ActorSystem system;

  @BeforeAll
  public static void setupClass() {
    system = ActorSystem.create("TestSystem");
  }

  public static class TestDto implements ApiDto {
    private final String stringValue;
    private final int numericValue;
    private final OffsetDateTime timeValue;
    private final List<String> listValue;
    private final String ignored = null;

    private TestDto() {
      this.stringValue = null;
      this.numericValue = 0;
      this.listValue = null;
      this.timeValue = null;
    }

    public TestDto(
        String stringValue, int numericValue, List<String> listValue, OffsetDateTime timeValue) {
      this.stringValue = stringValue;
      this.numericValue = numericValue;
      this.listValue = listValue;
      this.timeValue = timeValue;
    }

    @Override
    public void validate() {
      verifyNotEmpty(stringValue, "stringValue");
      verify(numericValue, n -> n > 0, "numericValue", "Positive integer expected");
      verifyNotEmpty(listValue, "listValue");
      verifyNotNull(timeValue, "timeValue");
      verify(ignored, Objects::isNull, "ignored", "Ignored values should remain untouched");
    }
  }

  @Test
  public void testMarshal() throws Exception {

    String jsonInput =
        "{'stringValue':'test value', 'numericValue`: 2, 'timeValue': '10/1/2018 3:44:82.121 EST', 'listValue': ['one', 'two', 'three']}"
            .replaceAll("'", "\"");

    ApiDto input =
        new TestDto(
            "test value",
            22,
            Arrays.asList("one", "two", "three"),
            OffsetDateTime.of(2018, 10, 3, 14, 55, 19, 934629, ZoneOffset.of("-0500")));

    Future<scala.collection.immutable.List<Marshalling<Object>>> marshalFuture =
        Marshal.marshaller().asScalaCastOutput().apply(input, system.dispatcher());

    scala.collection.immutable.List<Marshalling<Object>> result =
        Await.result(marshalFuture, Duration.apply(3, TimeUnit.SECONDS));

    System.out.println("Got a " + result);
  }
}
