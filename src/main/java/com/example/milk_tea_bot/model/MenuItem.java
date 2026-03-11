package com.example.milk_tea_bot.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuItem {
    private String category;
    private String itemId;
    private String name;
    private String description;
    private int priceM;
    private int priceL;
    private boolean available;
}
