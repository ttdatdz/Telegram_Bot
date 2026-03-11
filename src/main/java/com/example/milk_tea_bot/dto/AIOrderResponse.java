package com.example.milk_tea_bot.dto;

import com.example.milk_tea_bot.model.AIOrderItem;

import java.util.List;

public class AIOrderResponse {

    private List<AIOrderItem> orders;

    public List<AIOrderItem> getOrders() {
        return orders;
    }

    public void setOrders(List<AIOrderItem> orders) {
        this.orders = orders;
    }
}
