package com.jeff.cripto.trading.utils;

public class TradingUtils {
    public static double calculateDifferencePercentage(double lastPrice, double currentPrice){
        return  ((currentPrice - lastPrice) / lastPrice) * 100;
    }
}
