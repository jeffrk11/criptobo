package com.jeff.cripto.trading.bot;

import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.strategy.BuyStrategy;
import com.jeff.cripto.trading.strategy.SellStrategy;

public interface Bot {

    void process();
    Order buy(BuyStrategy strategy);
    Order sell(SellStrategy strategy);

    default double calculateDifferencePercentage(double lastPrice, double currentPrice){
        return  100 - ((lastPrice * 100 ) / currentPrice);
    }
}
