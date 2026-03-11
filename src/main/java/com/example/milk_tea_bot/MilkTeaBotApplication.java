package com.example.milk_tea_bot;

import com.example.milk_tea_bot.bot.MilkTeaBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class MilkTeaBotApplication {

	@Autowired
	private MilkTeaBot milkTeaBot;

	public static void main(String[] args) {
		SpringApplication.run(MilkTeaBotApplication.class, args);
	}

	@PostConstruct
	public void registerBot() {
		try {

			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

			botsApi.registerBot(milkTeaBot);

			System.out.println("TelegramBotsApi started");
			System.out.println("Registered bot: " + milkTeaBot.getBotUsername());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}