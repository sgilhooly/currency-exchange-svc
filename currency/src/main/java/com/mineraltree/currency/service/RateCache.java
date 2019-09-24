package com.mineraltree.currency.service;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mineraltree.currency.ControlCode;
import com.mineraltree.currency.GetRateFailedResponse;
import com.mineraltree.currency.GetRatesRequest;
import com.mineraltree.currency.RatesUnavailable;
import com.mineraltree.currency.dto.CurrencyRates;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Caches the results of currency rate lookups to handle requests for the information immediately.
 */
public class RateCache extends AbstractActor {

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final Map<String, CurrencyRates> currentRates;
  private ActorRef rateSource;
  private final Props rateSourceProps;
  private final Duration refreshInterval;
  private final Set<String> inFlight = new HashSet<>();
  private final LoadingCache<String, String> failedCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(1, TimeUnit.HOURS)
          .build(
              new CacheLoader<String, String>() {

                @Override
                public String load(String key) {
                  return key;
                }
              });

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
        .match(String.class, this::retryRetrieveRates)
        .match(GetRateFailedResponse.class, this::processRetrievalFail)
        .build();
  }

  private void retrieveRates(GetRatesRequest request) {
    if (!request.getBase().matches("[A-Za-z]{3}$")) {
      getSender()
          .tell(new IllegalArgumentException("Currency must be a 3 letter string"), getSelf());
      return;
    }

    boolean isBaseLoaded = currentRates.containsKey(request.getBase());
    if (request.responseExpected() && isBaseLoaded) {
      getSender().tell(currentRates.get(request.getBase()), getSelf());
    } else {
      log.info("[base={}] First request for currency rates. Fetching now.", request.getBase());

      if (failedCache.asMap().containsKey(request.getBase())) {
        getSender()
            .tell(new RatesUnavailable(new RuntimeException("Could not fetch rates")), getSelf());
      }

      if (!inFlight.contains(request.getBase())) {
        rateSource.tell(request, getSelf());
        inFlight.add(request.getBase());
      }

      if (request.responseExpected()) {
        scheduleRetryRetrieveRates(request.getBase());
      }
    }
  }

  private void updateCurrentRates(CurrencyRates rates) {
    if (failedCache.asMap().containsKey(rates.getBaseCurrency())) {
      failedCache.invalidate(rates.getBaseCurrency());
    }

    log.info(
        "[base={}] Updated current rates from provider {}",
        rates.getBaseCurrency(),
        rates.getProvider());
    currentRates.put(rates.getBaseCurrency(), rates);
    inFlight.remove(rates.getBaseCurrency());
  }

  private void handleControl(ControlCode code) {
    if (code == ControlCode.REFRESH) {
      rateSource.tell(ControlCode.REFRESH, getSelf());
    } else {
      unhandled(code);
    }
  }

  private void scheduleRetryRetrieveRates(String base) {
    getContext()
        .system()
        .scheduler()
        .scheduleOnce(
            Duration.ofMillis(100), getSelf(), base, getContext().dispatcher(), getSender());
  }

  private void retryRetrieveRates(String base) {
    if (currentRates.containsKey(base)) {
      getSender().tell(currentRates.get(base), getSelf());
    } else if (failedCache.asMap().containsKey(base)) {
      getSender()
          .tell(new RatesUnavailable(new RuntimeException("Could not fetch rates")), getSelf());
    } else {
      scheduleRetryRetrieveRates(base);
    }
  }

  private void processRetrievalFail(GetRateFailedResponse response) throws ExecutionException {
    // 'get' is a misleading name here, this actually loads the base into the fail cache
    failedCache.get(response.getBase());
  }
}
