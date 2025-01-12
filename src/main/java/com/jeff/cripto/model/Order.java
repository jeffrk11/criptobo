package com.jeff.cripto.model;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Order {
    private int orderId;
    private String botName;
    private String symbol;
    private String orderType;
    private double price;
    private double profit;
    private String status;
    private Long createdAt;
    private Long executedAt;
    private double commission;
    private String commissionAsset;
    private long binanceOrderId;
    private double paidValue;
    private BigDecimal quantity;
    private String binanceStatus;

}
