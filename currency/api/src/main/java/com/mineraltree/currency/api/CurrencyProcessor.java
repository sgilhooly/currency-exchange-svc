package com.mineraltree.currency.api;

import com.mineraltree.currency.dto.CurrencyRates;
import java.util.concurrent.CompletionStage;

public interface CurrencyProcessor {
  CompletionStage<CurrencyRates> getCurrencyRates(String base);
}
