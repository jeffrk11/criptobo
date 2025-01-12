package com.jeff.cripto.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.exceptions.DataBaseException;
import com.jeff.cripto.model.Order;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderParse {

    public static Order parseFrom(JsonObject json){
        Order response = new Order(
                            0,
                            ConfigLoader.get("bot.name"),
                            json.get("symbol").getAsString(),
                            json.get("side").getAsString().toLowerCase(),
                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("price").getAsDouble(),
                            0,
                            json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                            json.get("transactTime").getAsLong(),
                            System.currentTimeMillis(),

                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commission").getAsDouble(),
                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commissionAsset").getAsString(),
                            json.get("orderId").getAsLong(),
                            json.get("cummulativeQuoteQty").getAsDouble(),
                            BigDecimal.valueOf(json.get("executedQty").getAsDouble()),
                            json.get("status").getAsString());

        parseFills(response, json.getAsJsonArray("fills"));

        return response;
    }

    public static Order parseFromOpenOrders(JsonObject json){

        return new Order(
                0,
                ConfigLoader.get("bot.name"),
                json.get("symbol").getAsString(),
                json.get("side").getAsString().toLowerCase(),
                json.get("price").getAsDouble(),
                0,
                json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                json.get("time").getAsLong(),
                0L,
                0,
                "",
                json.get("orderId").getAsLong(),
                json.get("cummulativeQuoteQty").getAsDouble(),
                BigDecimal.valueOf(json.get("executedQty").getAsDouble()),
                json.get("status").getAsString()
        );

    }
    public static Order parseFromLimit(JsonObject json){

        boolean emptyFills = json.get("fills").getAsJsonArray().isEmpty();

        return new Order(
                0,
                ConfigLoader.get("bot.name"),
                json.get("symbol").getAsString(),
                json.get("side").getAsString().toLowerCase(),
                emptyFills ? json.get("price").getAsDouble() : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("price").getAsDouble(),
                0,
                json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                json.get("transactTime").getAsLong(),
                0L,
                emptyFills ? 0 : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commission").getAsDouble(),
                emptyFills ? "" : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commissionAsset").getAsString(),
                json.get("orderId").getAsLong(),
                0d,
                BigDecimal.valueOf(json.get("origQty").getAsDouble()),
                json.get("status").getAsString()
        );

    }

    public static Order parseFrom(ResultSet dbResult){
        try {
            return new Order(
                    dbResult.getInt("order_id"),
                    dbResult.getString("bot_name"),
                    dbResult.getString("symbol"),
                    dbResult.getString("order_type"),
                    dbResult.getDouble("price"),
                    dbResult.getDouble("profit"),
                    dbResult.getString("status"),
                    dbResult.getLong("created_at"),
                    dbResult.getLong("executed_at"),
                    dbResult.getDouble("commission"),
                    dbResult.getString("commission_asset"),
                    dbResult.getLong("binance_order_id"),
                    dbResult.getDouble("paid_value"),
                    BigDecimal.valueOf(dbResult.getDouble("quantity")),
                    dbResult.getString("binance_status"));
        } catch (SQLException e) {
            throw new DataBaseException(e, "Error when converting to Order");
        }
    }

    public static void parseTo(Order order, PreparedStatement preparedStatement) throws SQLException {

        preparedStatement.setInt(1, order.getOrderId());
        preparedStatement.setString(2, order.getBotName());
        preparedStatement.setString(3, order.getSymbol());
        preparedStatement.setString(4, order.getOrderType());
        preparedStatement.setDouble(5, order.getPrice());
        preparedStatement.setDouble(6, order.getProfit());
        preparedStatement.setString(7, order.getStatus());
        preparedStatement.setDouble(8, order.getCommission());
        preparedStatement.setString(9, order.getCommissionAsset());
        preparedStatement.setLong(10, order.getBinanceOrderId());
        preparedStatement.setLong(11, order.getCreatedAt());
        preparedStatement.setLong(12, order.getExecutedAt());
        preparedStatement.setDouble(13,order.getPaidValue());
        preparedStatement.setDouble(14, order.getQuantity().doubleValue());
        preparedStatement.setString(15, order.getBinanceStatus());
    }

    public static void parseFills(Order toPopulate, JsonArray fills){
        toPopulate.setQuantity(BigDecimal.ZERO);
        toPopulate.setCommission(0d);
        for(JsonElement fill : fills.asList()){
            toPopulate.setQuantity(toPopulate.getQuantity().add(BigDecimal.valueOf( fill.getAsJsonObject().get("qty").getAsDouble())));
            toPopulate.setCommission(toPopulate.getCommission() + fill.getAsJsonObject().get("commission").getAsDouble());
            if(fill.getAsJsonObject().has("quoteQty"))
                toPopulate.setPaidValue(toPopulate.getPaidValue() + fill.getAsJsonObject().get("quoteQty").getAsDouble());
        }
    }
}
