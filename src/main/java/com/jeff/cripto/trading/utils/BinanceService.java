package com.jeff.cripto.trading.utils;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.utils.HttpHelper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BinanceService {

    private static final Map<String, BigDecimal> prices = new HashMap<>();
    private static final Map<String, Long> lastCall = new HashMap<>();

    public static BigDecimal getCurrentPrice(){
        return getCurrentPrice(ConfigLoader.get("bot.symbol"));
    }

    public static BigDecimal getCurrentPrice(String symbol){
        if (prices.get(symbol) != null)
            if(TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - lastCall.getOrDefault(symbol, 0L))) < ConfigLoader.getDouble("bot.core_interval") - 5)
                return prices.get(symbol);

        lastCall.put(symbol, System.currentTimeMillis());
        JsonObject resp =  HttpHelper.doGet( String.format("%s/ticker/price?symbol=%s", ConfigLoader.get("binance.url"),symbol));
        prices.put(symbol, resp.get("price").getAsBigDecimal());

        return prices.get(symbol);
    }
}
