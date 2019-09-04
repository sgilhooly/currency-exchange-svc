package com.mineraltree.currency.service;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.mineraltree.currency.ControlCode;
import com.mineraltree.currency.GetRatesRequest;
import com.mineraltree.currency.ServiceNotReady;
import com.mineraltree.currency.dto.CurrencyRates;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Caches the results of currency rate lookups to handle requests for the information immediately.
 */
public class RateCache extends AbstractActor {

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final Map<String, CurrencyRates> currentRates;
  private ActorRef rateSource;
  private final Props rateSourceProps;
  private final Duration refreshInterval;

  public static Props mkProps(Props rateSourceProps, Duration refreshInterval) {
    return Props.create(RateCache.class, rateSourceProps, refreshInterval);
  }

  RateCache(Props rateSourceProps, Duration refreshInterval) {
    this.currentRates = new TreeMap<>();
    this.rateSourceProps = rateSourceProps;
    this.refreshInterval = refreshInterval;
  }

  @Override
  public void preStart() throws Exception {
    super.preStart();

    rateSource = getContext().actorOf(rateSourceProps, "rate-source");
    // Tell the rate loader to send us the current rates. It will reply with a CurrencyRates object

    getContext()
        .system()
        .scheduler()
        .schedule(
            refreshInterval,
            refreshInterval,
            getSelf(),
            ControlCode.REFRESH,
            getContext().dispatcher(),
            getSelf());
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
        .match(CurrencyRates.class, this::updateCurrentRates)
        .match(ControlCode.class, this::handleControl)
        .match(GetRatesRequest.class, this::retrieveRates)
        .build();
  }

  private void retrieveRates(GetRatesRequest request) {
    boolean isBaseLoaded = currentRates.containsKey(request.getBase());
    if (request.responseExpected() && isBaseLoaded) {
      getSender().tell(currentRates.get(request.getBase()), getSelf());
    } else {
      if (request.responseExpected()) {
        getSender().tell(new Status.Failure(new ServiceNotReady()), getSelf());
      }
      // TODO: don't let bursts of requests get through to the loader (only load once)
      log.info("[base={}] First request for currency rates. Fetching now.", request.getBase());
      rateSource.tell(request, getSelf());
    }
  }

  private void updateCurrentRates(CurrencyRates rates) {
    log.info(
        "[base={}] Updated current rates from provider {}",
        rates.getBaseCurrency(),
        rates.getProvider());
    currentRates.put(rates.getBaseCurrency(), rates);
  }

  private void handleControl(ControlCode code) {
    if (code == ControlCode.REFRESH) {
      rateSource.tell(ControlCode.REFRESH, getSelf());
    } else {
      unhandled(code);
    }
  }
}
