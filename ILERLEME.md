# KAİROS — İlerleme Kaydı (proje hafızası)

Her adım bitince buraya eklenir. En yeni kayıt en üstte.

## Şu anki durum
- **Durum:** Sürüm 1 planındaki 10 adımın kodu tamamlandı. **Gerçek cihazda henüz hiç çalıştırılmadı.**
- **Sıradaki:** gerçek cihaz/emülatör testi; ardından PLAN.md'deki "Sürüm 1 için kalan küçük işler"
- **Release derlemesi:** `./gradlew assembleRelease` → `~/.kairos-build/app/outputs/apk/release/app-release-unsigned.apk` (imzasız, 2,1 MB)
- **Geliştirme ortamı:** Java 17 (Temurin), Android SDK (platform 34/36, build-tools 34–36) `~/Library/Android/sdk`, Gradle 8.14.3 (wrapper), AGP 8.11.0, Kotlin 2.1.20, Compose BOM 2025.05.01
- **Derleme komutu:** `./gradlew assembleDebug` → `~/.kairos-build/app/outputs/apk/debug/app-debug.apk`
- **Derleme çıktıları iCloud dışında:** Masaüstü iCloud ile eşitlendiği için derleme klasörleri "dosya 2" çakışma kopyaları üretip derlemeyi bozuyordu. Çıktılar `~/.kairos-build/` altında (bkz. `build.gradle.kts`, `gradle.properties`). Test raporları: `~/.kairos-build/app/reports/tests/`
- **Git:** `main` dalında ilk commit atıldı (2026-11-19, proje sahibinin isteğiyle). Uzak depo (GitHub) yok, push yapılmadı

- **Testler:** `./gradlew testDebugUnitTest` · Ekran görüntüleri: `./gradlew recordRoborazziDebug` → `app/screenshots/`
- **Cihaz:** Emülatör ve bağlı telefon yok. Henüz gerçek cihazda çalıştırılmadı.

---

## 2026-11-19 — Karar uygulandı: veriler normal alanda
**Proje sahibi kararları:** "ding" sesi yok, pil için tek dokunuşlu izin yok (kodda zaten yoktu), veriler normal alana taşınsın.
**Yapılanlar**
- `KairosDatabase` ve `SettingsStore` artık Android'in normal (şifreyle korunan) alanında; "cihaz korumalı" alan kullanılmıyor
- Manifest: tüm `directBootAware` kaldırıldı, `LOCKED_BOOT_COMPLETED` dinlenmiyor
- `RescheduleReceiver`: yeniden kurma kilit ilk kez açılınca; o arada kaçanlar tek seferlik sessiz "Kaçırıldı" bildirimi
- KURALLAR §3.5 güncellendi, §3.8–3.9 eklendi (pil izni yok, ding yok)
- Uygulama hiç kurulmadığı için eski konumdan veri taşıma gerekmedi
**Doğrulama** — 126 test geçti ✓, lint temiz
- Yeni test `missedWhileOffIsShownOnceAfterUnlock`: kilit açılınca kaçan hatırlatma "Kaçırıldı" olarak sessiz kanalda bir kez geliyor; ikinci yeniden başlatmada tekrar gelmiyor

## 2026-11-19 — Adım 10: Yedekleme ve son kontroller ✓
**Yapılanlar**
- `backup/BackupCodec.kt`: okunabilir JSON yedek biçimi (`kairos-backup`, sürüm 1): tüm hatırlatmalar (tamamlananlar dahil) ve tüm ayarlar. Bozuk dosya, başka uygulamanın dosyası, daha yeni sürümün yedeği ve geçersiz hatırlatma için anlaşılır Türkçe hata mesajları
- `backup/BackupManager.kt`: kullanıcının seçtiği dosyaya yazar / dosyadan okur (Android dosya seçici). Sunucu, hesap yok
- Geri yükleme **onay penceresiyle**: yedek tarihi, yedekteki ve telefondaki hatırlatma sayısı, "geri alınamaz" uyarısı. Hatırlatmalar tek veritabanı işleminde değişir; alarmlar ve sesli özet yeniden kurulur; ilk açılış ekranı tekrar gösterilmez
- Ayarlar > **Veri** bölümü: "Yedeği dışa aktar" (önerilen ad `kairos-yedek-2026-11-19.json`), "Yedekten geri yükle"
- Yanlış olabilecek bir hata mesajı düzeltildi ("hiçbir şey değiştirilmedi" yerine doğru ifade)

