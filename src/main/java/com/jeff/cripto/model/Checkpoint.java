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

    public void updateCheckpoint(BigDecimal price, BigDecimal targetValue, boolean up){
        this.price = price;
        this.targetValue = targetValue;
        this.up = up;
        this.streak++;
    }

    public boolean isGoingUp(){
        return up;
    }
    public boolean isGoingDown(){
        return !up;
    }
}
