package com.jeff.cripto.trading.context;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public abstract class BotContext {
    protected BigDecimal currentPrice;

}
