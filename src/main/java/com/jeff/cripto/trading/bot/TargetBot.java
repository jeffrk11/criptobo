package com.jeff.cripto.trading.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.strategy.BuyStrategy;
import com.jeff.cripto.trading.strategy.LimitStrategy;
import com.jeff.cripto.trading.strategy.MarketStrategy;
import com.jeff.cripto.trading.strategy.SellStrategy;
import com.jeff.cripto.trading.utils.BinanceService;
import com.jeff.cripto.utils.HttpHelper;
import com.jeff.cripto.utils.OrderParse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class TargetBot implements Bot{
    static Logger log = Logger.getLogger(TargetBot.class.getName());
    private final OrderRepository orderRepository;


    public TargetBot(){
        this.orderRepository = new OrderRepository();
    }

    public void process(){
        //check last price
        BigDecimal currentPrice = BinanceService.getCurrentPrice();
        //log.info(String.format("Current price: %s - %s", currentPrice, ConfigLoader.get("bot.symbol")));


        //no more orders left
        if(getLastPendingOrders().isEmpty()){
            log.info("no more order -> buy");
            createOperation();
            return;
        }
        BigDecimal lastTradePrice = getLastTradePrice();
        double differencePercentage = calculateDifferencePercentage(lastTradePrice.doubleValue(), currentPrice.doubleValue());


        double targetDifference = calculateTargetDifference();

        List<Order> openInternalOrders = orderRepository.getOpenOrders();

        long timeSinceLastBuy = getTimeSinceLastBuy(orderRepository.getLastBoughtOrder());

        //log.info(String.format("Difference between last trade price %s, cut number : %s", differencePercentage, targetDifference));

        printLog(currentPrice, openInternalOrders,lastTradePrice, differencePercentage, targetDifference, (int) (timeSinceLastBuy / 60000));

        checkOpenOrder(openInternalOrders);

        if(differencePercentage <= targetDifference){
            log.info("target percentage  -> buy");
            createOperation();
            return;
        }

//        if(timeSinceLastBuy >= Integer.parseInt(ConfigLoader.get("bot.strategy.stagnantMinutes")) * 60000L){
//            log.info("time passed -> buy");
//            creteOperation();
//        }


    }

    private double calculateTargetDifference(){
        double baseDifference =  Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference"));
        List<Order> lastOperationsList = orderRepository.getAllOrders( 50);
        int openOrdersStreak = 1;
        for(Order order : lastOperationsList){
            if(order.getOrderType().equals("sell"))
                continue;
            if(order.getStatus().equals("executed"))
                break;
            openOrdersStreak++;
        }
        return baseDifference * openOrdersStreak * Double.parseDouble(ConfigLoader.get("bot.strategy.targetMultiply"));
    }

    private void createOperation(){
        Order boughtOrder = buy(new MarketStrategy());
        Order limitOrder =  sell(new LimitStrategy(boughtOrder.getPrice().multiply(BigDecimal.valueOf(Double.parseDouble(ConfigLoader.get("bot.strategy.targetPricePercentage")))),
                boughtOrder.getQuantity()));
        orderRepository.insertOrder(boughtOrder);
        orderRepository.insertOrder(limitOrder);
        orderRepository.createDependency(boughtOrder.getOrderId(), limitOrder.getOrderId());
    }

    private void printLog(BigDecimal currentPrice, List<Order> openOrders, BigDecimal lastTradePrice, double differencePercentage, double targetDifference, int minutes) {
        StringBuilder openedOrdersText = new StringBuilder();
        for(int i = 0; i < openOrders.size(); i++){
            openedOrdersText.append("🔴 $%s 🔹 ".formatted(openOrders.get(i).getPrice()));
            if( i > 0 && i % 7 == 0) openedOrdersText.append("\n\t");
        }

        BigDecimal targetPrice = (lastTradePrice.multiply (BigDecimal.valueOf(targetDifference/100))).add(lastTradePrice);

        String lastPricePosition = differencePercentage >= 0 ? "🌲" : "🔻";

        List<Order> lastOperationsList = orderRepository.getAllOrders( 20);

        StringBuilder lastOperationsText = new StringBuilder();
        for(int i = 0; i < lastOperationsList.size(); i +=2){
            LocalDateTime timeBuy = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOperationsList.get(i + 1).getExecutedAt()), ZoneId.of("Europe/Lisbon"));
            LocalDateTime timeSell = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOperationsList.get(i).getExecutedAt()), ZoneId.of("Europe/Lisbon"));

            boolean validDate = timeSell.getYear() != 1970;

            lastOperationsText.append("🟢 %s $%.2f ⇀ %02d/%02d-%02d:%02d  ❱ 🔴 %s $%.2f ⇀ %02d/%02d-%02d:%02d 💰 $%s \n\t"
                    .formatted(lastOperationsList.get(i+1).getStatus().equals("pending") ? "🔔" : "✔️", lastOperationsList.get(i+1).getPrice(),timeBuy.getDayOfMonth(),timeBuy.getMonthValue(), timeBuy.getHour(), timeBuy.getMinute(),
                               lastOperationsList.get(i).getStatus().equals("pending") ? "🔔" : "✔️", lastOperationsList.get(i).getPrice(),validDate ? timeSell.getDayOfMonth() : 0,validDate ? timeSell.getMonthValue() : 0, validDate ? timeSell.getHour() : 0, validDate ? timeSell.getMinute() : 0,
                               lastOperationsList.get(i).getProfit().toPlainString()));
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

    private long getTimeSinceLastBuy(Order order){
        if( order == null) return 0L;
        return  System.currentTimeMillis() - order.getCreatedAt();
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

            if(fills == null){
                //maybe expired
                updateOrder(order);
                orderRepository.updateOrder(order);
                return;
            }

            OrderParse.parseFills(order, fills);

            log.info(String.format("Order %s checked and filled updating database", order.getOrderId()));
            order.setStatus("executed");
//            order.setCommission(fills.get("commission").getAsDouble());
//            order.setCommissionAsset(fills.get("commissionAsset").getAsString());
            order.setBinanceStatus("FILLED");
//            order.setPaidValue(fills.get("quoteQty").getAsDouble());
            order.setPrice(fills.get(0).getAsJsonObject().get("price").getAsBigDecimal());

            long dependentOrderId = orderRepository.getDependencies(order.getOrderId()).get(0);

            Order dependentOrder = orderRepository.getOrderById(dependentOrderId);
            updateSoldOrders(List.of(dependentOrder), order);

        }


    }

    @Override
    public Order buy(BuyStrategy buyStrategy){
        return buyStrategy.buy();
    }

    @Override
    public Order sell(SellStrategy sellStrategy) {
        return sellStrategy.sell();
    }

    private void updateSoldOrders(List<Order> orders, Order soldOrder){
        BigDecimal finalProfit = BigDecimal.ZERO;

        for (Order order : orders){
            order.setStatus("executed");

            order.setProfit( (soldOrder.getPrice().multiply(order.getQuantity())).subtract(order.getPaidValue()));
            finalProfit = finalProfit.add(order.getPaidValue());
            //finalCommision += order.getCommission();
            orderRepository.updateOrder(order);
        }
        soldOrder.setProfit((soldOrder.getPaidValue().subtract(finalProfit)));
        soldOrder.setExecutedAt(System.currentTimeMillis());
        orderRepository.updateOrder(soldOrder);
    }


    public List<Order> getLastPendingOrders(){
        return orderRepository.getPendingOrders();
    }

    public BigDecimal getLastTradePrice(){
        return orderRepository.getLastOperationFilledOrder().getPrice();
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

    private void updateOrder(Order order){
        //checar se ordem tem status diferente EXPIRED_IN_MATCH
        JsonElement response =  HttpHelper.doSignedGet(String.format("%s/order", ConfigLoader.get("binance.url")),
                "symbol=" + ConfigLoader.get("bot.symbol"),
                        "orderId=" + order.getBinanceOrderId(),
                String.format("timestamp=%s", System.currentTimeMillis()));
        //se sim criar nova ordem com valor semelhante
        String status = response.getAsJsonObject().get("status").getAsString();
        if(status.equals("FILLED"))
            return;

        log.warning(String.format("order %s changed status to %s", order.getBinanceOrderId(), status));

        Order limitOrder = sell(new LimitStrategy(order.getPrice(), order.getQuantity()));
        order.setBinanceOrderId(limitOrder.getBinanceOrderId());
    }
}
