package com.jeff.cripto.trading.rules;

import com.jeff.cripto.model.Checkpoint;
import com.jeff.cripto.trading.context.BotContext;
import com.jeff.cripto.trading.context.LeverageContext;
import com.jeff.cripto.trading.utils.BinanceService;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;


@AllArgsConstructor
public class ValueBelow implements Rule{

    private LeverageContext context;

    @Override
    public boolean checkRule() {

        BigDecimal currentPrice = BinanceService.getCurrentPrice();

        return currentPrice.compareTo(context.getCheckpoint().getTargetValue()) < 0 &&
                context.getCheckpoint().getUp();
    }

}
