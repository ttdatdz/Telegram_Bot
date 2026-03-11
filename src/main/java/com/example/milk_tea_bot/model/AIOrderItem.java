package com.example.milk_tea_bot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIOrderItem  {
    private String item;
    private String size;
    private int quantity;
}