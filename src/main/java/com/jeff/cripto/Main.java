package com.jeff.cripto;

import com.jeff.cripto.config.ConfigLoader;

import com.jeff.cripto.trading.bot.Bot;
import com.jeff.cripto.trading.strategy.MarketStrategy;
import com.jeff.cripto.trading.strategy.StrategyBot;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Slf4j
public class Main {



    public static void main(String[] args) {
        log.info("""
               
                                 █████╗ ██████╗ ██╗██████╗ ████████╗ █████╗ ██████╗  █████╗
                                ██╔══██╗██╔══██╗██║██╔══██╗╚══██╔══╝██╔══██╗██╔══██╗██╔══██╗
                                ██║  ╚═╝██████╔╝██║██████╔╝   ██║   ██║  ██║██████╦╝██║  ██║
                                ██║  ██╗██╔══██╗██║██╔═══╝    ██║   ██║  ██║██╔══██╗██║  ██║
                                ╚█████╔╝██║  ██║██║██║        ██║   ╚█████╔╝██████╦╝╚█████╔╝
                                 ╚════╝ ╚═╝  ╚═╝╚═╝╚═╝        ╚═╝    ╚════╝ ╚═════╝  ╚════╝
               """);
        log.info("Starting {} lets make some moneeeey  \uD83D\uDCB0\uD83E\uDE99\uD83D\uDCB8", ConfigLoader.get("bot.name"));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int interval = Integer.parseInt(ConfigLoader.get("bot.core_interval"));

        Bot bot = StrategyBot.valueOf(ConfigLoader.get("bot.strategy.type")).getBot();
        Runnable task = () -> {
            try{
                bot.process();
            }catch (Exception e){
                log.error("Something happen: {}", e.getMessage());
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
    }
}