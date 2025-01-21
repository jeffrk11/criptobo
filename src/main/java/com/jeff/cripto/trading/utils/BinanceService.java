package com.jeff.cripto.trading.utils;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.utils.HttpHelper;

import java.math.BigDecimal;

public class BinanceService {

    public static BigDecimal getCurrentPrice(){
        JsonObject resp =  HttpHelper.doGet( String.format("%s/ticker/price?symbol=%s", ConfigLoader.get("binance.url"),ConfigLoader.get("bot.symbol")));
        return resp.get("price").getAsBigDecimal();
    }
}
