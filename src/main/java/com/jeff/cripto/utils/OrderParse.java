package com.jeff.cripto.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Order;

import java.math.BigDecimal;

public class OrderParse {

    public static Order parseFrom(JsonObject json){
        Order response = new Order(
                            0,
                            ConfigLoader.get("bot.name"),
                            json.get("symbol").getAsString(),
                            json.get("side").getAsString().toLowerCase(),
                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("price").getAsBigDecimal(),
                            BigDecimal.ZERO,
                            json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                            json.get("transactTime").getAsLong(),
                            System.currentTimeMillis(),

                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commission").getAsBigDecimal(),
                            json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commissionAsset").getAsString(),
                            json.get("orderId").getAsLong(),
                            json.get("cummulativeQuoteQty").getAsBigDecimal(),
                            json.get("executedQty").getAsBigDecimal(),
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
                json.get("price").getAsBigDecimal(),
                BigDecimal.ZERO,
                json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                json.get("time").getAsLong(),
                0L,
                BigDecimal.ZERO,
                "",
                json.get("orderId").getAsLong(),
                json.get("cummulativeQuoteQty").getAsBigDecimal(),
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
                emptyFills ? json.get("price").getAsBigDecimal() : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("price").getAsBigDecimal(),
                BigDecimal.ZERO,
                json.get("status").getAsString() == "FILLED " ? "executed" : "pending",
                json.get("transactTime").getAsLong(),
                0L,
                emptyFills ? BigDecimal.ZERO : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commission").getAsBigDecimal(),
                emptyFills ? "" : json.getAsJsonArray("fills").get(0).getAsJsonObject().get("commissionAsset").getAsString(),
                json.get("orderId").getAsLong(),
                BigDecimal.ZERO,
                json.get("origQty").getAsBigDecimal(),
                json.get("status").getAsString()
        );

    }


    public static void parseFills(Order toPopulate, JsonArray fills){
        toPopulate.setQuantity(BigDecimal.ZERO);
        toPopulate.setCommission(BigDecimal.ZERO);
        for(JsonElement fill : fills.asList()){
            toPopulate.setQuantity(toPopulate.getQuantity().add( fill.getAsJsonObject().get("qty").getAsBigDecimal()));
            toPopulate.setCommission(toPopulate.getCommission().add(fill.getAsJsonObject().get("commission").getAsBigDecimal()));
            if(fill.getAsJsonObject().has("quoteQty"))
                toPopulate.setPaidValue(toPopulate.getPaidValue().add(fill.getAsJsonObject().get("quoteQty").getAsBigDecimal()));
        }
    }
}
