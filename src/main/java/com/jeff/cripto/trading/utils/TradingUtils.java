package com.jeff.cripto.trading.utils;

public class TradingUtils {
    public static double calculateDifferencePercentage(double lastPrice, double currentPrice){
        return  100 - ((lastPrice * 100 ) / currentPrice);
    }
}
