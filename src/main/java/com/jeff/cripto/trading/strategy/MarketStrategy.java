package com.jeff.cripto.trading.strategy;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Setter
@Getter
@Slf4j
public class MarketStrategy implements BuyStrategy, SellStrategy {
    private BigDecimal quantity;

    @Override
    public Order buy() {
        log.warn("Buying {} in {}", ConfigLoader.get("bot.amount_to_trade"), ConfigLoader.get("bot.symbol"));
        try{
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                    "symbol="+ConfigLoader.get("bot.symbol"),
                    "side=BUY",
                    "type=MARKET",
                    "quoteOrderQty="+ConfigLoader.get("bot.amount_to_trade"),
                    String.format("timestamp=%s", System.currentTimeMillis()));
            Order order =  OrderParse.parseFrom(response);
            order.setPaidValue(response.get("cummulativeQuoteQty").getAsBigDecimal());
            log.info("bought for {}", order.getPaidValue());
            return order;
        }catch (Exception e){
            log.error("Error when buying");
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Order sell() {
        log.warn("Selling {} in {}", quantity, ConfigLoader.get("bot.symbol"));
        try{
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                    "symbol="+ConfigLoader.get("bot.symbol"),
                    "side=SELL",
                    "type=MARKET",
                    "quantity="+quantity.toString(),
                    String.format("timestamp=%s", System.currentTimeMillis()));
            Order order =  OrderParse.parseFrom(response);
            order.setPaidValue(response.get("cummulativeQuoteQty").getAsBigDecimal());
            log.info("Sold for {}", order.getPaidValue());
            return order;
        }catch (Exception e){
            log.error("Error when selling");
            log.error(e.getMessage());
        }
        return null;
    }
}
