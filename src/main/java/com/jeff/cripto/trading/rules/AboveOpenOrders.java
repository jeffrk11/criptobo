package com.jeff.cripto.trading.rules;

import com.jeff.cripto.trading.context.LeverageContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class AboveOpenOrders implements BuyRule<LeverageContext> {

    @Override
    public boolean shouldBuy(LeverageContext context) {

        if(context.getLastOrder() == null)
            return true;

        boolean result = context.getCurrentPrice().compareTo(context.getLastOrder().getPrice()) > 0;

        if(!result)
            log.info("nao vai comprar, pq o preco e maior comparado com a ultima ordem : agora %s ultima %s".formatted(context.getCurrentPrice().toPlainString(), context.getLastOrder().getPrice().toPlainString()));

        return result;
    }
}
