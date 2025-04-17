package com.jeff.cripto.trading.utils;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.Checkpoint;
import com.jeff.cripto.model.Direction;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class CheckpointEngine {
    private final Checkpoint checkpoint;

    private boolean running;

    public CheckpointEngine(){
        checkpoint = new Checkpoint(Direction.NONE);
    }

    public boolean isNotRunning(){
        return !this.running;
    }

    public void start(BigDecimal currentPrice) {
        log.info("starting checkpoint engine ⚙️");
        this.running = true;
        checkpoint.resetCheckpoint(currentPrice);

    }

    public Checkpoint process(BigDecimal currentPrice){
        if(checkpoint.isComplete()){
            log.info("checkpoint ja completo, resete");
            return checkpoint;
        }


        double differenceLastPoint = TradingUtils.calculateDifferencePercentage(checkpoint.getPrice().doubleValue() , currentPrice.doubleValue());

        if(checkpoint.getDirection() == Direction.NONE){
            setDirection(differenceLastPoint, currentPrice);
            return checkpoint;
        }

        if(shouldUpdateCheckpoint(checkpoint, differenceLastPoint))
            return updateCheckPoint(checkpoint, currentPrice, differenceLastPoint);

        boolean targetAchieved = checkTarget(checkpoint, currentPrice);
        if(!targetAchieved){
            //log.info("checkpoint ainda n chegou {} of +-{}",differenceLastPoint, ConfigLoader.getDouble("bot.strategy.baseDifference"));
            return checkpoint;
        }

        log.info("finish checkpoint engine 🚩");
        checkpoint.setComplete(true);
        running = false;
        return checkpoint;
    }

    private Checkpoint updateCheckPoint(Checkpoint checkpoint, BigDecimal currentPrice, double difference){
        checkpoint.setPrice(currentPrice);
        checkpoint.setTargetValue(calculateNextPrice(currentPrice, checkpoint.getDirection()));
        log.warn("updated checkpoint -> {}  |  {}",Math.abs(difference), Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference")));
        return checkpoint;
    }

    private boolean checkTarget(Checkpoint checkpoint,BigDecimal currentPrice){
        return switch (checkpoint.getDirection()){
            case UP   -> currentPrice.compareTo(checkpoint.getTargetValue()) < 0;
            case DOWN -> currentPrice.compareTo(checkpoint.getTargetValue()) >= 0;
            case NONE -> false;
        };
    }

    private void setDirection(double differenceLastPoint, BigDecimal currentPrice){
        boolean differenceHigherThanConfig = BigDecimal.valueOf(differenceLastPoint).abs().compareTo(BigDecimal.valueOf(ConfigLoader.getDouble("bot.strategy.baseDifference"))) > 0;

        if(!differenceHigherThanConfig){
            log.info("Checkpoint not in range {} of +-{}",differenceLastPoint, ConfigLoader.getDouble("bot.strategy.baseDifference"));
            return;
        }

        checkpoint.setUp(differenceLastPoint > 0);
        checkpoint.setDirection(differenceLastPoint > 0 ? Direction.UP : Direction.DOWN);
        checkpoint.setPrice(currentPrice);
        checkpoint.setTargetValue(calculateNextPrice(currentPrice, checkpoint.getDirection()));
        log.info("setting direction : {}", (checkpoint.isGoingUp() ? "up" : "down"));

    }

    private BigDecimal calculateNextPrice(BigDecimal currentPrice, Direction up){

        double targetDifference =  (Double.parseDouble(ConfigLoader.get("bot.strategy.targetPricePercentage")));

        BigDecimal rest = currentPrice.multiply(BigDecimal.valueOf(targetDifference));

        rest = up == Direction.UP ? rest.negate() : rest;

        return currentPrice.add(rest);
    }

    private boolean shouldUpdateCheckpoint(Checkpoint checkpoint, double differenceCheckpoint){
        if(checkpoint.isGoingUp() && differenceCheckpoint < 0)//want to buy and its going up
            return false;

        if(checkpoint.isGoingDown() && differenceCheckpoint > 0)//want to sell and its going down
            return false;
        return Math.abs(differenceCheckpoint) > Double.parseDouble(ConfigLoader.get("bot.strategy.baseDifference"));
    }
}
