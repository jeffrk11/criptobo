package com.jeff.cripto.trading.rules;

import com.jeff.cripto.database.OrderRepository;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.trading.context.LeverageContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class AboveOpenOrders implements BuyRule<LeverageContext> {

    private OrderRepository orderRepository;

    @Override
    public boolean shouldBuy(LeverageContext context) {
        Order lastPendingOrder = orderRepository.getLastPendingOrder();

        if(lastPendingOrder == null)
            return true;

        boolean isAboveLastOrder = context.getCurrentPrice().compareTo(lastPendingOrder.getPrice()) > 0;

        if(isAboveLastOrder)
            log.info("nao vai comprar, pq o preco e maior comparado com a ultima ordem : agora {} ultima {}",context.getCurrentPrice().toPlainString(), lastPendingOrder.getPrice().toPlainString());

        return !isAboveLastOrder;
    }
}
