package com.jeff.cripto.service;

import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.logging.Logger;


@AllArgsConstructor
public class LimitStrategy implements SellStrategy {
    static Logger log = Logger.getLogger(LimitStrategy.class.getName());

    private BigDecimal targetPrice;
    private BigDecimal quantity;


    @Override
    public Order sell() {
        log.info(String.format("creating LIMIT order to: %s",  targetPrice));
        try{
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order", ConfigLoader.get("binance.url")),
                    "symbol="+ConfigLoader.get("bot.symbol"),
                    "side=SELL",
                    "type=LIMIT",
                    "timeInForce=GTC",
                    "quantity="+ String.format("%.8f",quantity).replace(",","."),
                    "price="+String.format("%.2f", targetPrice).replace(",","."),
                    String.format("timestamp=%s", System.currentTimeMillis()));

            return OrderParse.parseFromLimit(response);
        }catch (Exception e){
            log.severe("ERROR BUYING "+e.getMessage());
            return null;
        }
    }
}
