package com.vampiroyunu;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) {
        
        // 1. KISIM: RENDER'I KANDIRAN SAHTE WEB SUNUCUSU
        try {
            // Render'ın bize verdiği gizli portu buluyoruz (Yoksa 8080 kullanıyoruz)
            String portStr = System.getenv("PORT");
            int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;
            
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exchange -> {
                String response = "Vampir Bot 7/24 Aktif ve Calisiyor!";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });
            server.start();
            System.out.println("Sahte web sunucusu port " + port + " uzerinde basladi. Render artik mutlu!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. KISIM: ASIL VAMPİR BOTUMUZU BAŞLATIYORUZ
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new VampirBot());
            System.out.println("Yakamoz'un Vampir Botu başarıyla çalıştırıldı! Kanlı gece başlıyor...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
