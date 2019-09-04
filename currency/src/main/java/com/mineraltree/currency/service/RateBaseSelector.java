package com.mineraltree.currency.service;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.mineraltree.currency.ControlCode;
import com.mineraltree.currency.GetRatesRequest;
import com.mineraltree.currency.api.CurrencyProcessor;
import java.util.List;

/**
 * Manages a collection of RateLoaders for each base currency type. Creates one RateLoader for each
 * base currency rate request. When a rate request is received for a particular base currency, this
 * will forward the request to a child actor assigned to that base currency. If a child actor for
 * that currency does not exist, it will be created.
 */
public class RateBaseSelector extends AbstractActor {

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
  private final List<CurrencyProcessor> providers;

  public static Props mkProps(List<CurrencyProcessor> providers) {
    return Props.create(RateBaseSelector.class, providers);
  }

  private RateBaseSelector(List<CurrencyProcessor> providers) {
    this.providers = providers;
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
        .match(GetRatesRequest.class, this::getRatesForBase)
        .match(ControlCode.class, this::handleControlCode)
        .build();
  }

  private void getRatesForBase(GetRatesRequest baseRequest) {
    ActorRef baseLoader =
        getContext()
            .findChild(baseRequest.getBase())
            .orElseGet(
                () ->
                    getContext()
                        .actorOf(
                            RateLoader.mkProps(baseRequest.getBase(), providers),
                            baseRequest.getBase()));

    log.debug("[base={}] Forwarding request to loader", baseRequest.getBase());
    baseLoader.forward(ControlCode.GET_CURRENT, getContext());
  }

  private void handleControlCode(ControlCode code) {
    if (code == ControlCode.REFRESH) {
      for (ActorRef child : getContext().getChildren()) {
        child.tell(code, getSender());
      }
    } else {
      unhandled(code);
    }
  }
}
