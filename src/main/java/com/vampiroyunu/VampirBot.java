package com.vampiroyunu;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class VampirBot extends TelegramLongPollingBot {

    private Map<Long, String> oyuncular = new HashMap<>();
    private Map<Long, String> roller = new HashMap<>();
    
    private long aktifGrupChatId = 0;
    private int aktifLobiMesajId = 0;

    private boolean geceAktif = false;
    private boolean oylamaAktif = false;
    private boolean oyunBasladi = false; 

    private List<Long> hayattaOlanlar = new ArrayList<>();
    
    private Map<Long, Long> geceVampirOylari = new HashMap<>(); 
    private Map<Long, Long> geceSifaciOylari = new HashMap<>(); 
    private Map<Long, Long> geceGozcuOylari = new HashMap<>(); 
    private Map<Long, Long> geceAvciOylari = new HashMap<>(); // YENİ
    
    private Map<Long, Boolean> sarhosYetenekKullandiMi = new HashMap<>(); // YENİ
    private Map<Long, Boolean> geceSarhosKarari = new HashMap<>(); // YENİ
    
    private Long vampirKarari = null;
    
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
    
    private final String RESIM_KAZANAN_KOYLU = "https://dummyimage.com/800x600/3abf2e/ffffff.jpg&text=Zafer:+Koyluler+Kazandi";
    private final String RESIM_KAZANAN_VAMPIR = "https://i.ibb.co/wZbGnFVQ/Ekran-g-r-nt-s-2026-03-01-173520.png";

    private final String RESIM_ROL_VAMPIR = "https://i.ibb.co/8gvVTXjS/Ekran-g-r-nt-s-2026-03-01-173803.png";
    private final String RESIM_ROL_SIFACI = "https://i.ibb.co/j9FMndv4/Ekran-g-r-nt-s-2026-03-01-174326.png";
    private final String RESIM_ROL_GOZCU = "https://i.ibb.co/YFgVg1Xj/Ekran-g-r-nt-s-2026-03-01-174610.png";
    private final String RESIM_ROL_KOYLU = "https://i.ibb.co/K4tLC9p/Ekran-g-r-nt-s-2026-03-01-174008.png";
    private final String RESIM_ROL_AVCI = "https://dummyimage.com/800x600/8a5518/ffffff.jpg&text=ROLUN:+AVCI"; // YENİ
    private final String RESIM_ROL_SARHOS = "https://dummyimage.com/800x600/d6871a/ffffff.jpg&text=ROLUN:+SARHOS"; // YENİ

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
                if (aktifGrupChatId != 0) {
                    mesajGonder(chatId, "⚠️ Zaten açık bir lobi veya devam eden bir oyun var! Yeni lobi kurmak için önce /iptal yazmalısınız.");
                    return;
                }

                System.out.println("\n[GİZLİ LOG] ========================================");
                System.out.println("[GİZLİ LOG] YENİ BİR LOBİ OLUŞTURULDU! (Grup ID: " + chatId + ")");

                oyuncular.clear();
                roller.clear();
                vampirKarari = null;
                geceVampirOylari.clear();
                geceSifaciOylari.clear();
                geceGozcuOylari.clear();
                geceAvciOylari.clear();
                sarhosYetenekKullandiMi.clear();
                geceSarhosKarari.clear();
                geceAktif = false;
                oylamaAktif = false;
                oyunBasladi = false;
                aktifGrupChatId = chatId; 
                lobiMesajiniGonder(chatId);
            } 
            else if (mesajMetni.startsWith("/basla")) {
                if (aktifGrupChatId == 0) {
                    mesajGonder(chatId, "⚠️ Önce /lobi yazarak bir oyun kurmalısınız!");
                    return;
                }
                if (oyunBasladi) {
                    mesajGonder(chatId, "⚠️ Oyun zaten başladı! Yeni oyun için önce /iptal yazmalısınız.");
                    return;
                }
                
                System.out.println("[GİZLİ LOG] OYUN BAŞLATILIYOR...");
                oyunuBaslat(chatId);
            }
            else if (mesajMetni.startsWith("/help")) {
                String helpMetni = "📖 **Karanlık Köy Nasıl Oynanır?**\n\n" +
                                   "🎭 **ROLLER:**\n" +
                                   "🧛‍♂️ **Vampir:** Her gece birini avlar. Ortak kararla kurban seçilir.\n" +
                                   "💉 **Şifacı:** Her gece bir kişiyi ölümden kurtarır.\n" +
                                   "👁️ **Gözcü:** Her gece birinin ruhuna bakıp gerçek rolünü öğrenir.\n" +
                                   "🏹 **Avcı:** Sınırsız kurşunu vardır. Gece birini vurur, sadece vampirse öldürür masumsa etki etmez!\n" +
                                   "🍺 **Sarhoş:** Oyun boyunca 1 kez sabahları meydana çıkıp mani söyleyerek ortalığı şenlendirir.\n" +
                                   "🧑‍🌾 **Köylü:** Özel gücü yoktur, gündüz mantığını konuşturur.\n\n" +
                                   "🌙 **GECE:** Özel roller bota özelden mesaj atıp seçim yapar.\n" +
                                   "☀️ **GÜNDÜZ:** Ölenler açıklanır. Kalanlar tartışıp haini asmak için oylama yapar.\n\n" +
                                   "🏆 **ZAFER:** Vampirler köylülerle eşitlenirse Karanlık kazanır. Tüm vampirler asılırsa Köylüler kazanır!";
                mesajGonder(chatId, helpMetni);
            }
            else if (mesajMetni.startsWith("/iptal")) {
                if (aktifGrupChatId != 0) {
                    System.out.println("[GİZLİ LOG] OYUN İPTAL EDİLDİ!");
                    if (geceZamanlayici != null) { geceZamanlayici.cancel(); geceZamanlayici = null; }
                    if (gunduzZamanlayici != null) { gunduzZamanlayici.cancel(); gunduzZamanlayici = null; }
                    
                    oyuncular.clear();
                    roller.clear();
                    hayattaOlanlar.clear();
                    oylar.clear();
                    vampirKarari = null;
                    geceVampirOylari.clear();
                    geceSifaciOylari.clear(); 
                    geceGozcuOylari.clear();
                    geceAvciOylari.clear();
                    sarhosYetenekKullandiMi.clear();
                    geceSarhosKarari.clear();
                    geceAktif = false;
                    oylamaAktif = false;
                    oyunBasladi = false;
                    aktifGrupChatId = 0; 
                    aktifLobiMesajId = 0;
                    
                    mesajGonder(chatId, "🛑 **Oyun İptal Edildi!**\nKöy derin bir sessizliğe büründü. Tüm kayıtlar silindi.");
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
                    System.out.println("[GİZLİ LOG - LOBİ] " + isim + " oyuna katıldı. Toplam: " + oyuncular.size() + " kişi.");
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
            String queryId = update.getCallbackQuery().getId();

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(queryId);
            try { execute(answer); } catch (TelegramApiException e) { }

            if (!hayattaOlanlar.contains(userId) && !butonVerisi.startsWith("oy_ver")) {
                return; 
            }

            if (butonVerisi.startsWith("avla_vampir_")) {
                if (!geceAktif) return; 
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                geceVampirOylari.put(userId, hedefId);
                
                System.out.println("[GİZLİ LOG - GECE] 🧛‍♂️ Vampir " + oyuncular.get(userId) + " -> " + oyuncular.get(hedefId) + " kişisine oy verdi.");
                mesajiMetneCevir(chatId, messageId, "🩸 Oyunu kullandın. Konseyin kararı sabah belli olacak.");
                
                String oyVerenIsim = oyuncular.get(userId);
                String hedefIsim = oyuncular.get(hedefId);
                for (Long hayattakiId : hayattaOlanlar) {
                    if (roller.get(hayattakiId).equals("Vampir") && !hayattakiId.equals(userId)) {
                        mesajGonder(hayattakiId, "🦇 **Karanlık İletişim:**\nKardeşin **" + oyVerenIsim + "**, oyunu **" + hedefIsim + "** için kullandı!");
                    }
                }
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("koru_sifaci_")) {
                if (!geceAktif) return;
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                geceSifaciOylari.put(userId, hedefId);
                System.out.println("[GİZLİ LOG - GECE] 💉 Şifacı " + oyuncular.get(userId) + " -> " + oyuncular.get(hedefId) + " kişisini korudu.");
                mesajiMetneCevir(chatId, messageId, "💉 Şifa çantanı hazırladın. Kararın sabah etkisini gösterecek.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("bak_gozcu_")) {
                if (!geceAktif) return;
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                geceGozcuOylari.put(userId, hedefId); 
                System.out.println("[GİZLİ LOG - GECE] 👁️ Gözcü " + oyuncular.get(userId) + " -> " + oyuncular.get(hedefId) + " kişisini gözetledi.");
                mesajiMetneCevir(chatId, messageId, "👁️ Gözlerini kapattın... Raporun gün doğduğunda ruhuna fısıldanacak.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("sik_avci_")) {
                if (!geceAktif) return;
                long hedefId = Long.parseLong(butonVerisi.split("_")[2]);
                geceAvciOylari.put(userId, hedefId); 
                System.out.println("[GİZLİ LOG - GECE] 🏹 Avcı " + oyuncular.get(userId) + " -> " + oyuncular.get(hedefId) + " kişisine nişan aldı.");
                mesajiMetneCevir(chatId, messageId, "🏹 Gümüş kurşunu namluya sürdün. Sabah kimin masum kimin canavar olduğunu göreceğiz.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("sarhos_evet")) {
                if (!geceAktif) return;
                geceSarhosKarari.put(userId, true);
                System.out.println("[GİZLİ LOG - GECE] 🍺 Sarhoş " + oyuncular.get(userId) + " yeteneğini KULLANDI.");
                mesajiMetneCevir(chatId, messageId, "🍺 İçkini fondipledin! Sabah meydanda şovun var.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("sarhos_hayir")) {
                if (!geceAktif) return;
                geceSarhosKarari.put(userId, false);
                System.out.println("[GİZLİ LOG - GECE] 🛑 Sarhoş " + oyuncular.get(userId) + " sızıp uyudu.");
                mesajiMetneCevir(chatId, messageId, "🛑 Şişeyi kenara bıraktın, bu gece sızıp uyuyorsun.");
                geceBittiMiKontrolEt();
            }
            else if (butonVerisi.startsWith("oy_ver_")) {
                if (!oylamaAktif || !hayattaOlanlar.contains(userId)) return; 
                
                long hedefId = Long.parseLong(butonVerisi.replace("oy_ver_", ""));
                oylar.put(userId, hedefId); 
                
                // YENİ: GRUBA CANLI OYLAMA BİLDİRİMİ (MENTION'LI)
                String oyVerenIsim = oyuncular.get(userId);
                String oyVerenMention = "<a href=\"tg://user?id=" + userId + "\">" + oyVerenIsim + "</a>";
                
                if (hedefId == -1) {
                    mesajGonder(aktifGrupChatId, "📣 " + oyVerenMention + " **boş oy** kullandı!");
                    System.out.println("[GİZLİ LOG - GÜNDÜZ OYU] 🗳️ " + oyVerenIsim + " -> BOŞ OY");
                } else {
                    String hedefIsim = oyuncular.get(hedefId);
                    String hedefMention = "<a href=\"tg://user?id=" + hedefId + "\">" + hedefIsim + "</a>";
                    mesajGonder(aktifGrupChatId, "📣 " + oyVerenMention + ", " + hedefMention + " adlı oyuncuyu **asma yönünde** oyunu kullandı.");
                    System.out.println("[GİZLİ LOG - GÜNDÜZ OYU] 🗳️ " + oyVerenIsim + " -> " + hedefIsim);
                }
                
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
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(htmlFormatla(lobiMetni, RESIM_LOBI_KANLI_AY));
        mesaj.setParseMode("HTML");
        mesaj.setReplyMarkup(markupInline);
        try {
            Message gonderilenMesaj = execute(mesaj);
            aktifLobiMesajId = gonderilenMesaj.getMessageId();
        } catch (TelegramApiException e) { e.printStackTrace(); }
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
        EditMessageText yaziGuncelle = new EditMessageText();
        yaziGuncelle.setChatId(String.valueOf(chatId));
        yaziGuncelle.setMessageId(messageId);
        yaziGuncelle.setText(htmlFormatla(yeniMetin, RESIM_LOBI_KANLI_AY));
        yaziGuncelle.setParseMode("HTML");
        yaziGuncelle.setReplyMarkup(markupInline);
        try { execute(yaziGuncelle); } catch (TelegramApiException e) { }
    }

    private void oyunuBaslat(long grupChatId) {
        if (oyuncular.size() < 3) { 
            mesajGonder(grupChatId, "⚠️ Meydan yeterince kalabalık değil! Oynamak için en az 3 kişi olmalısınız. Bekliyoruz...");
            return;
        }

        oyunBasladi = true;

        String baslangicMetni = "Güneş kan kırmızısı bir renkle batıyor ve son ışıklar da kayboluyor...\n" +
                                "Artık kimseye güvenme.\n\n" +
                                "🌙 Gece çöktü. Rollerinizi ruhunuza fısıldamak için evlerinize geliyorum.";
        resimGonder(grupChatId, RESIM_GECE, baslangicMetni);

        int kisiSayisi = oyuncular.size();
        
        int vampirSayisi = 1; 
        if (kisiSayisi >= 8 && kisiSayisi <= 14) { vampirSayisi = 2; } 
        else if (kisiSayisi >= 15 && kisiSayisi <= 19) { vampirSayisi = 3; } 
        else if (kisiSayisi >= 20) { vampirSayisi = 4; }

        int sifaciSayisi = 1;
        if (kisiSayisi >= 6 && kisiSayisi <= 18) { sifaciSayisi = 2; } 
        else if (kisiSayisi >= 19) { sifaciSayisi = 3; }

        int gozcuSayisi = 0;
        if (kisiSayisi >= 4 && kisiSayisi <= 13) { gozcuSayisi = 1; } 
        else if (kisiSayisi >= 14) { gozcuSayisi = 2; }

        // YENİ: Avcı ve Sarhoş sayısını belirliyoruz
        int avciSayisi = (kisiSayisi >= 16) ? 2 : (kisiSayisi >= 4 ? 1 : 0);
        int sarhosSayisi = (kisiSayisi >= 5) ? 1 : 0; 

        List<String> rolHavuzu = new ArrayList<>();
        for (int i = 0; i < vampirSayisi; i++) { rolHavuzu.add("Vampir"); }
        for (int i = 0; i < sifaciSayisi; i++) { rolHavuzu.add("Şifacı"); }
        for (int i = 0; i < gozcuSayisi; i++)  { rolHavuzu.add("Gözcü");  }
        for (int i = 0; i < avciSayisi; i++)   { rolHavuzu.add("Avcı");  }
        for (int i = 0; i < sarhosSayisi; i++) { rolHavuzu.add("Sarhoş");  }
        
        while (rolHavuzu.size() < kisiSayisi) { rolHavuzu.add("Köylü"); }

        Collections.shuffle(rolHavuzu);

        System.out.println("[GİZLİ LOG - DAĞITILAN ROLLER]");
        int index = 0;
        for (Long id : oyuncular.keySet()) {
            String cekilenRol = rolHavuzu.get(index);
            roller.put(id, cekilenRol);
            System.out.println(" - " + oyuncular.get(id) + " : " + cekilenRol);
            
            if (cekilenRol.equals("Sarhoş")) {
                sarhosYetenekKullandiMi.put(id, false); // Başlangıçta kimse kullanmadı
            }
            index++;
        }

        List<Long> vampirListesi = new ArrayList<>();
        for (Long id : roller.keySet()) {
            if (roller.get(id).equals("Vampir")) vampirListesi.add(id);
        }

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
            else if (rol.equals("Avcı")) resimGonder(id, RESIM_ROL_AVCI, "🏹 **SEN BİR AVCISIN!** Sınırsız mermin var. Gece vurduğun kişi vampirse ölür, masumsa kurşun işlemez!");
            else if (rol.equals("Sarhoş")) resimGonder(id, RESIM_ROL_SARHOS, "🍺 **SEN BİR SARHOŞSUN!** Oyun boyunca sadece 1 kez gece karar verip sabah meydanda mani söyleyebilirsin.");
            else resimGonder(id, RESIM_ROL_KOYLU, "🧑‍🌾 **SEN BİR KÖYLÜSÜN.** Tek silahın aklın.");
        }

        hayattaOlanlar.clear();
        hayattaOlanlar.addAll(oyuncular.keySet());
        
        geceyiBaslat(grupChatId);
    }

    private void geceyiBaslat(long grupChatId) {
        geceAktif = true; 
        System.out.println("[GİZLİ LOG] --- GECE BAŞLADI ---");
        mesajGonder(grupChatId, "⏳ **Gece 90 saniye sürecek.** Uyanık olanlar seçimlerini yapsın.");

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            if (rol.equals("Vampir")) oyuncuSecimMenusuGonder(id, "avla_vampir", "Kimi avlıyorsun? (Vampirlerin çoğunluk oyu geçerlidir, eşitlikte rastgele kurban seçilir)");
            else if (rol.equals("Şifacı")) oyuncuSecimMenusuGonder(id, "koru_sifaci", "Karanlıkta kimin nöbetini tutacaksın?");
            else if (rol.equals("Gözcü")) oyuncuSecimMenusuGonder(id, "bak_gozcu", "Kimin ruhuna bakmak istiyorsun?");
            else if (rol.equals("Avcı")) oyuncuSecimMenusuGonder(id, "sik_avci", "Tetiği kime çekeceksin? (Sadece vampirleri öldürebilirsin)");
            else if (rol.equals("Sarhoş") && !sarhosYetenekKullandiMi.getOrDefault(id, false)) {
                sarhosSecimMenusuGonder(id); // Özel Yes/No Menüsü
            }
        }

        geceZamanlayici = new Timer();
        geceZamanlayici.schedule(new TimerTask() {
            @Override
            public void run() {
                if (geceAktif) { 
                    geceAktif = false;
                    System.out.println("[GİZLİ LOG] Süre doldu, gece otomatik bitiyor.");
                    mesajGonder(aktifGrupChatId, "⏳ Gece süresi doldu! AFK kalanların hakları yandı...");
                    gunduzuBaslat(); 
                }
            }
        }, 90 * 1000); 
    }

    private void sarhosSecimMenusuGonder(long chatId) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(htmlFormatla("🍺 Bu gece sarhoşluğunu konuşturup sabah meydana çıkacak mısın? (Sadece 1 kez kullanılabilir)", null));
        mesaj.setParseMode("HTML");
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        
        InlineKeyboardButton btnEvet = new InlineKeyboardButton();
        btnEvet.setText("🍺 Evet, Patlat Maniyi!");
        btnEvet.setCallbackData("sarhos_evet");
        rowInline.add(btnEvet);
        
        InlineKeyboardButton btnHayir = new InlineKeyboardButton();
        btnHayir.setText("🛑 Hayır, Sızıp Uyu");
        btnHayir.setCallbackData("sarhos_hayir");
        rowInline.add(btnHayir);
        
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        mesaj.setReplyMarkup(markupInline);
        try { execute(mesaj); } catch (TelegramApiException e) { }
    }

    private void geceBittiMiKontrolEt() {
        if (!geceAktif) return;

        boolean vampirlerTamam = true;
        boolean sifacilarTamam = true;
        boolean gozculerTamam = true;
        boolean avcilarTamam = true;
        boolean sarhoslarTamam = true;

        for (Long id : hayattaOlanlar) {
            String rol = roller.get(id);
            if (rol.equals("Vampir") && !geceVampirOylari.containsKey(id)) vampirlerTamam = false;
            if (rol.equals("Şifacı") && !geceSifaciOylari.containsKey(id)) sifacilarTamam = false;
            if (rol.equals("Gözcü") && !geceGozcuOylari.containsKey(id)) gozculerTamam = false;
            if (rol.equals("Avcı") && !geceAvciOylari.containsKey(id)) avcilarTamam = false;
            if (rol.equals("Sarhoş") && !sarhosYetenekKullandiMi.getOrDefault(id, false) && !geceSarhosKarari.containsKey(id)) sarhoslarTamam = false;
        }

        if (vampirlerTamam && sifacilarTamam && gozculerTamam && avcilarTamam && sarhoslarTamam) {
            geceAktif = false;
            System.out.println("[GİZLİ LOG] Herkes seçimini yaptı, gece erken bitiyor.");
            gunduzuBaslat();
        }
    }

    private void gunduzuBaslat() {
        if (geceZamanlayici != null) {
            geceZamanlayici.cancel();
            geceZamanlayici = null;
        }

        System.out.println("[GİZLİ LOG] --- SABAH OLDU ---");
        List<String> olenler = new ArrayList<>();

        // 1. VAMPİR CİNAYETİ HESAPLAMASI
        vampirKarari = null;
        if (!geceVampirOylari.isEmpty()) {
            Map<Long, Integer> vampirHedefSayaci = new HashMap<>();
            for (Long hedef : geceVampirOylari.values()) {
                vampirHedefSayaci.put(hedef, vampirHedefSayaci.getOrDefault(hedef, 0) + 1);
            }
            int maxOy = 0;
            List<Long> enCokOyAlanlar = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : vampirHedefSayaci.entrySet()) {
                if (entry.getValue() > maxOy) {
                    maxOy = entry.getValue();
                    enCokOyAlanlar.clear();
                    enCokOyAlanlar.add(entry.getKey());
                } else if (entry.getValue() == maxOy) {
                    enCokOyAlanlar.add(entry.getKey()); 
                }
            }
            if (!enCokOyAlanlar.isEmpty()) {
                Collections.shuffle(enCokOyAlanlar);
                vampirKarari = enCokOyAlanlar.get(0);
            }
        }
        
        System.out.println("Vampirlerin Nihai Hedefi: " + (vampirKarari != null ? oyuncular.get(vampirKarari) : "Kimse"));

        if (vampirKarari != null && hayattaOlanlar.contains(vampirKarari)) {
            if (!geceSifaciOylari.containsValue(vampirKarari)) { 
                hayattaOlanlar.remove(vampirKarari);
                olenler.add(oyuncular.get(vampirKarari) + " (Boynunda diş izleri var...)");
                System.out.println("[GİZLİ LOG] " + oyuncular.get(vampirKarari) + " VAMPİRLER TARAFINDAN ÖLDÜRÜLDÜ!");
            } else {
                System.out.println("[GİZLİ LOG] " + oyuncular.get(vampirKarari) + " VURULDU AMA ŞİFACI(LAR) TARAFINDAN KURTARILDI!");
            }
        }

        // 2. AVCI VURUŞLARI HESAPLAMASI
        for (Map.Entry<Long, Long> entry : geceAvciOylari.entrySet()) {
            Long avciId = entry.getKey();
            Long hedefId = entry.getValue();
            
            if (roller.get(hedefId).equals("Vampir")) {
                if (!geceSifaciOylari.containsValue(hedefId)) {
                    if (!olenler.stream().anyMatch(s -> s.contains(oyuncular.get(hedefId)))) {
                        hayattaOlanlar.remove(hedefId);
                        olenler.add(oyuncular.get(hedefId) + " (Kafasında gümüş kurşun var...)");
                        System.out.println("[GİZLİ LOG] Avcı " + oyuncular.get(avciId) + ", Vampir " + oyuncular.get(hedefId) + "'i VURDU!");
                    }
                } else {
                     System.out.println("[GİZLİ LOG] Avcı " + oyuncular.get(avciId) + " Vampir'i vurdu ama ŞİFACI KURTARDI.");
                }
            } else {
                System.out.println("[GİZLİ LOG] Avcı " + oyuncular.get(avciId) + ", masum " + oyuncular.get(hedefId) + "'e sıktı ama etki etmedi.");
                mesajGonder(avciId, "🏹 Vurduğun kişi masumdu! Gümüş kurşun ona etki etmedi.");
            }
        }

        // SABAHA UYANMA MESAJI
        String sabahMetni = "☀️ Horozlar ötmeye başladı. Uzun ve korkunç bir gece sona erdi...\n\n";
        String gonderilecekResim = RESIM_SABAH_TEMIZ;

        if (olenler.isEmpty()) {
            sabahMetni += "İnanılmaz! Bu gece kan dökülmedi. Birileri ölümün kıyısından dönmüş olabilir...\n";
        } else {
            gonderilecekResim = RESIM_SABAH_KANLI; 
            sabahMetni += "Maalesef aramızdan ayrılanlar oldu:\n";
            for (String olen : olenler) sabahMetni += "⚰️ " + olen + "\n";
        }
        resimGonder(aktifGrupChatId, gonderilecekResim, sabahMetni);

        // 3. SARHOŞUN MANİSİ (Yetenek kullanıldıysa)
        String[] manis = {
            "Rakı şarap içtim, kendimden geçtim. Gece kimi vurdular, ben neredeydim?",
            "Şişede durduğu gibi durmaz, bu köy bana hiç yaramaz. İçtim içtim sallandım, vampiri görsem tanımam!",
            "Dut gibi sarhoşum, köyün yollarında mayhoşum. Kim ölmüş kim kalmış, banane ben zaten bomboşum!",
            "Kafam trilyon, gözlerim fır fır dönüyor. Asın beni kurtulun, midem zaten bulanıyor!",
            "Gece boyu içmişim, sızıp kalmışım köşede. Vampir gelse kanım acı, şifacı gelse ilacı sek!",
            "Vur kadehi ustam, bu gece de ölmedik. Yarın asarlar beni, zaten hiç gülmedik!",
            "Meyhaneci doldur, köyün hali duman! Kim hain kim masum, bu kafayla bulamam!"
        };

        for(Long sarhosId : geceSarhosKarari.keySet()) {
            if(geceSarhosKarari.get(sarhosId)) {
                String randomMani = manis[new Random().nextInt(manis.length)];
                mesajGonder(aktifGrupChatId, "🍺 **Köyün Sarhoşu <a href=\"tg://user?id=" + sarhosId + "\">" + oyuncular.get(sarhosId) + "</a> meydana sendelemeye başladı!**\n\n🎶 _\"" + randomMani + "\"_");
                sarhosYetenekKullandiMi.put(sarhosId, true);
            }
        }

        // 4. GÖZCÜ RAPORLARI
        if (!geceGozcuOylari.isEmpty()) {
            for (Map.Entry<Long, Long> entry : geceGozcuOylari.entrySet()) {
                Long gozcuId = entry.getKey();
                Long hedefId = entry.getValue();
                mesajGonder(gozcuId, "👁️ **GÖZCÜ RAPORU:**\nGece ruhuna baktığın **" + oyuncular.get(hedefId) + "** adlı kişinin gerçek kimliği: **" + roller.get(hedefId) + "**");
            }
        }

        vampirKarari = null;
        geceVampirOylari.clear();
        geceSifaciOylari.clear();
        geceGozcuOylari.clear();
        geceAvciOylari.clear();
        geceSarhosKarari.clear();

        if (!oyunBittiMi()) {
            oylamayiBaslat(); 
        }
    }

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
        System.out.println("[GİZLİ LOG] --- GÜNDÜZ OYLAMASI BAŞLADI ---");
        
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(aktifGrupChatId));
        mesaj.setText(htmlFormatla(oylamaMetniOlustur(), null));
        mesaj.setParseMode("HTML");

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
                    System.out.println("[GİZLİ LOG] Oylama süresi doldu.");
                    oylamayiBitir();
                }
            }
        }, 120 * 1000);
    }

    private void oylamaMesajiniGuncelle() {
        EditMessageText yeniMesaj = new EditMessageText();
        yeniMesaj.setChatId(String.valueOf(aktifGrupChatId));
        yeniMesaj.setMessageId(oylamaMesajId);
        yeniMesaj.setText(htmlFormatla(oylamaMetniOlustur(), null));
        yeniMesaj.setParseMode("HTML");

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
        try { execute(yeniMesaj); } catch (TelegramApiException e) { }
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

        System.out.println("[GİZLİ LOG - İDAM SONUCU]");
        if (enCokOyAlan == null || beraberlikVarMi || enCokOyAlan == -1) {
            System.out.println("Karar: KİMSE ASILMADI.");
            mesajGonder(aktifGrupChatId, "Köy kararsız kaldı... Meydanda kimse ipe götürülmedi. Herkes korku içinde evlerine dağılıyor.");
        } else {
            hayattaOlanlar.remove(enCokOyAlan);
            String asilanRol = roller.get(enCokOyAlan);
            System.out.println("Karar: " + oyuncular.get(enCokOyAlan) + " ASILDI! (Gerçek Rolü: " + asilanRol + ")");
            resimGonder(aktifGrupChatId, RESIM_IDAM, "🪢 Halk kararını verdi!\n**<a href=\"tg://user?id=" + enCokOyAlan + "\">" + oyuncular.get(enCokOyAlan) + "</a>** darağacına götürüldü ve asıldı.\n\nGerçek kimliği: **" + asilanRol + "**");
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

        if (kotuler == 0 || kotuler >= iyiler) {
            long bitenOyunChatId = aktifGrupChatId; 
            
            System.out.println("[GİZLİ LOG] ========================================");
            System.out.println("[GİZLİ LOG] OYUN BİTTİ!");
            if (kotuler == 0) {
                System.out.println("[GİZLİ LOG] KAZANAN: KÖYLÜLER");
                resimGonder(bitenOyunChatId, RESIM_KAZANAN_KOYLU, "🎉 **KÖYLÜLER KAZANDI!** 🎉\nKöydeki tüm karanlık güçler yok edildi. Artık geceleri rahatça uyuyabilirsiniz.");
            } else {
                System.out.println("[GİZLİ LOG] KAZANAN: VAMPİRLER");
                resimGonder(bitenOyunChatId, RESIM_KAZANAN_VAMPIR, "🦇 **KARANLIK KAZANDI!** 🦇\nMasumların sayısı karanlığı durdurmaya yetmedi. Köy tamamen Vampirlerin eline geçti...");
            }

            StringBuilder sonListe = new StringBuilder("\n📜 **OYUN BİTTİ! İŞTE KÖYÜN GERÇEK YÜZÜ:**\n\n");
            for (Map.Entry<Long, String> entry : roller.entrySet()) {
                Long oyuncuId = entry.getKey();
                String oyuncuIsmi = oyuncular.get(oyuncuId);
                String rolu = entry.getValue();
                
                String durum = hayattaOlanlar.contains(oyuncuId) ? "🟢 (Hayatta)" : "☠️ (Ölü)";
                sonListe.append(durum).append(" ").append(oyuncuIsmi).append(" ➔ **").append(rolu).append("**\n");
            }
            
            mesajGonder(bitenOyunChatId, sonListe.toString());

            aktifGrupChatId = 0; 
            oyunBasladi = false;
            return true;
        }
        return false;
    }
    
    private String htmlFormatla(String metin, String resimUrl) {
        String guvenliMetin = metin.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        guvenliMetin = guvenliMetin.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        
        if (resimUrl != null && !resimUrl.isEmpty()) {
            return "<a href=\"" + resimUrl + "\">&#8203;</a>" + guvenliMetin;
        }
        return guvenliMetin;
    }

    private void resimGonder(long chatId, String resimUrl, String metin) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(htmlFormatla(metin, resimUrl));
        mesaj.setParseMode("HTML");
        try {
            execute(mesaj);
        } catch (TelegramApiException e) {
            mesajGonder(chatId, metin);
        }
    }

    private void oyuncuSecimMenusuGonder(long chatId, String islemTuru, String mesajMetni) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(htmlFormatla(mesajMetni, null));
        mesaj.setParseMode("HTML");
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
        yeniMesaj.setText(htmlFormatla(yeniMetin, null));
        yeniMesaj.setParseMode("HTML");
        try { execute(yeniMesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void mesajGonder(long chatId, String metin) {
        SendMessage mesaj = new SendMessage();
        mesaj.setChatId(String.valueOf(chatId));
        mesaj.setText(htmlFormatla(metin, null));
        mesaj.setParseMode("HTML");
        try { execute(mesaj); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}
