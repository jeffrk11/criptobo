package com.jeff.cripto.trading.context;

import com.jeff.cripto.model.Checkpoint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeverageContext extends BotContext{
    private Checkpoint checkpoint;

    public LeverageContext(){
        checkpoint = new Checkpoint();
    }
}
