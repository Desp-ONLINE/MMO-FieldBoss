package com.binggre.mmofieldboss.objects;

import lombok.Getter;

@Getter
public class ChanceReward {

    private String type;
    private String id;
    private int amount;

    public int getAmount() {
        if (amount == 0) {
            amount = 1;
        }
        return amount;
    }
}