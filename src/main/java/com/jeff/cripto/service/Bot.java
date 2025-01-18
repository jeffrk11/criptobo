package com.jeff.cripto.service;

import com.jeff.cripto.model.Order;

public interface Bot {

    void process();
    Order buy(BuyStrategy strategy);
    Order sell(SellStrategy strategy);
}
