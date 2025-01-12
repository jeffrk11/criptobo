package com.jeff.cripto.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@AllArgsConstructor
public class BotService {
    static Logger log = Logger.getLogger(BotService.class.getName());
    private OrderRepository orderRepository;


    public void analyze(){
        //check last price
        double currentPrice = getCurrentPrice();
        //log.info(String.format("Current price: %s - %s", currentPrice, ConfigLoader.get("bot.symbol")));


        //no more orders left
        if(getLastPendingOrders().isEmpty()){
            buy();
            return;
        }
        double lastTradePrice = getLastTradePrice();
        double differencePercentage = calculateDifferencePercentage(lastTradePrice, currentPrice);


        double baseDifference =  Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference"));
        double targetDifference = baseDifference * orderRepository.getCountOpenBuyOrders() * Double.parseDouble(ConfigLoader.get("bot.strategy.targetMultiply"));
        List<Order> openInternalOrders = orderRepository.getOpenOrdersByBot(ConfigLoader.get("bot.name"));

        long timeSinceLastBuy = getTimeSinceLastBuy(orderRepository.getLastBoughtOrdersByBot(ConfigLoader.get("bot.name")));

        //log.info(String.format("Difference between last trade price %s, cut number : %s", differencePercentage, targetDifference));

        printLog(currentPrice, openInternalOrders,lastTradePrice, differencePercentage, targetDifference, (int) (timeSinceLastBuy / 60000));

        checkOpenOrder(openInternalOrders);

        if(differencePercentage <= targetDifference){
            buy();
            return;
        }
        
        if(timeSinceLastBuy > Integer.parseInt(ConfigLoader.get("bot.strategy.stagnantMinutes")) * 10000L){
            buy();
        }


    }

