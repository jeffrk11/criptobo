package com.jeff.cripto.trading.rules;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.utils.BinanceService;
import com.jeff.cripto.trading.utils.TradingUtils;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
public class ValueOutOfBaseDiff implements Rule{

    private OrderRepository orderRepository;

    @Override
    public boolean checkRule() {

        BigDecimal currentPrice = BinanceService.getCurrentPrice();

        Order order = orderRepository.getLastPendingOrder();

        if(order == null)
            return true;

        if(currentPrice.compareTo(order.getPrice()) > 0)
            return false;

        double diff = TradingUtils.calculateDifferencePercentage(order.getPrice().doubleValue(), currentPrice.doubleValue());
        return BigDecimal.valueOf(diff).abs().compareTo(BigDecimal.valueOf(ConfigLoader.getDouble("bot.strategy.notBuyZone"))) > 0;
    }

}
