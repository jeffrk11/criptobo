package com.jeff.cripto.trading.rules;

import com.jeff.cripto.trading.context.LeverageContext;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ValueBelow implements Rule{

    private LeverageContext context;

    @Override
    public boolean checkRule() {
        return context.getCurrentPrice().compareTo(context.getCheckpoint().getTargetValue()) < 0 && context.getCheckpoint().getUp();
    }

}
