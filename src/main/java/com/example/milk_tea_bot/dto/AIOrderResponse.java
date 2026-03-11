package com.example.milk_tea_bot.dto;

import com.example.milk_tea_bot.model.AIOrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AIOrderResponse {

    private List<AIOrderItem> orders;
    private String phone;   // Thêm cái này
    private String address;
    public List<AIOrderItem> getOrders() {
        return orders;
    }

    public void setOrders(List<AIOrderItem> orders) {
        this.orders = orders;
    }
}
