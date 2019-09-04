package com.mineraltree.currency;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.mineraltree.currency.api.CurrencyProcessor;
import com.mineraltree.currency.dto.CurrencyRates;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public class CurrencyProcessorLogic implements CurrencyProcessor {

  private final ActorRef rateSupplier;
  private final ExecutionContext executionContext;

  public CurrencyProcessorLogic(ActorRef rateSupplier, ExecutionContext executionContext) {
    this.rateSupplier = rateSupplier;
    this.executionContext = executionContext;
  }

  @Override
  public CompletionStage<CurrencyRates> getCurrencyRates(String base) {
    GetRatesRequest request = new GetRatesRequest(base);

    Future<CurrencyRates> currencyRatesFuture =
        Patterns.ask(rateSupplier, request, Timeout.apply(10, TimeUnit.SECONDS))
            .map(
                o -> {
                  if (o instanceof CurrencyRates) {
                    return (CurrencyRates) o;
                  }
                  throw new RuntimeException(
                      "Invalid return type from rate supplier. Expected CurrencyRates but received "
                          + o.getClass().getName()
                          + ": "
                          + o.toString());
                },
                executionContext);
    return FutureConverters.toJava(currencyRatesFuture);
  }
}
