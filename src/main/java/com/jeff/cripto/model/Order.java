package com.jeff.cripto.model;

import com.jeff.cripto.utils.annotations.Column;
import com.jeff.cripto.utils.annotations.PrimaryKey;
import com.jeff.cripto.utils.annotations.Table;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table("orders")
public class Order {

    @PrimaryKey
    @Column("order_id")
    private long orderId;
    @Column("bot_name")
    private String botName;
    @Column("symbol")
    private String symbol;
    @Column("order_type")
    private String orderType;
    @Column("price")
    private BigDecimal price;
    @Column("profit")
    private BigDecimal profit;
    @Column("status")
    private String status;
    @Column("created_at")
    private Long createdAt;
    @Column("executed_at")
    private Long executedAt;
    @Column("commission")
    private BigDecimal commission;
    @Column("commission_asset")
    private String commissionAsset;
    @Column("binance_order_id")
    private long binanceOrderId;
    @Column("paid_value")
    private BigDecimal paidValue;
    @Column("quantity")
    private BigDecimal quantity;
    @Column("binance_status")
    private String binanceStatus;

}
