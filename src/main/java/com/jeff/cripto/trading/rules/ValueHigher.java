package com.jeff.cripto.trading.rules;

import com.jeff.cripto.trading.context.LeverageContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValueHigher implements BuyRule<LeverageContext>{

    @Override
    public boolean shouldBuy(LeverageContext context) {
        return context.getCurrentPrice().compareTo(context.getCheckpoint().getTargetValue()) >= 0 && context.getCheckpoint().isGoingDown();
    }

}
