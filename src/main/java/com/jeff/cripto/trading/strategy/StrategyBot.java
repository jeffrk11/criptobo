package com.jeff.cripto.trading.strategy;

import com.jeff.cripto.trading.bot.Bot;
import com.jeff.cripto.trading.bot.LevaregeBot;
import com.jeff.cripto.trading.bot.TargetBot;
import lombok.Getter;

@Getter
public enum StrategyBot {

    TARGET("target", new TargetBot()),
    LEVAREGE("levarege", new LevaregeBot());

    private final String name;
    private final Bot bot;

    StrategyBot(String name, Bot bot) {
        this.name = name;
        this.bot = bot;
    }
}
