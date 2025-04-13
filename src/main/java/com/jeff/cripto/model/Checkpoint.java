package com.jeff.cripto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@NoArgsConstructor
@Getter
@Setter
public class Checkpoint {
    private BigDecimal price;
    private BigDecimal targetValue;
    private Boolean up;
    private Direction direction;
    private boolean complete;
    private int streak;

    public Checkpoint(BigDecimal price) {
        this.price = price;
        streak = 1;
    }

    public Checkpoint(BigDecimal price, BigDecimal targetValue) {
        this.price = price;
        this.targetValue = targetValue;
        this.streak = 1;
    }

    public Checkpoint(Direction direction) {
        this.direction = direction;
    }

    public void updateCheckpoint(BigDecimal price, BigDecimal targetValue, boolean up){
        this.price = price;
        this.targetValue = targetValue;
        this.up = up;
        this.streak++;
    }

    public boolean isNotComplete(){
        return !complete;
    }

    public boolean isGoingUp(){
        return direction == Direction.UP;
    }
    public boolean isGoingDown(){
        return direction == Direction.DOWN;
    }

    public void resetCheckpoint(BigDecimal price){
        this.setPrice(price);
        this.setUp(null);
        this.setDirection(Direction.NONE);
        this.setComplete(false);
    }
}
