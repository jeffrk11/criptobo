package com.jeff.cripto.trading.rules;

import com.jeff.cripto.trading.context.LeverageContext;
import com.jeff.cripto.trading.utils.BinanceService;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
public class ValueHigher implements Rule{

    private LeverageContext context;

    @Override
    public boolean checkRule() {

        BigDecimal currentPrice = BinanceService.getCurrentPrice();

        return currentPrice.compareTo(context.getCheckpoint().getTargetValue()) >= 0 && context.getCheckpoint().isGoingDown();
    }

}
