package com.example.milk_tea_bot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
//    api này chỉ dùng để giữ connect đến render. GIúp render k bị chết
    @GetMapping("/ping")
    public String ping(){
        return "OK";
    }

}