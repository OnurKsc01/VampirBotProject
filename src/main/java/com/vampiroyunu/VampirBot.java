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
    private Map<Long, Boolean> avciKursunuKullandiMi = new HashMap<>();
    
    private long aktifGrupChatId = 0;
    private int aktifLobiMesajId = 0;

    private List<Long> hayattaOlanlar = new ArrayList<>();
    private Long vampirKarari = null;
    private Long yarasaKarari = null;
    private Long sifaciKarari = null; 
    private Timer geceZamanlayici; 

    private Map<Long, Long> oylar = new HashMap<>(); 
    private Timer gunduzZamanlayici;
    private int oylamaMesajId = 0;

    // --- TELEGRAMIN %100 KABUL ETTİĞİ (ENGELSİZ) GÖRSELLER ---
    private final String RESIM_LOBI_KANLI_AY = "https://i.ibb.co/xqrQr19s/kanliay.jpg";
    private final String RESIM_GECE = "https://i.ibb.co/RGpMYMr4/gece.jpg";
    private final String RESIM_SABAH_KANLI = "https://i.ibb.co/5W3hJzLV/image.png";
    private final String RESIM_SABAH_TEMIZ = "https://i.ibb.co/gLJ4BtPW/Ekran-g-r-nt-s-2026-03-01-171702.png";
    private final String RESIM_IDAM = "https://i.ibb.co/5XKHV7Dp/idam.jpg";
    private final String RESIM_KAZANAN_KOYLU = "https://i.ibb.co/0y7YVr4k/Ekran-g-r-nt-s-2026-03-01-172054.png";
    private final String RESIM_KAZANAN_VAMPIR = "https://i.ibb.co/wZbGnFVQ/Ekran-g-r-nt-s-2026-03-01-173520.png";

    private final String RESIM_ROL_VAMPIR = "https://i.ibb.co/8gvVTXjS/Ekran-g-r-nt-s-2026-03-01-173803.png";
    private final String RESIM_ROL_SIFACI = "https://i.ibb.co/j9FMndv4/Ekran-g-r-nt-s-2026-03-01-174326.png";
    private final String RESIM_ROL_GOZCU = "https://i.ibb.co/YFgVg1Xj/Ekran-g-r-nt-s-2026-03-01-174610.png";
    private final String RESIM_ROL_AVCI = "https://i.ibb.co/NgPKwwLR/Ekran-g-r-nt-s-2026-03-01-174504.png";
    private final String RESIM_ROL_KOYLU = "https://i.ibb.co/K4tLC9p/Ekran-g-r-nt-s-2026-03-01-174008.png";
    private final String RESIM_ROL_YARASA = "https://i.ibb.co/6cQCvs1t/Ekran-g-r-nt-s-2026-03-01-174239.png";

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

            if (mesajMetni.equals("/lobi")) {
                oyuncular.clear();
                roller.clear();
                avciKursunuKullandiMi.clear();
                vampirKarari = null;
                yarasaKarari = null;
                sifaciKarari = null;
                aktifGrupChatId = chatId; 
                lobiMesajiniGonder(chatId);
            } 
            else if (mesajMetni.equals("/basla")) {
                oyunuBaslat(chatId);
            }
            else if (mesajMetni.startsWith("/start katil")) {
                if (aktifGrupChatId == 0) {
                    mesajGonder(chatId, "Şu an açık bir lobi yok. Önce grupta /lobi yazılmalı.");
                    return;
                }
                
                if (!oyuncular.containsKey(userId)) {
                    oyuncular.put(userId, isim);
                    mesajGonder(chatId, "Karanlık köye giriş yaptın! Gruba dönebilirsin.");
                    
                    String yeniMetin = "Karanlık Çöküyor... Köy Meydanında Toplanıyoruz.\n\nKatılanlar (" + oyuncular.size() + " kişi):\n";
                    for (String oyuncuIsmi : oyuncular.values()) {
                        yeniMetin += "- " + oyuncuIsmi + "\n";
                    }
                    yeniMetin += "\n*(Başlamak için grupta /basla yazın)*";
                    
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

            if (butonVerisi.startsWith("avla_vampir_")) {
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                vampirKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "🩸 Karar verildi. Hedefin: " + oyuncular.get(hedefId));
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("avla_yarasa_")) {
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                yarasaKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "🦇 Hedefin belirlendi: " + oyuncular.get(hedefId));
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("koru_sifaci_")) {
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                sifaciKarari = hedefId;
                mesajiMetneCevir(chatId, messageId, "💉 Şifa çantanı hazırladın. Bu gece koruduğun kişi: " + oyuncular.get(hedefId));
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("bak_gozcu_")) {
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                mesajiMetneCevir(chatId, messageId, "👁️ Gözlerini kapattın...\n**" + oyuncular.get(hedefId) + "** adlı kişinin rolü: **" + roller.get(hedefId) + "**");
            }
            else if (butonVerisi.startsWith("ates_avci_")) {
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                avciKursunuKullandiMi.put(userId, true);
                mesajiMetneCevir(chatId, messageId, "💥 Tetiği çektin! **" + oyuncular.get(hedefId) + "** vuruldu.");
                hayattaOlanlar.remove(hedefId);
                resimGonder(aktifGrupChatId, RESIM_SABAH_KANLI, "💥 Gecenin sessizliğini yırtan korkunç bir silah sesi duyuldu! Biri vuruldu...");
            }
            else if (butonVerisi.startsWith("oy_ver_")) {
                if (!hayattaOlanlar.contains(userId)) return; 
                
                long hedefId = Long.parseLong(butonVerisi.replace("oy_ver_", ""));
                oylar.put(userId, hedefId); 
                
                oylamaMesajiniGuncelle(); 

                if (oylar.size() == hayattaOlanlar.size()) {
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
            mesajGonder(grupChatId, "Köy meydanı bomboş... Başlamak için yeterli kişi yok!");
            return;
        }

        String baslangicMetni = "Güneş kan kırmızısı bir renkle batıyor ve son ışıklar da kayboluyor...\n" +
                                "Artık kimseye güvenme.\n\n" +
                                "🌙 Gece çöktü. Rollerinizi ruhunuza fısıldamak için evlerinize geliyorum.";
        resimGonder(grupChatId, RESIM_GECE, baslangicMetni);

        List<String> rolHavuzu = new ArrayList<>();
        int kisiSayisi = oyuncular.size();

        if (kisiSayisi >= 1) rolHavuzu.add("Vampir");
        if (kisiSayisi >= 2) rolHavuzu.add("Şifacı"); 
        if (kisiSayisi >= 3) rolHavuzu.add("Gözcü"); 
        if (kisiSayisi >= 4) rolHavuzu.add("Avcı");
        if (kisiSayisi >= 5) rolHavuzu.add("Yarasa");
        while (rolHavuzu.size() < kisiSayisi) rolHavuzu.add("Köylü");

        Collections.shuffle(rolHavuzu);

        int index = 0;
        for (Long id : oyuncular.keySet()) {
            String cekilenRol = rolHavuzu.get(index);
            roller.put(id, cekilenRol); 
            
            if (cekilenRol.equals("Vampir")) resimGonder(id, RESIM_ROL_VAMPIR, "🧛‍♂️ **SEN BİR VAMPİRSİN!** Maskeni tak, avlanma vakti.");
            else if (cekilenRol.equals("Şifacı")) resimGonder(id, RESIM_ROL_SIFACI, "💉 **SEN BİR ŞİFACISIN!** Her gece birini ölümden kurtarabilirsin.");
            else if (cekilenRol.equals("Yarasa")) resimGonder(id, RESIM_ROL_YARASA, "🦇 **SEN BİR YARASASIN!** Vampirin en sadık yardımcısı sensin.");
            else if (cekilenRol.equals("Gözcü")) resimGonder(id, RESIM_ROL_GOZCU, "👁️ **SEN BİR GÖZCÜSÜN!** Her gece birinin ruhuna bakabilirsin.");
            else if (cekilenRol.equals("Avcı")) {
                avciKursunuKullandiMi.put(id, false); 
                resimGonder(id, RESIM_ROL_AVCI, "🏹 **SEN BİR AVCISIN!** Silahında tek bir gümüş kurşun var.");
            }
            else resimGonder(id, RESIM_ROL_KOYLU, "🧑‍🌾 **SEN BİR KÖYLÜSÜN.** Tek silahın aklın.");
            index++;
        }

        hayattaOlanlar.clear();
        hayattaOlanlar.addAll(oyuncular.keySet());
        
        geceyiBaslat(grupChatId);
    }

    private void geceyiBaslat(long grupChatId) {
        mesajGonder(grupChatId, "⏳ **Gece 90 saniye sürecek.** Uyanık olanlar seçimlerini yapsın.");

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            if (rol.equals("Vampir")) oyuncuSecimMenusuGonder(id, "avla_vampir", "Kimi avlıyorsun?");
            else if (rol.equals("Şifacı")) oyuncuSecimMenusuGonder(id, "koru_sifaci", "Karanlıkta kimin nöbetini tutacaksın?");
            else if (rol.equals("Yarasa")) oyuncuSecimMenusuGonder(id, "avla_yarasa", "Kimi karanlığa çekiyorsun?");
            else if (rol.equals("Gözcü")) oyuncuSecimMenusuGonder(id, "bak_gozcu", "Kimin ruhuna bakmak istiyorsun?");
            else if (rol.equals("Avcı")) {
                if (!avciKursunuKullandiMi.get(id)) oyuncuSecimMenusuGonder(id, "ates_avci", "Bu gece kurşununu kullanacak mısın?");
            }
        }

        geceZamanlayici = new Timer();
        geceZamanlayici.schedule(new TimerTask() {
            @Override
            public void run() {
                mesajGonder(aktifGrupChatId, "⏳ Gece süresi doldu! AFK kalanların hakları yandı...");
                gunduzuBaslat(); 
            }
        }, 90 * 1000); 
    }

    private void geceBittiMiKontrolEt() {
        boolean vampirTamam = true;
        boolean yarasaTamam = true;
        boolean sifaciTamam = true;

        for (Long id : hayattaOlanlar) {
            if (roller.get(id).equals("Vampir") && vampirKarari == null) vampirTamam = false;
            if (roller.get(id).equals("Yarasa") && yarasaKarari == null) yarasaTamam = false;
            if (roller.get(id).equals("Şifacı") && sifaciKarari == null) sifaciTamam = false;
        }

        if (vampirTamam && yarasaTamam && sifaciTamam) gunduzuBaslat();
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
        
        if (yarasaKarari != null && hayattaOlanlar.contains(yarasaKarari)) {
            if (!yarasaKarari.equals(sifaciKarari)) { 
                hayattaOlanlar.remove(yarasaKarari);
                if (!olenler.stream().anyMatch(s -> s.contains(oyuncular.get(yarasaKarari)))) {
                    olenler.add(oyuncular.get(yarasaKarari) + " (Karanlığa çekilmiş...)");
                }
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
        
        vampirKarari = null;
        yarasaKarari = null;
        sifaciKarari = null;
        
        resimGonder(aktifGrupChatId, gonderilecekResim, sabahMetni);

        if (!oyunBittiMi()) {
            oylamayiBaslat(); 
        }
    }

    private void oylamayiBaslat() {
        oylar.clear();
        String oylamaMetni = "⚖️ **KİMİ ASIYORUZ?**\n\nHainin kim olduğunu bulmak için tartışma başlasın! Aşağıdan oyunu kullan.\n⏳ **Süre: 2 Dakika**\n\nVerilen Oylar:\n*(Henüz kimse oy vermedi)*";
        
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(aktifGrupChatId));
        mesaj.setText(oylamaMetni);

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
                oylamayiBitir();
            }
        }, 120 * 1000);
    }

    private void oylamaMesajiniGuncelle() {
        String metin = "⚖️ **KİMİ ASIYORUZ?**\n\n⏳ **Süre: 2 Dakika**\n\n**Verilen Oylar:**\n";
        
        for (Map.Entry<Long, Long> oy : oylar.entrySet()) {
            String veren = oyuncular.get(oy.getKey());
            String verilen = oy.getValue() == -1 ? "Boş Oy 🏳️" : oyuncular.get(oy.getValue());
            metin += "👤 " + veren + " ➔ " + verilen + "\n";
        }

        EditMessageText yeniMesaj = new EditMessageText();
        yeniMesaj.setChatId(String.valueOf(aktifGrupChatId));
        yeniMesaj.setMessageId(oylamaMesajId);
        yeniMesaj.setText(metin);

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
            if (rol.equals("Vampir") || rol.equals("Yarasa")) kotuler++;
            else iyiler++;
        }

        if (kotuler == 0) {
            resimGonder(aktifGrupChatId, RESIM_KAZANAN_KOYLU, "🎉 **KÖYLÜLER KAZANDI!** 🎉\nKöydeki tüm karanlık güçler yok edildi. Artık geceleri rahatça uyuyabilirsiniz.");
            return true;
        } else if (kotuler >= iyiler) {
            resimGonder(aktifGrupChatId, RESIM_KAZANAN_VAMPIR, "🦇 **KARANLIK KAZANDI!** 🦇\nMasumların sayısı karanlığı durdurmaya yetmedi. Köy tamamen Vampirlerin eline geçti...");
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