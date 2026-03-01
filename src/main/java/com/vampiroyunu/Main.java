package com.vampiroyunu;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) {
        
        // 1. KISIM: RENDER'I KANDIRAN ZIRHLI WEB SUNUCUSU
        try {
            int port = 8080;
            String portStr = System.getenv("PORT");
            // Render bize bir port verirse onu alıyoruz, yoksa 8080 kullanıyoruz
            if (portStr != null && !portStr.trim().isEmpty()) {
                port = Integer.parseInt(portStr.trim());
            }
            
            // "0.0.0.0" yazarak Render'ın dışarıdan bizi rahatça kontrol etmesine izin veriyoruz
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", exchange -> {
                String response = "Karanlik Koy 7/24 Aktif ve Calisiyor!";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });
            server.start();
            System.out.println("Sahte web sunucusu " + port + " portunda basladi. Render artik mutlu!");
        } catch (Throwable t) {
            System.out.println("Sunucu baslatilirken onemsiz bir hata oldu, bot calismaya devam edecek: " + t.getMessage());
        }

        // 2. KISIM: ASIL VAMPİR BOTUMUZU BAŞLATIYORUZ
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new VampirBot());
            System.out.println("Yakamoz'un Vampir Botu başarıyla çalıştırıldı! Kanlı gece başlıyor...");
        } catch (Throwable t) {
            System.out.println("Bot baslatilirken kritik hata: " + t.getMessage());
        }
    }
}
