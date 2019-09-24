package com.mineraltree.currency.service;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.mineraltree.currency.ControlCode;
import com.mineraltree.currency.GetRateFailedResponse;
import com.mineraltree.currency.RatesUnavailable;
import com.mineraltree.currency.api.CurrencyProcessor;
import com.mineraltree.currency.providers.BaseProvider;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

/**
 * Looks up the currency exchange rates for a particular base currency. Fetches the current exchange
 * rates from the configured provider(s) and returns them to the requester. This will attempt to
 * fetch the rates from the first configured provider and then, if that fails, fall back to
 * alternative providers until one succeeds.
 */
public class RateLoader extends AbstractActor {

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final List<BaseProvider> providers;
  private final String base;
  private Iterator<BaseProvider> providerSelector;
  private BaseProvider activeProvider;

  public static Props mkProps(String base, List<CurrencyProcessor> providers) {
    return Props.create(RateLoader.class, base, providers);
  }

  RateLoader(String base, List<BaseProvider> providers) {
    this.base = base;
    this.providers = providers;
    providerSelector = providers.iterator();
    if (!providerSelector.hasNext()) {
      throw new IllegalArgumentException(
          "No providers configured. At least one exchange rate provider must be configured");
    }
    activeProvider = providerSelector.next();
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
        .match(RatesUnavailable.class, this::getNextProvider)
        .match(ControlCode.class, this::handleControlCode)
        .build();
  }

  private void handleControlCode(ControlCode code) {
    switch (code) {
      case REFRESH:
      case GET_CURRENT:
        loadCurrentRates();
        break;

      case PROVIDER_RESET:
        resetActiveProvider();
        break;

      default:
        unhandled(code);
        break;
    }
  }

  private void loadCurrentRates() {
    final ActorRef replyTo = getSender();
    final ActorRef self = getSelf();
    activeProvider
        .getCurrencyRates(base)
        .whenComplete(
            (rate, err) -> {
              if (null != err) {
                log.debug(
                    "[base={}, provider={}] Rate lookup failed: {}",
                    base,
                    activeProvider.getProviderName(),
                    err);
                self.tell(new RatesUnavailable(err), replyTo);
              } else {
                // We got successful rates, tell the requester and reset the active provider
                // so the next request starts at the beginning
                log.debug("[base={}] Rates retrieved successfully", base);
                replyTo.tell(rate, self);
                resetActiveProvider();
              }
            });
  }

  private void getNextProvider(Throwable lastFailure) {
    if (providerSelector.hasNext()) {
      BaseProvider formerProvider = activeProvider;
      activeProvider = providerSelector.next();
      log.debug(
          "[base={}, provider={}] Provider returned failure status, switching to provider [{}]: {}",
          base,
          formerProvider.getProviderName(),
          activeProvider.getProviderName(),
          lastFailure.toString());
      loadCurrentRates();
    } else {
      log.error(
          "Unable to fetch current exchange rates from all configured providers. Rates may be stale.");

      getSender().tell(new GetRateFailedResponse(base), getSelf());

      resetActiveProvider();
    }
  }

  private void resetActiveProvider() {
    providerSelector = providers.iterator();
    activeProvider = providerSelector.next();
  }
}
