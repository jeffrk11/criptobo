package com.jeff.cripto.trading.rules;

import com.jeff.cripto.trading.context.BotContext;

public interface BuyRule<T extends BotContext> {

    boolean shouldBuy(T context);
}
