package com.example.milk_tea_bot.service;

import com.example.milk_tea_bot.model.MenuItem;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public int calculate(MenuItem item, String size, int qty) {

        int price;

        if (size.equalsIgnoreCase("M"))
            price = item.getPriceM();
        else
            price = item.getPriceL();

        return price * qty;
    }

}
