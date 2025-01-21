package com.jeff.cripto.trading.strategy;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Setter
@Getter
public class MarketStrategy implements BuyStrategy, SellStrategy {
    static Logger log = Logger.getLogger(MarketStrategy.class.getName());

    private BigDecimal quantity;

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

    @Override
    public Order sell() {
        log.warning(String.format("Selling %s in %s", quantity, ConfigLoader.get("bot.symbol")));
        try{
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                    "symbol="+ConfigLoader.get("bot.symbol"),
                    "side=SELL",
                    "type=MARKET",
                    "quantity="+quantity.toString(),
                    String.format("timestamp=%s", System.currentTimeMillis()));
            Order order =  OrderParse.parseFrom(response);
            order.setPaidValue(response.get("cummulativeQuoteQty").getAsBigDecimal());
            log.info(String.format("Sold for %s", order.getPaidValue()));
            return order;
        }catch (Exception e){
            log.severe("Error when selling");
            log.severe(e.getMessage());
        }
        return null;
    }
}
