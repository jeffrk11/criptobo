package com.jeff.cripto.trading.bot;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Checkpoint;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.strategy.BuyStrategy;
import com.jeff.cripto.trading.strategy.MarketStrategy;
import com.jeff.cripto.trading.strategy.SellStrategy;
import com.jeff.cripto.trading.utils.BinanceService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LevaregeBot implements Bot{
    static Logger log = Logger.getLogger(LevaregeBot.class.getName());
    private final OrderRepository orderRepository;
    private List<Checkpoint> checkpoints;
    private Checkpoint checkpoint;
    private BigDecimal currentPrice;

    public LevaregeBot(){
        this.orderRepository = new OrderRepository();
    }

    @Override
    public void process() {
        currentPrice = BinanceService.getCurrentPrice();

        if(checkpoint == null){
            log.info("Setting checkpoint");
            checkpoint = new Checkpoint(currentPrice);
            return;
        }

        double differenceCheckpoint = calculateDifferencePercentage(checkpoint.getPrice().doubleValue() ,currentPrice.doubleValue());

        printLog(checkpoint, currentPrice, differenceCheckpoint);

        //is up or down, this set the direction
        if(checkpoint.getUp() == null && BigDecimal.valueOf(differenceCheckpoint).abs().compareTo(BigDecimal.valueOf(ConfigLoader.getDouble("bot.strategy.baseDifference"))) > 0){
            checkpoint.setUp(differenceCheckpoint > 0);
            checkpoint.setPrice(currentPrice);
            checkpoint.setTargetValue(calculateNextPrice(currentPrice, checkpoint.getUp()));
            return;
        }else if(checkpoint.getUp() == null){
            log.info("Checkpoint not in range %s of +-%s".formatted(differenceCheckpoint, ConfigLoader.getDouble("bot.strategy.baseDifference")));
        }

        //still not knowing
        if(checkpoint.getUp() == null) return;

        if(shouldBuy(checkpoint, currentPrice)){
            Order lastOrder = orderRepository.getLastBoughtOrder();
            checkpoint.setUp(null);
            if(!orderRepository.getPendingOrders().isEmpty() && lastOrder != null && currentPrice.compareTo(lastOrder.getPrice()) > 0)
                return;

            //n compra orders a cima do ultimo valor comprado
            log.warning("BUY");
            buy(new MarketStrategy());
            return;
        }
        if(shouldSell(checkpoint, currentPrice)){
            log.warning("SELL");
            Order order = sell(new MarketStrategy());
            if(order != null)
                log.info("Sold for %s".formatted(order.getPaidValue().toPlainString()));

            checkpoint.setUp(null);
            return;
        }
        if(shouldUpdateCheckpoint(checkpoint, differenceCheckpoint)){
            checkpoint.setPrice(currentPrice);
            checkpoint.setTargetValue(calculateNextPrice(currentPrice, checkpoint.getUp()));
            log.warning("updated checkpoint -> %s --- %s".formatted(Math.abs(differenceCheckpoint), Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference"))));
            //log.info("checkpoint value %s target value %s".formatted(checkpoint.getPrice(), checkpoint.getTargetValue()));
        }

    }

    public void printLog(Checkpoint checkpoint, BigDecimal currentPrice, double diffPercentage){

        if(checkpoint.getUp() == null)
            return;


        StringBuilder finalLog = new StringBuilder("\n");

        String[] result =new String[9];

        boolean currentPriceAboveCheckpoint = currentPrice.compareTo(checkpoint.getPrice()) >= 0;
        result[0] =                                                                             "";
        result[1] = checkpoint.isGoingDown() ?                                                  "┌––––––––––––––––––––🚧  %.2f".formatted(checkpoint.getTargetValue()) : "";
        result[2] = (checkpoint.isGoingDown() ? "┊" : " ").concat(currentPriceAboveCheckpoint ? "                 ┏━━ 🪙 %.2f".formatted(currentPrice) : " ");
        result[3] = (checkpoint.isGoingDown() ? "┊" : " ").concat(currentPriceAboveCheckpoint ? "            ┏━━━━┛ %.2f".formatted(diffPercentage > 0 ? diffPercentage : "" ) : " ");;
        result[4] = "🚩: %.2f ".formatted(checkpoint.getPrice()).concat(checkpoint.isGoingUp() ? "🌲" : "🔻");
        result[5] = (checkpoint.isGoingUp() ? "┊" : " ").concat(!currentPriceAboveCheckpoint ? "            ┗━━━━┓ %.2f".formatted(diffPercentage < 0 ? diffPercentage : "") : " ");;
        result[6] = (checkpoint.isGoingUp() ? "┊" : " ").concat(!currentPriceAboveCheckpoint ? "                 ┗━━ 🪙 %.2f".formatted(currentPrice) : " ");
        result[7] = checkpoint.isGoingUp() ?                                                   "└––––––––––––––––––––🚧  %.2f".formatted(checkpoint.getTargetValue()) : "";
        result[8] = "";

        if(currentPrice.compareTo(checkpoint.getTargetValue()) < 0 && checkpoint.isGoingUp()){
            result[6] =                            "┊                 ┃";
            result[7] = checkpoint.isGoingUp() ?   "└–––––––––––––––––┃–– 🚧 %.2f".formatted(checkpoint.getTargetValue()) : "                  ┃ ";
            result[8] =                            "                  ┗━━ 🪙 %.2f".formatted(currentPrice);
        }else if(currentPrice.compareTo(checkpoint.getTargetValue()) > 0 && checkpoint.isGoingDown()){
            result[0] =                            "                  ┏━━ 🪙 %.2f".formatted(currentPrice);
            result[1] = checkpoint.isGoingDown() ? "┌–––––––––––––––––┃–– 🚧 %.2f".formatted(checkpoint.getTargetValue()) : "                 ┃ ";
            result[2] =                            "┊                 ┃";
        }

        for (int i = 0; i < result.length; i++) {
            finalLog.append(result[i]);
            finalLog.append("\n");
        }

        log.info(finalLog.toString());
    }


    private boolean shouldBuy(Checkpoint checkpoint, BigDecimal currentPrice){
        return currentPrice.compareTo(checkpoint.getTargetValue()) >= 0 && !checkpoint.getUp();
    }
    private boolean shouldSell(Checkpoint checkpoint, BigDecimal currentPrice){
        return currentPrice.compareTo(checkpoint.getTargetValue()) < 0 && checkpoint.getUp();
    }

    private boolean shouldUpdateCheckpoint(Checkpoint checkpoint, double differenceCheckpoint){

        if(checkpoint.isGoingUp() && differenceCheckpoint < 0)//want to buy and its going up
            return false;

        if(checkpoint.isGoingDown() && differenceCheckpoint > 0)//want to sell and its going down
            return false;

        return Math.abs(differenceCheckpoint) > Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference"));
    }


    private BigDecimal calculateNextPrice(BigDecimal currentPrice, Boolean up){

        double targetDifference =  (Double.parseDouble(ConfigLoader.get("bot.strategy.targetPricePercentage")));

        BigDecimal rest = currentPrice.multiply(BigDecimal.valueOf(targetDifference));

        rest = up ? rest.negate() : rest;

        return currentPrice.add(rest);
    }

    private BigDecimal getAllBoughtQuantity(){

        return null;
    }

    @Override
    public Order buy(BuyStrategy strategy) {
        Order order = strategy.buy();
        if(order == null) return null;
        orderRepository.insertOrder(order);
        return order;
    }

    @Override
    public Order sell(SellStrategy strategy) {
        List<Order> orders = orderRepository.getPendingOrders();
        List<Order> validOrders = new ArrayList<>();

        for(Order order : orders){
            double diff = calculateDifferencePercentage( order.getPrice().doubleValue(), currentPrice.doubleValue());
            if(diff >= ConfigLoader.getDouble("bot.strategy.baseDifference") * ConfigLoader.getDouble("bot.strategy.targetMultiply")){
                validOrders.add(order);
            }
        }

        if(validOrders.isEmpty()){
            log.info("There is no orders to sell in range");
            return null;
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (Order order : validOrders) {
            totalQuantity = totalQuantity.add(order.getQuantity());
        }

        ((MarketStrategy) strategy).setQuantity(totalQuantity);

        Order sold =  strategy.sell();
        if(sold == null) return null;

        for(Order order : validOrders){
            order.setExecutedAt(System.currentTimeMillis());
            order.setProfit(order.getQuantity().multiply(sold.getPrice()).subtract(order.getPaidValue()));
            order.setStatus("executed");
            sold.setProfit(sold.getProfit().add(order.getProfit()));
            orderRepository.updateOrder(order);
            orderRepository.createDependency(order.getOrderId(), sold.getOrderId());
        }
        sold.setStatus("executed");
        orderRepository.insertOrder(sold);
        return sold;
    }
}
