package com.mineraltree.currency;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mineraltree.api.rest.ApiRouter;
import com.mineraltree.currency.api.CurrencyProcessor;
import com.mineraltree.currency.providers.FixerProvider;
import com.mineraltree.currency.providers.OpenExchangeProvider;
import com.mineraltree.currency.rest.CurrencyApiRouter;
import com.mineraltree.currency.service.RateBaseSelector;
import com.mineraltree.currency.service.RateCache;
import com.mineraltree.http.RequestUtils;
import com.mineraltree.secret.SecretService;
import com.mineraltree.service.ServiceMain;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.time.Duration;
import java.util.List;

public class CurrencyServiceMain extends ServiceMain {

  public CurrencyServiceMain() {
    super("currency-service");
  }

  protected void initializeServers(ActorSystem system) {
    RequestUtils requestUtils = new RequestUtils(system);
    SecretService.initializeVault(config);

    Config secretConfig =
        SecretService.getVault().getSecrets(config.getString("currency-key-vault"));
    Config fullConfig = config.withFallback(secretConfig);

    List<? extends ConfigObject> providerConfigList = config.getObjectList("providers");

    Builder<CurrencyProcessor> providerListBuilder = ImmutableList.builder();
    for (ConfigObject providerConfig : providerConfigList) {
      switch (providerConfig.get("type").unwrapped().toString()) {
        case "FIXER":
          providerListBuilder.add(
              new FixerProvider(providerConfig.toConfig(), requestUtils, fullConfig));
          break;

        case "OPENEXCHANGE":
          providerListBuilder.add(
              new OpenExchangeProvider(providerConfig.toConfig(), requestUtils, fullConfig));
          break;

        default:
          throw new RuntimeException(
              "Configuration file contains invalid provider definition. Type '"
                  + providerConfig.get("type").unwrapped()
                  + "' is not recognized.");
      }
    }
    Duration refreshInterval = config.getDuration("refresh-every");
    List<CurrencyProcessor> allProviders = providerListBuilder.build();
    Props baseLoader = RateBaseSelector.mkProps(allProviders);
    ActorRef mainLoader =
        system.actorOf(RateCache.mkProps(baseLoader, refreshInterval), "top-cache");

    preloadRates(config, mainLoader);

    CurrencyProcessor newCurrency = new CurrencyProcessorLogic(mainLoader, system.dispatcher());
    ApiRouter router = new CurrencyApiRouter.Builder().setCurrencyProcessor(newCurrency).build();
    startApiServer(router, config.getConfig("server"));
  }

  private void preloadRates(Config config, ActorRef loader) {
    config.getStringList("preload-rates").stream()
        .map(b -> new GetRatesRequest(b, false))
        .forEach(r -> loader.tell(r, ActorRef.noSender()));
  }

  public static void main(String[] args) {
    CurrencyServiceMain service = new CurrencyServiceMain();
    service.initialize();
  }
}
