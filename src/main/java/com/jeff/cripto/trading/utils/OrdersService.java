package com.jeff.cripto.trading.utils;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrdersService {

    private final OrderRepository orderRepository;

    public OrdersService(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }

    public List<Order> getPendingOrderBelowBaseDiff(BigDecimal currentPrice){
        List<Order> orders = orderRepository.getPendingOrders();
        List<Order> validOrders = new ArrayList<>();

        for(Order order : orders){
            double diff = TradingUtils.calculateDifferencePercentage( order.getPrice().doubleValue(), currentPrice.doubleValue());
            if(diff >= ConfigLoader.getDouble("bot.strategy.sellDifference")){
                validOrders.add(order);
            }
        }

        return validOrders;
    }

}
