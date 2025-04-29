package com.jeff.cripto.trading.bot;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Checkpoint;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.context.LeverageContext;
import com.jeff.cripto.trading.rules.*;
import com.jeff.cripto.trading.strategy.BuyStrategy;
import com.jeff.cripto.trading.strategy.MarketStrategy;
import com.jeff.cripto.trading.strategy.SellStrategy;
import com.jeff.cripto.trading.utils.BinanceService;
import com.jeff.cripto.trading.utils.CheckpointEngine;
import com.jeff.cripto.trading.utils.TradingUtils;
import com.jeff.cripto.trading.utils.OrdersService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class LeverageBot implements Bot{

    private final OrderRepository orderRepository;
    private final LeverageContext botContext;
    private final List<BuyRule<LeverageContext>> buyRules;
    private final List<Rule> sellRules;
    private final CheckpointEngine checkpointEngine;
    private final OrdersService ordersService;
    private BigDecimal bnbValue;

    public LeverageBot(){
        this.orderRepository = new OrderRepository();
        this.botContext = new LeverageContext();
        this.checkpointEngine = new CheckpointEngine();
        this.ordersService = new OrdersService(orderRepository);
        this.buyRules = List.of(    new ValueHigher(),
                                    new AboveOpenOrders(orderRepository));

        this.sellRules = List.of( new ValueBelow(botContext));
    }

    @Override
    public void process() {
        botContext.setCurrentPrice(BinanceService.getCurrentPrice());
        bnbValue = BinanceService.getCurrentPrice("BNBUSDC");

        if(checkpointEngine.isNotRunning()){
            checkpointEngine.start(botContext.getCurrentPrice());
            return;
        }
        botContext.setCheckpoint(checkpointEngine.process(botContext.getCurrentPrice()));

        printLog(botContext.getCheckpoint(), TradingUtils.calculateDifferencePercentage(botContext.getCheckpoint().getPrice().doubleValue() , botContext.getCurrentPrice().doubleValue()));

        if(botContext.getCheckpoint().isNotComplete())
            return;

        if(shouldBuy(botContext.getCheckpoint(), botContext.getCurrentPrice())){
            log.warn("BUY");
            buy(new MarketStrategy());
            return;
        }
        if(shouldSell(botContext.getCheckpoint(), botContext.getCurrentPrice())){
            log.warn("SELL");
            Order order = sell(new MarketStrategy());
            if(order != null)
                log.info("Sold for %s".formatted(order.getPaidValue().toPlainString()));
            return;
        }


    }

    public void printLog(Checkpoint checkpoint, double diffPercentage){

        if(checkpoint.getTargetValue() == null)
            return;



        StringBuilder finalLog = new StringBuilder("\n");

        String[] result =new String[9];

        boolean currentPriceAboveCheckpoint = botContext.getCurrentPrice().compareTo(checkpoint.getPrice()) >= 0;
        result[0] =                                                                             "";
        result[1] = checkpoint.isGoingDown() ?                                                  "┌––––––––––––––––––––🚧  %.2f".formatted(checkpoint.getTargetValue()) : "";
        result[2] = (checkpoint.isGoingDown() ? "┊" : " ").concat(currentPriceAboveCheckpoint ? "                 ┏━━ 🪙 %.2f".formatted(botContext.getCurrentPrice()) : " ");
        result[3] = (checkpoint.isGoingDown() ? "┊" : " ").concat(currentPriceAboveCheckpoint ? "            ┏━━━━┛ %.2f".formatted(diffPercentage > 0 ? diffPercentage : 0) : " ");;
        result[4] = "🚩: %.2f ".formatted(checkpoint.getPrice()).concat(checkpoint.isGoingUp() ? "🌲" : "🔻");
        result[5] = (checkpoint.isGoingUp() ? "┊" : " ").concat(!currentPriceAboveCheckpoint ? "            ┗━━━━┓ %.2f".formatted(diffPercentage < 0 ? diffPercentage : 0) : " ");;
        result[6] = (checkpoint.isGoingUp() ? "┊" : " ").concat(!currentPriceAboveCheckpoint ? "                 ┗━━ 🪙 %.2f".formatted(botContext.getCurrentPrice()) : " ");
        result[7] = checkpoint.isGoingUp() ?                                                   "└––––––––––––––––––––🚧  %.2f".formatted(checkpoint.getTargetValue()) : "";
        result[8] = "";

        if(botContext.getCurrentPrice().compareTo(checkpoint.getTargetValue()) < 0 && checkpoint.isGoingUp()){
            result[6] =                            "┊                 ┃";
            result[7] = checkpoint.isGoingUp() ?   "└–––––––––––––––––┃–– 🚧 %.2f".formatted(checkpoint.getTargetValue()) : "                  ┃ ";
            result[8] =                            "                  ┗━━ 🪙 %.2f".formatted(botContext.getCurrentPrice());
        }else if(botContext.getCurrentPrice().compareTo(checkpoint.getTargetValue()) < 0 && checkpoint.isGoingUp()){
            result[0] =                            "                  ┏━━ 🪙 %.2f".formatted(botContext.getCurrentPrice());
            result[1] = checkpoint.isGoingDown() ? "┌–––––––––––––––––┃–– 🚧 %.2f".formatted(checkpoint.getTargetValue()) : "                 ┃ ";
            result[2] =                            "┊                 ┃";
        }

        for (String s : result) {
            finalLog.append(s);
            finalLog.append("\n");
        }

        log.info(finalLog.toString());

    }

    private boolean shouldBuy(Checkpoint checkpoint, BigDecimal currentPrice){
        boolean shouldBuy = buyRules.stream().allMatch(r -> r.shouldBuy(botContext));
        log.info("shouldbuy ? : {}", shouldBuy);
        return shouldBuy;
    }

    private boolean shouldSell(Checkpoint checkpoint, BigDecimal currentPrice){
        boolean shouldSell = sellRules.stream().allMatch(Rule::checkRule);
        log.info("shoulsell ? : {}", shouldSell);
        return currentPrice.compareTo(checkpoint.getTargetValue()) < 0 && checkpoint.getUp();
    }

    @Override
    public Order buy(BuyStrategy strategy) {
        Order order = strategy.buy();
        if(order == null) return null;

        order.setCommission(order.getCommission().multiply(bnbValue));
        order.setCommissionAsset("BNBUSDC");

        orderRepository.insertOrder(order);
        return order;
    }

    @Override
    public Order sell(SellStrategy strategy) {
        List<Order> validOrders = ordersService.getPendingOrderBelowBaseDiff(botContext.getCurrentPrice());

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

        sold.setCommission(sold.getCommission().multiply(bnbValue));
        sold.setCommissionAsset("BNBUSDC");

        orderRepository.insertOrder(sold);
        return sold;
    }
}
