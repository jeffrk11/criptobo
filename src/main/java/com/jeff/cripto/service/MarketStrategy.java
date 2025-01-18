package com.jeff.cripto.service;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;

import java.util.logging.Logger;

public class MarketStrategy implements BuyStrategy {
    static Logger log = Logger.getLogger(MarketStrategy.class.getName());

    @Override
    public Order buy() {
        log.warning(String.format("Buying %s in %s", ConfigLoader.get("bot.amount_to_trade"), ConfigLoader.get("bot.symbol")));
        try{
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                    "symbol="+ConfigLoader.get("bot.symbol"),
                    "side=BUY",
                    "type=MARKET",
                    "quoteOrderQty="+ConfigLoader.get("bot.amount_to_trade"),
                    String.format("timestamp=%s", System.currentTimeMillis()));
            Order order =  OrderParse.parseFrom(response);
            order.setPaidValue(response.get("cummulativeQuoteQty").getAsBigDecimal());
            log.info(String.format("bought for %s", order.getPaidValue()));
            return order;
        }catch (Exception e){
            log.severe("Error when buying");
            log.severe(e.getMessage());
        }
        return null;
    }
}
