package com.jeff.cripto.trading.context;

import com.jeff.cripto.model.Checkpoint;
import com.jeff.cripto.model.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeverageContext extends BotContext{
    private Checkpoint checkpoint;
    private Order lastOrder;
}
