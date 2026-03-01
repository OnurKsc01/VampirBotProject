package com.vampiroyunu;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Telegram API'sine bağlanıyoruz
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            
            // Kendi yazdığımız botu sisteme kaydediyoruz
            botsApi.registerBot(new VampirBot());
            
            System.out.println("Yakamoz'un Vampir Botu başarıyla çalıştırıldı! Kanlı gece başlıyor...");
            
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}