**Doğrulama**
- 125 testin hepsi geçti ✓
- `BackupTest` (2): hatalı dosyalar reddediliyor; tam döngü: 3 hatırlatma + ayarlar dışa aktarılır → her şey silinip değiştirilir → geri yüklenince hatırlatmalar (önem, tekrar, tamamlanma), tema, özet ayarları ve 3 alarm (2 hatırlatma + özet) geri geliyor
- Lint: hata yok ✓
- Küçültülmüş (R8) release derlemesi başarılı ✓; veritabanı, alıcılar ve özet servisi APK'da duruyor. Release sürümü cihazda çalıştırılmadı

## Sürüm 1 — genel durum (2026-11-19)
- 10 adımın tamamı kodlandı, 125 otomatik test geçiyor
- **Doğrulanmayan:** gerçek dokunmatik ekran (kaydırma, cetvel sürükleme), gerçek bildirimlerin görünümü, gerçek Türkçe ses, Android izin pencereleri, üretici (Samsung/Xiaomi) davranışları, release sürümünün çalışması
- **Proje sahibinin kararları (2026-11-19):** "ding" sesi YOK, pil için tek dokunuşlu izin YOK (KURALLAR §3.8–3.9'a işlendi; kodda zaten böyle, değişiklik gerekmedi)
- **Depolama kararı (2026-11-19):** veriler normal, şifreyle korunan alana taşındı (aşağıdaki kayda bakın)
- Git: ilk commit atıldı; push yok

## 2026-11-19 — Adım 9: İlk açılış ve güvenilirlik ✓
**Yapılanlar**
- `reliability/Reliability.kt` (reReminder'dan): bildirim izni, kesin alarm izni, pil kısıtlaması, üretici tespiti (Xiaomi, Samsung, Huawei, OnePlus… önek eşleşmesiyle; "Micromax" Xiaomi sanılmıyor), dontkillmyapp.com rehber bağlantısı, cihazın açabildiği ilk ayar ekranını açma
- **Karar:** pil için `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (tek dokunuşla sistem penceresi) KULLANILMADI; Google Play bu izni sınırlıyor ve kurallarda yok. Uygulamanın ayar sayfası açılıyor ("Pil > Kısıtlamasız"). İstenirse proje sahibine sorulacak
- `reliability/ReliabilityUi.kt`: izin listesi (her satırda ne işe yaradığı + "Açık" ya da düğme). Zorunlu izinler dolu düğme, isteğe bağlı olanlar (pil, üretici rehberi) çerçeveli düğme: öne itilmiyor
- `ui/onboarding/OnboardingScreen.kt`: tek ekranlık ilk açılış. İzinler istenir ama **hiçbiri zorunlu değil**, "Başla" her zaman çalışır. Sesli özet sorusu: **"Hayır" seçili gelir**, "Evet, 08:00'de" seçilebilir; soru bir daha sorulmaz
- Bildirim izni: sistem penceresi; kalıcı reddedildiyse uygulamanın bildirim ayarları açılır. İzin durumu kullanıcı ayarlardan dönünce yenilenir
- Ayarlar > **Güvenilirlik** bölümü: hatırlatmaları engelleyen bir sorun varsa en üstte, yoksa en altta
- Bugün ekranında **uyarı şeridi**: sadece bildirim ya da kesin alarm izni kapalıysa; "Aç" ve "Kapat". Kapatılınca aynı sorun için bir daha gösterilmez (yeni bir sorun çıkarsa bir kez gösterilir)
- `TimeText.timeWithLocative`: doğru Türkçe bulunma eki ("08:00'de", "09:00'da", "13:00'te", "06:40'ta")

**Doğrulama** — 123 testin hepsi geçti ✓, lint'te hata yok
- `ReliabilityTest` (4): üretici tespiti, engelleyici durumlar (pil ve üretici engelleyici değil), uyarı imzası, Türkçe ekler
- `OnboardingTest` (4): cevap vermeden Başla → özet KAPALI; "Evet" → açık; açık ve koyu ekran görüntüsü

**Açık konular**
- İzin pencereleri ve Android ayar ekranlarına geçiş gerçek cihazda denenmedi
- Bildirim izni artık isteniyor: Android 13+ telefonlarda bildirimler görünür hâle geldi

## 2026-11-19 — Adım 8: Günlük sesli özet ✓
**Yapılanlar**
- `speech/BriefPlanner.kt`: bir sonraki özet anı (seçili günler, hafta sonu ayrı saat) ve karar (oku / yaz / hiçbir şey)
- `speech/BriefScheduler.kt`: tek kesin alarm; her çalışmadan sonra ertesi gün kurulur. Özet **kapalıysa alarm yok** (KURALLAR §1.4)
- `speech/BriefReceiver.kt`: bugünkü ve (istenirse) gecikmiş sayıyı hesaplar → `BriefText` cümlesi → telefon normalse okur; sessiz/titreşim/Rahatsız Etmeyin açıksa kullanıcının seçimine göre yazılı bildirim / yine de oku / hiçbir şey. Hatırlatma yoksa ve "Sessiz kal" seçiliyse hiçbir şey olmaz
- `speech/BriefSpeaker.kt` (reReminder AlertPlayer'dan): Türkçe TTS, seçilen ses kanalı (bildirim / medya / alarm) ve hız, okurken müziği geçici kısar, bitince her şeyi kapatır, en geç 30 sn'de kendini kapatır
- `speech/DailyBriefService.kt` (reReminder AlertSoundService'ten): okuma birkaç saniyelik `systemExempted` ön plan servisinde (Android arka plandaki sesi kısabilir). Servis başlatılamazsa ya da telefonda Türkçe ses yoksa cümle **sessiz bildirim** olarak gösterilir (Türkçe ses yoksa nasıl yükleneceği de yazar)
- Bildirim kanalları: "Günlük özet" (sessiz, yazılı hâl), "Sesli okuma" (en düşük önem, okuma sırasında gösterge)
- Ayarlar'daki **Dinle** düğmesi çalışıyor: örnek cümleyi seçili ses ve hızla okur; Türkçe ses yoksa uyarır ve metin okuma ayarlarını açar
- Özet ayarı her değiştiğinde alarm yeniden kurulur; yeniden başlatma / saat dilimi değişiminde de
- Manifest: FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED, TTS motoru görünürlüğü (`<queries>`), alıcı ve servis
- Lint'in bulduğu hata düzeltildi: `grafit_ground` rengi sadece gece klasöründeydi (bazı cihazlarda çökme riski)

**Doğrulama** — 115 testin hepsi geçti ✓, lint'te hata yok
- `BriefPlannerTest` (6): varsayılan kapalıyken alarm yok, gün/saat hesapları, hafta sonu saati, kararlar
- `BriefFlowTest` (6, Robolectric, gerçek Android bileşenleri): kullanıcı açmadıkça alarm kurulmuyor; açınca kuruluyor, kapatınca iptal; titreşimde yazılı özet geliyor ve **hatırlatma içeriği yok**; normal modda okuma servisi başlıyor; "Hiçbir şey yapma" seçiliyse hiçbir şey olmuyor; hatırlatma yokken sessiz kalıp ertesi güne kuruluyor

**Açık konular**
- Gerçek sesli okuma (TTS motoru) test ortamında çalıştırılamaz; gerçek cihazda dinlenmeli
- Okumadan önce kısa bir "ding" sesi araştırma raporunda vardı; eklenmedi (gerekirse sorulacak)
- Lint, kütüphanelerin daha yeni sürümlerini öneriyor (Kotlin 2.4, AGP 9.5, Compose BOM 2026.09 vb.). Toplu güncelleme ayrı bir iş olarak yapılmalı

## 2026-11-19 — Adım 7: Ayarlar ekranı ✓
**Yapılanlar**
- `ui/settings/SettingsScreen.kt`: tüm bölümler
  - **Günlük sesli özet:** ana anahtar (varsayılan KAPALI). Açılınca alt seçenekler görünür: saat, günler (her gün ayrı seçilir), hafta sonu farklı saat, örnek cümle + Dinle düğmesi (Adım 8'de bağlanacak, şimdilik gizli), gecikmişleri de söyle, hatırlatma yoksa (Sessiz kal / "Hatırlatmanız yok" de), telefon sessizdeyken (Sadece bildirim / Yine de konuş / Hiçbir şey yapma), ses (Bildirim / Medya / Alarm), konuşma hızı (Yavaş / Normal / Hızlı)
  - **Yeni hatırlatma:** varsayılan saat (Son kullanılan / Sabit saat), Sabah·Öğle·Akşam·Gece saatleri, önceden hatırlat varsayılanı
  - **Bildirimler:** erteleme süresi (bildirim düğmesine de yansır), sessiz saatler + başlangıç/bitiş, ses ve titreşim → Android'in uygulama bildirim ayarları
  - **Liste:** sağa / sola kaydırınca (Tamamla / Ertele / Sil / Hiçbir şey), tamamlananları sil (Hiçbir zaman (varsayılan) / 7 / 30 / 90 gün)
  - **Görünüm:** tema seçici, açık/koyu/telefonla aynı, saat biçimi 24/12, haftanın ilk günü
  - **Hakkında:** sürüm, lisans
- Henüz çalışmayan özelliklerin ayarı gösterilmiyor (ısrarcı mod v1.1'de)
- Kendi anahtar (switch) bileşeni: açıkken ana renk dolu, kapalıyken ince çerçeve; satırın tamamı dokunulabilir, ekran okuyucuda "anahtar"
- `speech/BriefText.kt`: özet cümlesi ("Günaydın. Bugün 4 hatırlatmanız var."); saate göre selam; içerik asla yok; yoksa ve "Sessiz kal" seçiliyse hiç konuşmaz
- `ui/settings/SettingsText.kt`: tüm seçeneklerin Türkçe adları
- Taş koyu temada seçim dairesi zemine karışıyordu → seçili olmayan dairelere belirgin kenar eklendi (Adım 2 notu kapandı)
- "Tamamlananları sil" ayarı açıksa uygulama açılışında uygulanıyor
- `MainActivity` artık tüm ayarları dinliyor; ayar değişiklikleri `SettingsStore.update` ile

**Doğrulama** — 103 testin hepsi geçti ✓
- `BriefTextTest` (7): sadece sayı, gecikmiş sadece istenirse, yoksa sessiz, selam saate göre, gün etiketleri
- `SettingsScreenshotTest` (3): varsayılan, özet açık, Taş koyu → `app/screenshots/ayarlar_tam_*.png`

**Açık konular**
- Sesli özet açılabiliyor ama henüz zamanlanmıyor/okunmuyor (Adım 8)

## 2026-11-19 — Adım 6: Hatırlatma ekleme ✓
**Yapılanlar**
- `ui/editor/EditorDraft.kt` (`EditorLogic`): taslak durumu ve saf kurallar
  - Yeni hatırlatma: son seçilen saat; bugün ve saat geçtiyse bir sonraki tam saat; "Son seçtiğiniz saat" açıklaması
  - Gün değişince: saate dokunulmadıysa öneri yeniden hesaplanır (bugün 16:00 → yarın 12:00); dokunulduysa saat korunur, sadece geçmişe düşerse ileri alınır
  - Gün çipleri: Bugün · Yarın · Cumartesi (cuma–pazar arasıysa Pazartesi) · Tarih
  - Tekrar seçenekleri: **Tekrar yok** (varsayılan) · Her gün · Hafta içi · Her <gün> · Her ayın <N>'i (gün sabitlenir; Adım 3'teki kayma notu çözüldü) · Her yıl
  - Önceden hatırlat: Yok (varsayılan) · 5 dk · 15 dk · 30 dk · 1 saat · 1 gün
- `ui/editor/TimeRuler.kt`: **saat cetveli** (imza). Kadran bölmeleri (saat başı uzun, yarım orta, çeyrek kısa), ortada pirinç ibre ve üçgen, 5 dakikalık adımlar, çeyrek saatlerde hafif titreşim, ekran okuyucu için kaydırıcı. İbre seçili saatin rakamını kesmesin diye rakam hizasında aralıklı
- `ui/editor/EditorSheet.kt`: alt sayfa. Metin alanı (yeni eklemede klavye kendiliğinden açılır, klavyedeki Tamam ile kaydedilir), büyük saat (dokununca saat yazma penceresi), cetvel, Sabah/Öğle/Akşam kısayolları (Ayarlar'daki saatlerle), geçmiş saat uyarısı, Tekrar/Önceden hatırlat/Önem, Vazgeç (yeni) / Sil (düzenleme), Kaydet (metin boşken pasif). Takvim ve saat pencereleri KAİROS renkleriyle
- Ana ekrana bağlandı: "Hatırlatma ekle" sayfayı açar; Bugün/Yakında'da satıra dokununca düzenleme açılır. Kaydedince alt mesaj: "Kaydedildi · Yarın, 12:00" (düzenlemede Geri al ile)
- **Kaydedilen saat bir sonraki eklemede varsayılan olur** (`settings.rememberUsedMinute`)

**Doğrulama** — 93 testin hepsi geçti ✓
- `EditorLogicTest` (11): varsayılanlar kapalı, son kullanılan saat, gün değişimi kuralları, gün çipleri, tekrar seçenekleri, cetvel adımları ve sınırları, geçmiş uyarısı
- `LastUsedTimeFlowTest` (2, gerçek uygulama nesneleriyle): **yarın 12:00 kaydedilir → sonraki eklemede 12:00 gelir** (proje sahibinin örneği); düzenleme kaydı günceller
- `EditorScreenshotTest` (3): Banker açık, Grafit koyu, boş taslak (Lacivert) → `app/screenshots/ekle_*.png`
- Görüntüde bulunan kusur düzeltildi: pirinç ibre seçili saatin rakamını ortadan bölüyordu

**Açık konular**
- "Özel…" tekrar ekranı (örn. 3 haftada bir salı+perşembe) yok; veri katmanı destekliyor, arayüz eksik
- Cetvel sürükleme, alt sayfa ve takvim/saat pencereleri gerçek dokunmatik ekranda denenmedi
- Takvim penceresinin dili telefonun diline göre gelir (Türkçe telefonda Türkçe)

## 2026-11-19 — Adım 5: Ana ekranlar ✓
**Yapılanlar**
- `ui/components/KairosIcons.kt`: kendi ince çizgili ikon seti (Bugün, Yakında, Onay, Ayarlar, Artı, Zil, Tekrar, Ertele, Sil, Geri aç). Hazır Material ikon paketi kullanılmadı
- `ui/components/Common.kt`: bölüm etiketi (Türkçe büyük harf: i → İ), birincil düğme, tamamlama dairesi (44 dp dokunma alanı), ince çizgi, boş liste notu
- `ui/home/HomeUi.kt` (`HomeUiBuilder`): kayıtları ekran satırlarına çeviren saf mantık. "Sıradaki · 38 dk sonra", "Ertelendi · 12:10", tekrar açıklaması, gecikmiş "Dün, 17:00", özet satırı ("3 hatırlatma · 1 gecikmiş" / "Bugün için hatırlatma yok" / "Bugünkü hatırlatmalar tamamlandı"). Bugün tamamlananlar üstü çizili olarak listede kalır
- `util/RecurrenceText.kt`: "Her gün", "Hafta içi", "Her pazartesi, perşembe", "2 haftada bir cuma", "Her ayın 15'i", "Her ayın son cuma"; Türkçe ek uyumu (1'i, 2'si, 3'ü, 6'sı, 9'u, 10'u)
- `ui/home/HomeViewModel.kt`: dakika başında kendini yeniler; tamamla / geri aç / sil / ertele; her işlemde **Geri al** mesajı (tekrarlayan tamamlanınca "Sonraki: Perşembe")
- `ui/home/TodayScreen.kt`, `ListScreens.kt` (Yakında: Yarın / Bu hafta / Daha sonra; Tamamlanan: "Geri aç"), `ReminderRows.kt` (saat sütunlu satır, gecikmiş kartı, kaydırma)
- Kaydırma: sağ = Tamamla, sol = Ertele (varsayılan; Ayarlar'dan değişecek). Ertele kaydırması erteleme seçenekleri sayfasını açar (Birazdan / 1 saat sonra / Bu akşam / Yarın sabah)
- `ui/home/HomeScreen.kt`: alt menü (seçili sekmede pirinç çizgi), "Hatırlatma ekle" düğmesi, geri tuşu Bugün'e döner
- `ui/settings/SettingsScreen.kt`: GEÇİCİ, sadece tema bölümü (Adım 7'de tamamlanacak)
- Geçici `ThemePreviewScreen` ve `ThemeScreenshotTest` silindi; yerine `HomeScreenshotTest`

**Doğrulama** — 77 testin hepsi geçti ✓
- `HomeUiBuilderTest` (6): taslaktaki Bugün ekranı birebir, özet metinleri, Yakında grupları, 12 saat sıralaması, tekrar metinleri ve Türkçe ekler
- `HomeScreenshotTest` (10): Bugün (6 tema hâli), Yakında, Tamamlanan, Ayarlar, boş Bugün → `app/screenshots/`
- Ekran görüntüsünde bulunan hata düzeltildi: kaydırma arka planı satır dururken kenarlardan koyu şerit olarak görünüyordu

**Notlar**
- "Hatırlatma ekle" düğmesi şimdilik bir şey yapmıyor (Adım 6)
- Satıra dokununca düzenleme açılması Adım 6'da eklenecek
- Kaydırma ve geri al gerçek dokunmatik ekranda denenmedi (cihaz yok)

## 2026-11-19 — Adım 4: Alarm ve bildirim altyapısı ✓
**Yapılanlar**
- `alarm/AlarmPlanner.kt`: hangi alarmın ne zaman kurulacağını hesaplayan saf mantık (asıl alarm, opsiyonel önceden hatırlatma, erteleme; geçmiş saat için alarm yok; eski/geçersiz alarmı tanıma)
- `alarm/AlarmScheduler.kt` (reReminder'dan): `setExactAndAllowWhileIdle`; kesin alarm izni yoksa hatırlatmayı düşürmeden kesin olmayan alarma geçer
- `alarm/ReminderAlarmReceiver.kt`: alarm çalınca hatırlatma hâlâ geçerliyse bildirim gösterir
- `alarm/RescheduleReceiver.kt` (reReminder'dan): yeniden başlatma (kilit açılmadan da), güncelleme, saat/saat dilimi değişimi, izin değişimi → tüm alarmlar yeniden kurulur; kapalıyken kaçırılanlar için tek seferlik **sessiz** bildirim
- `notification/Notifications.kt`: 3 kanal (Hatırlatmalar, Önceden hatırlatmalar, Kaçırılanlar); bildirimde **Tamamlandı · 10 dk ertele · 1 saat**; sessiz saatler açıksa ses çıkarmaz
- `notification/ActionReceiver.kt` (Remember'dan): düğmeler uygulamayı açmadan çalışır
- `snooze/SnoozePresets.kt` (Remember'dan): Birazdan / 1 saat sonra / Bu akşam / Yarın sabah (Adım 5–6'da menüde kullanılacak)
- `util/TimeText.kt`: Türkçe saat ve gün metinleri ("Bugün, 12:00", "Pazartesi", "1 Aralık", 12 saat modunda ÖÖ/ÖS)
- `KairosApp` + `AppGraph`: uygulama genelinde tek örnekler; repository her değişiklikte alarmları günceller
- Veritabanı ve ayarlar **cihaz korumalı depolamada** (KURALLAR §3.5: kilit açılmadan önce de alarm kurulabilsin)
- `Reminder`'a `last_notified_at` eklendi (yeniden başlatmada aynı hatırlatma iki kez bildirilmesin)
- Manifest: POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, VIBRATE; 3 alıcı
- Durum çubuğu simgesi `ic_notification` (kadran bölmeleri)

**Doğrulama** — 71 testin hepsi geçti ✓
- `AlarmPlannerTest` (9): seyahatte duvar saati korunuyor, yaz saati boşluğu, önceden hatırlatma varsayılan kapalı, düzenlenen hatırlatmanın eski alarmı yok sayılıyor
- `ReminderFlowTest` (5, Robolectric, gerçek Android bileşenleri): ekle → kesin alarm kuruldu; alarm çaldı → bildirim + 3 düğme; Tamamlandı → hatırlatma bitti, bildirim kalktı, alarm iptal; Ertele → yeni alarm; düzenleme sonrası eski alarm bir şey göstermiyor; yeniden başlatma → hepsi yeniden kuruldu
- `SnoozePresetsTest` (4): erteleme seçenekleri, Türkçe metinler, gece yarısını aşan sessiz saatler

**Notlar / açık konular**
- Bildirim ve kesin alarm **izinleri henüz istenmiyor** (Adım 9). O zamana kadar Android 13+ cihazda bildirim görünmez
- ~~Cihaz korumalı depolama~~ → sonradan proje sahibi kararıyla normal alana taşındı
- Testlerde ortaya çıkan sorun düzeltildi: veritabanı ve ayarlar statik tekil nesneydi; artık `AppGraph`'e ait
- Hâlâ gerçek cihazda denenmedi

## 2026-11-19 — Adım 3: Veri katmanı ✓
**Yapılanlar**
- Room 2.7.1 + KSP, kotlinx.serialization eklendi; şema `app/schemas/` altına dışa aktarılıyor (sürüm 1)
- `data/Reminder.kt`: tarih + günün dakikası olarak saklanır (saat dilimi/yaz saati değişse de 12:00 → 12:00). Tekrar, önceden hatırlatma, önem, ısrarcı mod varsayılan kapalı/normal
- `data/Recurrence.kt`: Remember'dan uyarlandı (günlük / haftalık gün seçimli / aylık ayın günü veya N'inci haftanın günü / yıllık; tarih veya sayıyla bitiş). Saatlik tekrar çıkarıldı. NOTICE.md'ye işlendi
- `data/ReminderDao.kt`, `KairosDatabase.kt`, `ReminderRepository.kt`: ekle, düzenle, tamamla (tekrarlayan bir sonraki tarihe taşınır), ertele, sil; değiştiren her işlem "Geri al" için önceki kaydı döndürür; sesli özet için gün sayıları
- `data/ReminderBuckets.kt`: Gecikmiş / Bugün / Yarın / Bu hafta / Daha sonra gruplaması (ertelenen gecikmiş sayılmaz)
- `data/settings/AppSettings.kt` + `SettingsStore.kt`: tüm ayarlar (görünüm, günlük özet, yeni hatırlatma, bildirimler, liste, ilk açılış). **Opsiyonel her şey varsayılan kapalı**
- `data/TimeSuggestion.kt`: son kullanılan saat; bugün ve saat geçmişse bir sonraki tam saat; 23:00 sonrası yarına geçer

**Doğrulama** — 53 testin hepsi geçti ✓
- `RecurrenceRuleTest` (12): hafta içi, iki haftada bir Pzt+Per, ayın 31'i kısa aylarda kaymıyor, ayın son cuması, artık yıl, bitişler, JSON
- `TimeSuggestionTest` (7): son kullanılan saat, geçmiş saat → sonraki tam saat, gece yarısı → yarın, sabit saat modu
- `ReminderBucketsTest` (3), `ReminderRepositoryTest` (7, gerçek Room veritabanıyla): tamamla/geri al, tekrarlayanın ilerlemesi, sil/geri al, erteleme temizliği, özet sayıları
- `AppSettingsDefaultsTest` (11): KURALLAR §1.2'deki her "varsayılan kapalı" kuralı tek tek denetleniyor; tüm ayarlar yazılıp okununca aynı kalıyor

**Notlar**
- Aylık tekrarda `monthlyMode` boş bırakılırsa 31'inden kısa aylarda gün kayabilir. Ekleme ekranı (Adım 6) aylık seçildiğinde her zaman `ByDayOfMonth(gün)` yazmalı
- `Appearance` sınıfı `SettingsStore.kt`'den `AppSettings.kt`'ye taşındı

## 2026-11-19 — Adım 2: Tema sistemi ✓
**Yapılanlar**
- `ui/theme/Palettes.kt`: 5 tema (Banker, Lacivert, Bordo, Taş, Grafit) × açık/koyu = 10 renk tablosu; `ThemeMode` (Açık / Koyu / Telefonla aynı, varsayılan "Telefonla aynı"); varsayılan tema Banker
- `ui/theme/Theme.kt`: `KairosTheme`, `Kairos.colors`. Material'in tüm renk yuvaları KAİROS renklerine bağlandı; varsayılan mor hiçbir bileşende görünemez
- `ui/theme/Type.kt`: Schibsted Grotesk (değişken ağırlık) tüm metinlerde, Barlow Semi Condensed saatlerde (`TimeStyles`, eşit genişlikte rakamlar)
- Yazı tipleri `res/font/` altında, OFL lisans metinleri `licenses/` altında, `NOTICE.md` güncellendi
- `data/settings/SettingsStore.kt`: DataStore, tema ve mod seçimi kalıcı
- `ui/settings/ThemePicker.kt`: Ayarlar'da kullanılacak tema seçici (her daire temanın açık ve koyu yarısını gösterir)
- `MainActivity`: kayıtlı temayla açılır; ayar okunana kadar çizim yapmaz (yanlış tema yanıp sönmez); durum çubuğu ikonları temaya göre açık/koyu
- `ui/preview/ThemePreviewScreen.kt`: **GEÇİCİ** doğrulama ekranı, Adım 5'te Bugün ekranıyla değişecek

**Doğrulama**
- `PaletteRulesTest` (3 test): 10 tablonun hepsinde metin kontrastı WCAG eşiklerini geçiyor, mor/indigo yok, Material moru yok ✓
- `ThemeScreenshotTest` (10 test): her tema hâli görüntülendi → `app/screenshots/tema_*.png`; yazı tipleri ve Türkçe karakterler doğru ✓
- `assembleDebug` başarılı ✓

**Notlar / açık konular**
- Bordo açık temanın "gecikmiş" rengi yanlışlıkla #9A4A28 yazılmıştı; kurallardaki #A0522D'nin testi geçtiği görülünce kurala geri dönüldü
- Taş koyu temada, seçicideki Taş dairesi zemine çok yakın; Adım 7'de (Ayarlar) daireye belirgin bir kenar eklenecek
- Türetilen koyu tonlar proje sahibinin görsel onayını bekliyor

## 2026-11-19 — Adım 1: Proje iskeleti ✓
**Yapılanlar**
- Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`), wrapper 8.14.3
- `app` modülü: paket `app.kairos`, minSdk 26, target/compileSdk 36, Java 17, Compose
- `MainActivity` (geçici yer tutucu ekran, Banker zemin rengi)
- Açılış penceresi teması: açıkta Banker zemini, koyu modda Grafit zemini (`res/values*/themes.xml`)
- Uygulama simgesi: Banker yeşili zemin üzerinde kadran bölmeleri ve pirinç ibre (vektör, gradyan yok)
- `LICENSE` (GPL-3.0), `NOTICE.md`, `.gitignore`, `local.properties` (git dışı)

**Doğrulama**
- `./gradlew assembleDebug` → BUILD SUCCESSFUL, uyarı yok, APK 10 MB (debug)
- Henüz cihazda/emülatörde çalıştırılmadı

## 2026-11-19 — Adım 0: Kaynak kodlar ✓
- reReminder (a963087) ve Remember (80d099c) `kaynak/` altına indirildi (git dışı, sadece okuma)
- Alınacak 19 dosya `kaynak/secilenler/` altına ayrıldı; dosya → KAİROS eşleşmesi `kaynak/KAYNAKLAR.md`'de
- Tasks.org'dan `VoiceOutputAssistant.java` ve iki ayar XML'i alındı
- Kaynak dosyalarda dosya başı telif satırı yok; uyarlarken depo + commit notu eklenecek

## 2026-11-19 — Adım 0: Belgeler
**Yapılanlar**
- ~4.400 GitHub deposu tarandı, rakip uygulamalar ve Android kuralları araştırıldı → `ARASTIRMA.md`
- Tasarım yönü belirlendi ve taslak tuval yayınlandı: https://claude.ai/artifact/6gy8Qg6Sw9YPwtnnrKcB5s (kimlik panosu + Bugün, Ekle, Ayarlar, Bugün-Grafit ekranları)
- Proje sahibi kararları: uygulama public (GPL kod alınabilir), tekrar ve sesli özet sadece istenirse, tüm temalar kullanıcıya sunulacak
- `KURALLAR.md`, `PLAN.md`, `ILERLEME.md`, `CLAUDE.md` oluşturuldu

**Açık konular**
- Grafit dışındaki temaların koyu hâlleri, Grafit'in açık hâli henüz tasarlanmadı (Adım 2'de renk tablosu olarak üretilecek)
