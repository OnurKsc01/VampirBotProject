package com.vampiroyunu;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class VampirBot extends TelegramLongPollingBot {

    private Map<Long, String> oyuncular = new HashMap<>();
    private Map<Long, String> roller = new HashMap<>();
    
    private long aktifGrupChatId = 0;
    private int aktifLobiMesajId = 0;

    // --- KİLİT MEKANİZMALARI ---
    private boolean geceAktif = false;
    private boolean oylamaAktif = false;
    private boolean oyunBasladi = false; // YENİ: Oyun başladığında komutları kilitlemek için

    private List<Long> hayattaOlanlar = new ArrayList<>();
    private Long vampirKarari = null;
    private Long sifaciKarari = null; 
    private Long gozcuKarari = null; 
    
    private Timer geceZamanlayici; 
    private Map<Long, Long> oylar = new HashMap<>(); 
    private Timer gunduzZamanlayici;
    private int oylamaMesajId = 0;

    // --- GÖRSELLER ---
    private final String RESIM_LOBI_KANLI_AY = "https://i.ibb.co/xqrQr19s/kanliay.jpg";
    private final String RESIM_GECE = "https://i.ibb.co/RGpMYMr4/gece.jpg";
    private final String RESIM_SABAH_KANLI = "https://i.ibb.co/5W3hJzLV/image.png";
    private final String RESIM_SABAH_TEMIZ = "https://i.ibb.co/gLJ4BtPW/Ekran-g-r-nt-s-2026-03-01-171702.png";
    private final String RESIM_IDAM = "https://i.ibb.co/5XKHV7Dp/idam.jpg";
    
    // YENİ: Köylü kazanma görseli URL yapısı bozuk olduğu için Telegram engelliyordu, temizlendi.
    private final String RESIM_KAZANAN_KOYLU = "https://i.ibb.co/0y7YVr4k/Ekran-g-r-nt-s-2026-03-01-172054.png";
    private final String RESIM_KAZANAN_VAMPIR = "https://i.ibb.co/wZbGnFVQ/Ekran-g-r-nt-s-2026-03-01-173520.png";

    private final String RESIM_ROL_VAMPIR = "https://i.ibb.co/8gvVTXjS/Ekran-g-r-nt-s-2026-03-01-173803.png";
    private final String RESIM_ROL_SIFACI = "https://i.ibb.co/j9FMndv4/Ekran-g-r-nt-s-2026-03-01-174326.png";
    private final String RESIM_ROL_GOZCU = "https://i.ibb.co/YFgVg1Xj/Ekran-g-r-nt-s-2026-03-01-174610.png";
    private final String RESIM_ROL_KOYLU = "https://i.ibb.co/K4tLC9p/Ekran-g-r-nt-s-2026-03-01-174008.png";

    @Override
    public String getBotUsername() {
        return "vampirkoylutr_bot"; 
    }

    @Override
    public String getBotToken() {
        return "8710387701:AAEzu3pQsPLGmF-oIGUtpzSg-gMoBc2yTCs"; 
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String mesajMetni = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            String isim = update.getMessage().getFrom().getFirstName();

            if (mesajMetni.startsWith("/lobi")) {
                // YENİ: Aktif bir lobi veya oyun varsa engelle
                if (aktifGrupChatId != 0) {
                    mesajGonder(chatId, "⚠️ Zaten açık bir lobi veya devam eden bir oyun var! Yeni lobi kurmak için önce /iptal yazmalısınız.");
                    return;
                }

                oyuncular.clear();
                roller.clear();
                vampirKarari = null;
                sifaciKarari = null;
                gozcuKarari = null;
                geceAktif = false;
                oylamaAktif = false;
                oyunBasladi = false;
                aktifGrupChatId = chatId; 
                lobiMesajiniGonder(chatId);
            } 
            else if (mesajMetni.startsWith("/basla")) {
                // YENİ: Oyun zaten başladıysa veya lobi yoksa engelle
                if (aktifGrupChatId == 0) {
                    mesajGonder(chatId, "⚠️ Önce /lobi yazarak bir oyun kurmalısınız!");
                    return;
                }
                if (oyunBasladi) {
                    mesajGonder(chatId, "⚠️ Oyun zaten başladı! Yeni oyun için önce /iptal yazmalısınız.");
                    return;
                }
                
                oyunuBaslat(chatId);
            }
            else if (mesajMetni.startsWith("/iptal")) {
                if (aktifGrupChatId != 0) {
                    if (geceZamanlayici != null) { geceZamanlayici.cancel(); geceZamanlayici = null; }
                    if (gunduzZamanlayici != null) { gunduzZamanlayici.cancel(); gunduzZamanlayici = null; }
                    
                    oyuncular.clear();
                    roller.clear();
                    hayattaOlanlar.clear();
                    oylar.clear();
                    vampirKarari = null;
                    sifaciKarari = null;
                    gozcuKarari = null;
                    geceAktif = false;
                    oylamaAktif = false;
                    oyunBasladi = false;
                    aktifGrupChatId = 0; 
                    aktifLobiMesajId = 0;
                    
                    mesajGonder(chatId, "🛑 **Oyun İptal Edildi!**\nKöy derin bir sessizliğe büründü. Tüm kayıtlar silindi. Yeni lobi için /lobi yazabilirsiniz.");
                } else {
                    mesajGonder(chatId, "Şu an iptal edilecek aktif bir lobi bulunmuyor.");
                }
            }
            else if (mesajMetni.startsWith("/start katil")) {
                if (aktifGrupChatId == 0) {
                    mesajGonder(chatId, "Şu an açık bir lobi yok. Önce grupta /lobi yazılmalı.");
                    return;
                }
                if (oyunBasladi) {
                    mesajGonder(chatId, "Maalesef oyun çoktan başladı, bu el katılamazsın.");
                    return;
                }
                
                if (!oyuncular.containsKey(userId)) {
                    oyuncular.put(userId, isim);
                    mesajGonder(chatId, "Karanlık köye giriş yaptın! Gruba dönebilirsin.");
                    
                    String yeniMetin = "Karanlık Çöküyor... Köy Meydanında Toplanıyoruz.\n\nKatılanlar (" + oyuncular.size() + " kişi):\n";
                    for (String oyuncuIsmi : oyuncular.values()) {
                        yeniMetin += "- " + oyuncuIsmi + "\n";
                    }
                    yeniMetin += "\n*(Başlamak için grupta /basla yazın veya komuta tıklayın)*";
                    
                    lobiMesajiniGuncelle(aktifGrupChatId, aktifLobiMesajId, yeniMetin);
                } else {
                    mesajGonder(chatId, "Zaten lobidesin, sabırsızlanma. Gece yaklaşıyor...");
                }
            }
        } 
        
        else if (update.hasCallbackQuery()) {
            String butonVerisi = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long userId = update.getCallbackQuery().getFrom().getId();

            if (!hayattaOlanlar.contains(userId) && !butonVerisi.startsWith("oy_ver")) {
                return; 
            }

            if (butonVerisi.startsWith("avla_vampir_")) {
                if (!geceAktif) return; 
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                vampirKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "🩸 Karar verildi. Hedefin sabah belli olacak.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("koru_sifaci_")) {
                if (!geceAktif) return;
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                sifaciKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "💉 Şifa çantanı hazırladın. Kararın sabah etkisini gösterecek.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("bak_gozcu_")) {
                if (!geceAktif) return;
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                gozcuKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "👁️ Gözlerini kapattın... Raporun gün doğduğunda ruhuna fısıldanacak.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("oy_ver_")) {
                if (!oylamaAktif || !hayattaOlanlar.contains(userId)) return; 
                
                long hedefId = Long.parseLong(butonVerisi.replace("oy_ver_", ""));
                oylar.put(userId, hedefId); 
                
                oylamaMesajiniGuncelle(); 

                if (oylar.size() == hayattaOlanlar.size()) {
                    oylamaAktif = false; 
                    if (gunduzZamanlayici != null) gunduzZamanlayici.cancel();
                    oylamayiBitir();
                }
            }
        }
    }

    private void lobiMesajiniGonder(long chatId) {
        String lobiMetni = "Karanlık Çöküyor... Köy Meydanında Toplanıyoruz.\n\nHenüz kimse katılmadı.";
        
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton buton = new InlineKeyboardButton();
        buton.setText("🗡️ Oyuna Katıl");
        buton.setUrl("https://t.me/" + getBotUsername() + "?start=katil"); 
        rowInline.add(buton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);

        try {
            SendPhoto fotograf = new SendPhoto();
            fotograf.setChatId(String.valueOf(chatId));
            fotograf.setPhoto(new InputFile(RESIM_LOBI_KANLI_AY));
            fotograf.setCaption(lobiMetni);
            fotograf.setReplyMarkup(markupInline);
            
            Message gonderilenMesaj = execute(fotograf);
            aktifLobiMesajId = gonderilenMesaj.getMessageId();
        } catch (TelegramApiException e) { 
            SendMessage duzMesaj = new SendMessage();
            duzMesaj.setChatId(String.valueOf(chatId));
            duzMesaj.setText(lobiMetni);
            duzMesaj.setReplyMarkup(markupInline);
            try {
                Message gonderilenMesaj = execute(duzMesaj);
                aktifLobiMesajId = gonderilenMesaj.getMessageId();
            } catch (TelegramApiException ex) { ex.printStackTrace(); }
        }
    }

    private void lobiMesajiniGuncelle(long chatId, int messageId, String yeniMetin) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton buton = new InlineKeyboardButton();
        buton.setText("🗡️ Oyuna Katıl");
        buton.setUrl("https://t.me/" + getBotUsername() + "?start=katil"); 
        rowInline.add(buton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);

        try {
            EditMessageCaption fotografGuncelle = new EditMessageCaption();
            fotografGuncelle.setChatId(String.valueOf(chatId));
            fotografGuncelle.setMessageId(messageId);
            fotografGuncelle.setCaption(yeniMetin);
            fotografGuncelle.setReplyMarkup(markupInline);
            execute(fotografGuncelle);
        } catch (TelegramApiException e) { 
            try {
                EditMessageText yaziGuncelle = new EditMessageText();
                yaziGuncelle.setChatId(String.valueOf(chatId));
                yaziGuncelle.setMessageId(messageId);
                yaziGuncelle.setText(yeniMetin);
                yaziGuncelle.setReplyMarkup(markupInline);
                execute(yaziGuncelle);
            } catch (TelegramApiException ex) { ex.printStackTrace(); }
        }
    }

    private void oyunuBaslat(long grupChatId) {
        if (oyuncular.size() < 1) { 
            mesajGonder(grupChatId, "Köy meydanı bomboş... Başlamak için yeterli kişi yok! Lobi hala açık, bekliyoruz...");
            return;
        }

        oyunBasladi = true; // YENİ: Oyun başladığı için lobiyi kilitledik

        String baslangicMetni = "Güneş kan kırmızısı bir renkle batıyor ve son ışıklar da kayboluyor...\n" +
                                "Artık kimseye güvenme.\n\n" +
                                "🌙 Gece çöktü. Rollerinizi ruhunuza fısıldamak için evlerinize geliyorum.";
        resimGonder(grupChatId, RESIM_GECE, baslangicMetni);

        int kisiSayisi = oyuncular.size();
        
        // VAMPİR DENGESİ (Manuel Ayar) BURAYA EKLENDİ
        int vampirSayisi = 1; // Varsayılan olarak 1 vampir
        if (kisiSayisi >= 6 && kisiSayisi <= 9) {
            vampirSayisi = 2; // 6 ile 9 kişi arasıysa 2 vampir
        } else if (kisiSayisi >= 10) {
            vampirSayisi = 3; // 10 kişi ve üzeri ise 3 vampir
        }

        List<String> rolHavuzu = new ArrayList<>();
        for (int i = 0; i < vampirSayisi; i++) {
            rolHavuzu.add("Vampir");
        }
        if (kisiSayisi >= 3) rolHavuzu.add("Şifacı"); 
        if (kisiSayisi >= 4) rolHavuzu.add("Gözcü"); 
        while (rolHavuzu.size() < kisiSayisi) rolHavuzu.add("Köylü");

        Collections.shuffle(rolHavuzu);

        // Rolleri eşleştir
        int index = 0;
        for (Long id : oyuncular.keySet()) {
            roller.put(id, rolHavuzu.get(index));
            index++;
        }

        // YENİ: Vampirleri tespit et (Birbirlerini tanımaları için)
        List<Long> vampirListesi = new ArrayList<>();
        for (Long id : roller.keySet()) {
            if (roller.get(id).equals("Vampir")) vampirListesi.add(id);
        }

        // Oyunculara mesajlarını gönder
        for (Long id : oyuncular.keySet()) {
            String rol = roller.get(id);
            
            if (rol.equals("Vampir")) {
                StringBuilder vampirMesaji = new StringBuilder("🧛‍♂️ **SEN BİR VAMPİRSİN!** Maskeni tak, avlanma vakti.");
                if (vampirListesi.size() > 1) {
                    vampirMesaji.append("\n\n🩸 **Diğer Vampir Müttefiklerin:** ");
                    for (Long vId : vampirListesi) {
                        if (!vId.equals(id)) {
                            vampirMesaji.append("\n- ").append(oyuncular.get(vId));
                        }
                    }
                }
                resimGonder(id, RESIM_ROL_VAMPIR, vampirMesaji.toString());
            }
            else if (rol.equals("Şifacı")) resimGonder(id, RESIM_ROL_SIFACI, "💉 **SEN BİR ŞİFACISIN!** Her gece birini ölümden kurtarabilirsin.");
            else if (rol.equals("Gözcü")) resimGonder(id, RESIM_ROL_GOZCU, "👁️ **SEN BİR GÖZCÜSÜN!** Her gece birinin ruhuna bakabilirsin.");
            else resimGonder(id, RESIM_ROL_KOYLU, "🧑‍🌾 **SEN BİR KÖYLÜSÜN.** Tek silahın aklın.");
        }

        hayattaOlanlar.clear();
        hayattaOlanlar.addAll(oyuncular.keySet());
        
        geceyiBaslat(grupChatId);
    }

    private void geceyiBaslat(long grupChatId) {
        geceAktif = true; 
        mesajGonder(grupChatId, "⏳ **Gece 90 saniye sürecek.** Uyanık olanlar seçimlerini yapsın.");

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            if (rol.equals("Vampir")) oyuncuSecimMenusuGonder(id, "avla_vampir", "Kimi avlıyorsun? (Takımından ilk tıklayan kararı verir)");
            else if (rol.equals("Şifacı")) oyuncuSecimMenusuGonder(id, "koru_sifaci", "Karanlıkta kimin nöbetini tutacaksın?");
            else if (rol.equals("Gözcü")) oyuncuSecimMenusuGonder(id, "bak_gozcu", "Kimin ruhuna bakmak istiyorsun?");
        }

        geceZamanlayici = new Timer();
        geceZamanlayici.schedule(new TimerTask() {
            @Override
            public void run() {
                if (geceAktif) { 
                    geceAktif = false;
                    mesajGonder(aktifGrupChatId, "⏳ Gece süresi doldu! AFK kalanların hakları yandı...");
                    gunduzuBaslat(); 
                }
            }
        }, 90 * 1000); 
    }

    private void geceBittiMiKontrolEt() {
        if (!geceAktif) return;

        boolean vampirTamam = true;
        boolean sifaciTamam = true;
        boolean gozcuTamam = true;

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            // VampirKarari null ise hiçbir vampir henüz tıklamadı demektir
            if (rol.equals("Vampir") && vampirKarari == null) vampirTamam = false;
            if (rol.equals("Şifacı") && sifaciKarari == null) sifaciTamam = false;
            if (rol.equals("Gözcü") && gozcuKarari == null) gozcuTamam = false;
        }

        if (vampirTamam && sifaciTamam && gozcuTamam) {
            geceAktif = false;
            gunduzuBaslat();
        }
    }

    private void gunduzuBaslat() {
        if (geceZamanlayici != null) {
            geceZamanlayici.cancel();
            geceZamanlayici = null;
        }

        String sabahMetni = "☀️ Horozlar ötmeye başladı. Uzun ve korkunç bir gece sona erdi...\n\n";
        List<String> olenler = new ArrayList<>();

        if (vampirKarari != null && hayattaOlanlar.contains(vampirKarari)) {
            if (!vampirKarari.equals(sifaciKarari)) { 
                hayattaOlanlar.remove(vampirKarari);
                olenler.add(oyuncular.get(vampirKarari) + " (Boynunda diş izleri var...)");
            }
        }

        String gonderilecekResim = RESIM_SABAH_TEMIZ;

        if (olenler.isEmpty()) {
            sabahMetni += "İnanılmaz! Bu gece kan dökülmedi. Birileri ölümün kıyısından dönmüş olabilir...\n";
        } else {
            gonderilecekResim = RESIM_SABAH_KANLI; 
            sabahMetni += "Maalesef aramızdan ayrılanlar oldu:\n";
            for (String olen : olenler) sabahMetni += "⚰️ " + olen + "\n";
        }
        
        resimGonder(aktifGrupChatId, gonderilecekResim, sabahMetni);

        if (gozcuKarari != null) {
            Long gozcuId = null;
            for (Long id : oyuncular.keySet()) {
                if (roller.get(id).equals("Gözcü")) { gozcuId = id; break; }
            }
            if (gozcuId != null) {
                mesajGonder(gozcuId, "👁️ **GÖZCÜ RAPORU:**\nGece ruhuna baktığın **" + oyuncular.get(gozcuKarari) + "** adlı kişinin gerçek kimliği: **" + roller.get(gozcuKarari) + "**");
            }
        }

        vampirKarari = null;
        sifaciKarari = null;
        gozcuKarari = null;

        if (!oyunBittiMi()) {
            oylamayiBaslat(); 
        }
    }

    // YENİ: Toplantıda Ölü ve Canlı Listesi Oluşturucu
    private String oylamaMetniOlustur() {
        StringBuilder metin = new StringBuilder("⚖️ **KİMİ ASIYORUZ?**\nHainin kim olduğunu bulmak için tartışma başlasın! Aşağıdan oyunu kullan.\n\n");
        
        metin.append("🟢 **Hayatta Olanlar:**\n");
        for (Long id : hayattaOlanlar) {
            metin.append("👤 ").append(oyuncular.get(id)).append("\n");
        }

        metin.append("\n🪦 **Ölenler:**\n");
        boolean olenVarMi = false;
        for (Long id : oyuncular.keySet()) {
            if (!hayattaOlanlar.contains(id)) {
                metin.append("☠️ ").append(oyuncular.get(id)).append(" (").append(roller.get(id)).append(")\n");
                olenVarMi = true;
            }
        }
        if (!olenVarMi) metin.append("- Henüz kimse ölmedi.\n");

        metin.append("\n⏳ **Süre: 2 Dakika**\n\n**Verilen Oylar:**\n");
        
        if (oylar.isEmpty()) {
            metin.append("*(Henüz kimse oy vermedi)*");
        } else {
            for (Map.Entry<Long, Long> oy : oylar.entrySet()) {
                String veren = oyuncular.get(oy.getKey());
                String verilen = oy.getValue() == -1 ? "Boş Oy 🏳️" : oyuncular.get(oy.getValue());
                metin.append("🗳️ ").append(veren).append(" ➔ ").append(verilen).append("\n");
            }
        }
        return metin.toString();
    }

    private void oylamayiBaslat() {
        oylar.clear();
        oylamaAktif = true; 
        
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(aktifGrupChatId));
        mesaj.setText(oylamaMetniOlustur());

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Long hedefId : hayattaOlanlar) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton buton = new InlineKeyboardButton();
            buton.setText("🎯 " + oyuncular.get(hedefId));
            buton.setCallbackData("oy_ver_" + hedefId); 
            rowInline.add(buton);
            rowsInline.add(rowInline);
        }
        
        List<InlineKeyboardButton> bosOySatiri = new ArrayList<>();
        InlineKeyboardButton bosOyButonu = new InlineKeyboardButton();
        bosOyButonu.setText("🏳️ Kimseyi Asma (Boş Oy)");
        bosOyButonu.setCallbackData("oy_ver_-1");
        bosOySatiri.add(bosOyButonu);
        rowsInline.add(bosOySatiri);

        markupInline.setKeyboard(rowsInline);
        mesaj.setReplyMarkup(markupInline);
        
        try {
            Message gonderilen = execute(mesaj);
            oylamaMesajId = gonderilen.getMessageId();
        } catch (TelegramApiException e) { e.printStackTrace(); }

        gunduzZamanlayici = new Timer();
        gunduzZamanlayici.schedule(new TimerTask() {
            @Override
            public void run() {
                if (oylamaAktif) { 
                    oylamaAktif = false;
                    oylamayiBitir();
                }
            }
        }, 120 * 1000);
    }

    private void oylamaMesajiniGuncelle() {
        EditMessageText yeniMesaj = new EditMessageText();
        yeniMesaj.setChatId(String.valueOf(aktifGrupChatId));
        yeniMesaj.setMessageId(oylamaMesajId);
        yeniMesaj.setText(oylamaMetniOlustur());

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (Long hedefId : hayattaOlanlar) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton buton = new InlineKeyboardButton();
            buton.setText("🎯 " + oyuncular.get(hedefId));
            buton.setCallbackData("oy_ver_" + hedefId); 
            rowInline.add(buton);
            rowsInline.add(rowInline);
        }
        List<InlineKeyboardButton> bosOySatiri = new ArrayList<>();
        InlineKeyboardButton bosOyButonu = new InlineKeyboardButton();
        bosOyButonu.setText("🏳️ Kimseyi Asma (Boş Oy)");
        bosOyButonu.setCallbackData("oy_ver_-1");
        bosOySatiri.add(bosOyButonu);
        rowsInline.add(bosOySatiri);

        markupInline.setKeyboard(rowsInline);
        yeniMesaj.setReplyMarkup(markupInline);
        try { execute(yeniMesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void oylamayiBitir() {
        mesajiMetneCevir(aktifGrupChatId, oylamaMesajId, "⚖️ **OYLAMA SONA ERDİ!** Oylar sayılıyor...");

        Map<Long, Integer> oySayaci = new HashMap<>();
        for (Long verilenOyId : oylar.values()) {
            oySayaci.put(verilenOyId, oySayaci.getOrDefault(verilenOyId, 0) + 1);
        }

        Long enCokOyAlan = null;
        int maxOy = 0;
        boolean beraberlikVarMi = false;

        for (Map.Entry<Long, Integer> giris : oySayaci.entrySet()) {
            if (giris.getValue() > maxOy) {
                maxOy = giris.getValue();
                enCokOyAlan = giris.getKey();
                beraberlikVarMi = false;
            } else if (giris.getValue() == maxOy) {
                beraberlikVarMi = true; 
            }
        }

        if (enCokOyAlan == null || beraberlikVarMi || enCokOyAlan == -1) {
            mesajGonder(aktifGrupChatId, "Köy kararsız kaldı... Meydanda kimse ipe götürülmedi. Herkes korku içinde evlerine dağılıyor.");
        } else {
            hayattaOlanlar.remove(enCokOyAlan);
            String asilanRol = roller.get(enCokOyAlan);
            resimGonder(aktifGrupChatId, RESIM_IDAM, "🪢 Halk kararını verdi!\n**" + oyuncular.get(enCokOyAlan) + "** darağacına götürüldü ve asıldı.\n\nGerçek kimliği: **" + asilanRol + "**");
        }

        if (!oyunBittiMi()) {
            Timer duraksama = new Timer();
            duraksama.schedule(new TimerTask() {
                @Override
                public void run() {
                    resimGonder(aktifGrupChatId, RESIM_GECE, "Güneş tekrar batıyor... Yeni bir gece başlıyor.");
                    geceyiBaslat(aktifGrupChatId);
                }
            }, 5000);
        }
    }

    private boolean oyunBittiMi() {
        int kotuler = 0;
        int iyiler = 0;

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            if (rol.equals("Vampir")) kotuler++;
            else iyiler++;
        }

        if (kotuler == 0) {
            resimGonder(aktifGrupChatId, RESIM_KAZANAN_KOYLU, "🎉 **KÖYLÜLER KAZANDI!** 🎉\nKöydeki tüm karanlık güçler yok edildi. Artık geceleri rahatça uyuyabilirsiniz.");
            aktifGrupChatId = 0; 
            oyunBasladi = false;
            return true;
        } else if (kotuler >= iyiler) {
            resimGonder(aktifGrupChatId, RESIM_KAZANAN_VAMPIR, "🦇 **KARANLIK KAZANDI!** 🦇\nMasumların sayısı karanlığı durdurmaya yetmedi. Köy tamamen Vampirlerin eline geçti...");
            aktifGrupChatId = 0; 
            oyunBasladi = false;
            return true;
        }
        return false;
    }

    // --- YARDIMCI METODLAR ---
    
    private void resimGonder(long chatId, String resimUrl, String metin) {
        SendPhoto fotograf = new SendPhoto();
        fotograf.setChatId(String.valueOf(chatId));
        fotograf.setPhoto(new InputFile(resimUrl));
        fotograf.setCaption(metin);
        try {
            execute(fotograf);
        } catch (TelegramApiException e) {
            mesajGonder(chatId, metin);
        }
    }

    private void oyuncuSecimMenusuGonder(long chatId, String islemTuru, String mesajMetni) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(mesajMetni);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        
        for (Long hedefId : hayattaOlanlar) {
            if (!hedefId.equals(chatId) || islemTuru.equals("koru_sifaci")) { 
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton buton = new InlineKeyboardButton();
                buton.setText("🎯 " + oyuncular.get(hedefId));
                buton.setCallbackData(islemTuru + "_" + hedefId); 
                rowInline.add(buton);
                rowsInline.add(rowInline);
            }
        }
        markupInline.setKeyboard(rowsInline);
        mesaj.setReplyMarkup(markupInline);
        try { execute(mesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void mesajiMetneCevir(long chatId, int messageId, String yeniMetin) {
        EditMessageText yeniMesaj = new EditMessageText();
        yeniMesaj.setChatId(String.valueOf(chatId));
        yeniMesaj.setMessageId(messageId);
        yeniMesaj.setText(yeniMetin);
        try { execute(yeniMesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void mesajGonder(long chatId, String metin) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(metin);
        try { execute(mesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}

