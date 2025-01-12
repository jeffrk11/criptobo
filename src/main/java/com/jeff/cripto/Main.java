package com.jeff.cripto;

import com.jeff.cripto.config.ConfigLoader;

import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.service.BotService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    static Logger logger = Logger.getLogger(Main.class.getName());


    public static void main(String[] args) {
        logger.info(String.format("Starting %s lets make some moneeeey  \uD83D\uDCB0\uD83E\uDE99\uD83D\uDCB8", ConfigLoader.get("bot.name")));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int interval = Integer.parseInt(ConfigLoader.get("bot.core_interval"));

        BotService botService = new BotService(new OrderRepository());

        Runnable task = () -> {
            try{
                botService.analyze();

            }catch (Exception e){
                logger.severe("Something happen: "+e.getMessage());
            }
        };


        scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
//        JsonObject resp =  HttpHelper.doGet(ConfigLoader.get("binance.url")+"/ticker/price?symbol=BTCUSDC");



//'        logger.info(resp.toString());
//
//        String queryString = "symbol=BTCUSDC&side=BUY&type=MARKET&quoteOrderQty=10&timestamp="+System.currentTimeMillis();
//
//        // Sua SECRET_KEY da API Binance
//        String secretKey = ConfigLoader.get("binance.secret_key");
//
//        // Gerar a assinatura
//        String signature = Signature.generateSignature(queryString, secretKey);
//
//        // Exibir a assinatura gerada
//        System.out.println("Assinatura: " + signature);'


//        Connection connection = DataBaseConnection.getConnection();
//        Statement statement = connection.createStatement();
//        ResultSet result = statement.executeQuery("SELECT * FROM orders");
//
//        while (result.next()){
//            Order order = new Order(
//                    result.getInt("order_id"),
//                    result.getString("symbol"),
//                    result.getString("order_type"),
//                    result.getDouble("price"),
//                    result.getDouble("quantity"),
//                    result.getString("status"),
//                    result.getTimestamp("created_at"),
//                    result.getTimestamp("executed_at")
//            );
//        }

    }
}