    private void printLog(double currentPrice, List<Order> openOrders, double lastTradePrice, double differencePercentage, double targetDifference, int minutes) {
        StringBuilder openedOrdersText = new StringBuilder();
        for(int i = 0; i < openOrders.size(); i++){
            openedOrdersText.append("🔴 $%s 🔹 ".formatted(openOrders.get(i).getPrice()));
            if( i > 0 && i % 7 == 0) openedOrdersText.append("\n\t");
        }

        double targetPrice = (lastTradePrice * (targetDifference /100)) + lastTradePrice;

        String lastPricePosition = differencePercentage >= 0 ? "🌲" : "🔻";

        List<Order> lastOperationsList = orderRepository.getAllOrders(ConfigLoader.get("bot.name"), 20);

        StringBuilder lastOperationsText = new StringBuilder();
        for(int i = 0; i < lastOperationsList.size(); i +=2){
            LocalDateTime timeBuy = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOperationsList.get(i + 1).getExecutedAt()), ZoneId.of("Europe/Lisbon"));
            LocalDateTime timeSell = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOperationsList.get(i).getExecutedAt()), ZoneId.of("Europe/Lisbon"));

            boolean validDate = timeSell.getYear() != 1970;

            lastOperationsText.append("🟢 %s $%.2f ⇀ %02d/%02d-%02d:%02d  ❱ 🔴 %s $%.2f ⇀ %02d/%02d-%02d:%02d 💰 $%s \n\t"
                    .formatted(lastOperationsList.get(i+1).getStatus().equals("pending") ? "🔔" : "✔️", lastOperationsList.get(i+1).getPrice(),timeBuy.getDayOfMonth(),timeBuy.getMonthValue(), timeBuy.getHour(), timeBuy.getMinute(),
                               lastOperationsList.get(i).getStatus().equals("pending") ? "🔔" : "✔️", lastOperationsList.get(i).getPrice(),validDate ? timeSell.getDayOfMonth() : 0,validDate ? timeSell.getMonthValue() : 0, validDate ? timeSell.getHour() : 0, validDate ? timeSell.getMinute() : 0, lastOperationsList.get(i).getProfit()));
        }
        int time = Integer.parseInt(ConfigLoader.get("bot.strategy.stagnantMinutes"));
        StringBuilder timeText = new StringBuilder("▪".repeat(time));
        int auxMin = Math.min(minutes, time);
       timeText.replace(0, auxMin, "█".repeat(auxMin));
       timeText.append("❗%s min - 【%s max】".formatted(minutes, time));


        String logMesssage =
                """
                
                ╔══════════════════════════════════════════════════════════════════════════════════════════════
                ║💵  PREÇO ATUAL: $ %s 【 %.2f 】 %s     🪙 MOEDA: %s
                ╚══════════════════════════════════════════════════════════════════════════════════════════════
                 ⚖️  LAST PRICE:  $ %s
                 📍  NEXT PRICE:🟢$ %.2f 【 %.2f 】

                📑 OPEN ORDERS: %s
                    %s
                
                📜 LAST 10 OPERATIONS:
                    %s
                
                ⏰ TIME SINCE LAST BUY:
                   %s
                
                ============================================================
                📈 PROFIT:
                    📅 Profit today : $ %s
                    💰 Profit all:    $ %s
                ============================================================
                """.formatted(
                currentPrice, differencePercentage, lastPricePosition, ConfigLoader.get("bot.symbol"),
                        lastTradePrice,
                        targetPrice, targetDifference,
                        openOrders.size(), openedOrdersText,
                        lastOperationsText,
                        timeText,
                        orderRepository.getAllProfitByDate(LocalDateTime.now()),
                        orderRepository.getAllProfitByDate(null)
                );

        log.info(logMesssage);
    }

    private long getTimeSinceLastBuy(List<Order> orders){
        if(orders.isEmpty())
            return 0;
        return  System.currentTimeMillis() - orders.get(0).getCreatedAt();
//        log.info(String.format("%s minutes has passed since last buy", time / 60000 ));
//        return time > 3600000; //one hour
//        return time;
    }

    private void checkOpenOrder(List<Order> openInternalOrders){
        List<Order> openOrders = getOpenOrders();

        if(openInternalOrders.isEmpty())
            return;

        for(Order order : openInternalOrders){
            Optional<Order> findOrder =  openOrders.stream().filter(o -> o.getBinanceOrderId() == order.getBinanceOrderId())
                                                                .findFirst();
            if(findOrder.isPresent())
                continue;

            JsonArray fills = getFills(order.getBinanceOrderId());

//            if(fills == null){
//                updatingOrder(order);
//                orderRepository.updateOrder(order);
//                return;
//            }

            OrderParse.parseFills(order, fills);

            log.info(String.format("Order %s checked and filled updating database", order.getOrderId()));
            order.setStatus("executed");
//            order.setCommission(fills.get("commission").getAsDouble());
//            order.setCommissionAsset(fills.get("commissionAsset").getAsString());
            order.setBinanceStatus("FILLED");
//            order.setPaidValue(fills.get("quoteQty").getAsDouble());
            order.setPrice(fills.get(0).getAsJsonObject().get("price").getAsDouble());

            int dependentOrderId = orderRepository.getDependencies(order.getOrderId()).get(0);

            Order dependentOrder = orderRepository.getOrderById(dependentOrderId);
            updateSoldOrders(List.of(dependentOrder), order);


//            List<Order> ordersBelowPrice = orderRepository.getOrdersBelowPrice(ConfigLoader.get("bot.name"), order.getPrice());

//            if(ordersBelowPrice.isEmpty())
//                throw new RuntimeException("no orders below this price");
//
//            orderRepository.updateOrder(order);
        }


    }

    public void buy(){
        try{

            log.warning(String.format("Buying %s in %s",ConfigLoader.get("bot.amount_to_trade"), ConfigLoader.get("bot.symbol")));
            JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                                                                                        "symbol="+ConfigLoader.get("bot.symbol"),
                                                                                                "side=BUY",
                                                                                                "type=MARKET",
                                                                                                "quoteOrderQty="+ConfigLoader.get("bot.amount_to_trade"),
                                                            String.format("timestamp=%s", System.currentTimeMillis()));
            Order order = OrderParse.parseFrom(response);
            order.setPaidValue(response.get("cummulativeQuoteQty").getAsDouble());
            log.info(String.format("bought for %s", order.getPaidValue()));
            orderRepository.insertOrder(order);

            Order limitOrder = createLimitOrder(order.getPrice() *  Double.parseDouble(ConfigLoader.get("bot.strategy.targetPricePercentage")), order.getQuantity());
            orderRepository.insertOrder(limitOrder);
            orderRepository.createDependency(order.getOrderId(), limitOrder.getOrderId());

        }catch (Exception e){
            log.severe("Error when buying");
            log.severe(e.getMessage());
        }
    }
    public Order createLimitOrder(double targetPrice, BigDecimal quantity){
        log.info(String.format("creating LIMIT order to: %s",  targetPrice));
        try{


        JsonObject response =  HttpHelper.doSignedPost(String.format("%s/order",ConfigLoader.get("binance.url")),
                                                                                                "symbol="+ConfigLoader.get("bot.symbol"),
                                                                                                "side=SELL",
                                                                                                "type=LIMIT",
                                                                                                "timeInForce=GTC",
                                                                                                "quantity="+ String.format("%.8f",quantity).replace(",","."),
                                                                                                "price="+String.format("%.2f", targetPrice).replace(",","."),
                                                                                                String.format("timestamp=%s", System.currentTimeMillis()));

        return OrderParse.parseFromLimit(response);
        }catch (Exception e){
            log.severe("ERROR BUYING "+e.getMessage());
            return null;
        }
    }

    public void sell(double currentPrice){
        try{
            List<Order> ordersBelowPrice = orderRepository.getOrdersBelowPrice(ConfigLoader.get("bot.name"), currentPrice);
            double quantityToSell = 0.0;
            for(Order order : ordersBelowPrice){
                quantityToSell += order.getQuantity().doubleValue();
            }

            log.warning(String.format("Selling %s in %s", ConfigLoader.get("bot.amount_to_trade"), ConfigLoader.get("bot.symbol")));
            JsonObject response = HttpHelper.doSignedPost(String.format("%s/order", ConfigLoader.get("binance.url")),
                    "symbol=" + ConfigLoader.get("bot.symbol"),
                    "side=SELL",
                    "type=MARKET",
                    "quantity="+String.format("%.8f",quantityToSell),
                    String.format("timestamp=%s", System.currentTimeMillis()));
            Order order = OrderParse.parseFrom(response);
            order.setStatus("executed");


            log.info(String.format("sold for %s", order.getPaidValue()));
            updateSoldOrders(ordersBelowPrice, order);
            orderRepository.insertOrder(order);

        }catch (Exception e){
            log.severe("Error when selling");
            log.severe(e.getMessage());
        }

    }

    private void updateSoldOrders(List<Order> orders, Order soldOrder){
        double finalProfit = 0.0;

        for (Order order : orders){
            order.setStatus("executed");

            order.setProfit( (soldOrder.getPrice() * order.getQuantity().doubleValue()) - order.getPaidValue());
            finalProfit += order.getPaidValue();
            //finalCommision += order.getCommission();
            orderRepository.updateOrder(order);
        }
        soldOrder.setProfit((soldOrder.getPaidValue() - finalProfit));
        soldOrder.setExecutedAt(System.currentTimeMillis());
        orderRepository.updateOrder(soldOrder);
    }


    private double calculateDifferencePercentage(double lastPrice, double currentPrice){
        return  100 - ((lastPrice * 100 ) / currentPrice);
    }

    public double getCurrentPrice(){
        JsonObject resp =  HttpHelper.doGet( String.format("%s/ticker/price?symbol=%s",ConfigLoader.get("binance.url"),ConfigLoader.get("bot.symbol")));
        return resp.get("price").getAsDouble();
    }

    public List<Order> getLastPendingOrders(){
        return orderRepository.getPendingOrdersByBot(ConfigLoader.get("bot.name"));
    }

    public double getLastTradePrice(){
        List<Order> orders = orderRepository.getLastBoughtOrdersByBot(ConfigLoader.get("bot.name"));

        return orders.get(0).getPrice();
    }

    public List<Order> getOpenOrders(){
//        log.warning(String.format("Getting all Open Orders of %s", ConfigLoader.get("bot.symbol")));
        JsonElement response = HttpHelper.doSignedGet(String.format("%s/openOrders", ConfigLoader.get("binance.url")),
                "symbol=" + ConfigLoader.get("bot.symbol"),
                String.format("timestamp=%s", System.currentTimeMillis()));

        //List<Order> openOrders = orderRepository.getOpenOrdersByBot(ConfigLoader.get("bot.name"));
//        log.warning(String.format("Quantity of orders opened %s",response.getAsJsonArray().asList().size()));

        return response.getAsJsonArray().asList().stream().map( o -> OrderParse.parseFromOpenOrders(o.getAsJsonObject())).toList();
    }

    public JsonArray getFills(long orderId){
        JsonElement response = HttpHelper.doSignedGet(String.format("%s/myTrades", ConfigLoader.get("binance.url")),
                "symbol=" + ConfigLoader.get("bot.symbol"),
                "orderId=" +orderId,
                String.format("timestamp=%s", System.currentTimeMillis()));

        if(response.getAsJsonArray().isEmpty())
            return null;

        return response.getAsJsonArray();
    }

    private void updatingOrder(Order order){
        //checar se ordem tem status diferente EXPIRED_IN_MATCH
        JsonElement response =  HttpHelper.doSignedGet(String.format("%s/order", ConfigLoader.get("binance.url")),
                "symbol=" + ConfigLoader.get("bot.symbol"),
                        "orderId=" + order.getBinanceOrderId(),
                String.format("timestamp=%s", System.currentTimeMillis()));
        //se sim criar nova ordem com valor semelhante
        String status = response.getAsJsonObject().get("status").getAsString();
        if(status == "FILLED")
            return;

        log.warning(String.format("order %s changed status to %s", order.getBinanceOrderId(), status));

        Order limitOrder = createLimitOrder(order.getPrice(), order.getQuantity());
        order.setBinanceOrderId(limitOrder.getBinanceOrderId());
    }
}
