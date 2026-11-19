# KAİROS — Hatırlatıcı Uygulaması Araştırma ve Öneri Raporu
_Tarih: 19 Kasım 2026_

## 1. Araştırmanın kapsamı
- GitHub'da 50+ farklı sorgu (anahtar kelime + topic + dil filtreleri) ile **~4.400 benzersiz depo** tarandı.
- Öne çıkan ~15 deponun README'si, ayar dosyaları ve alarm/TTS kodu incelendi.
- Ticari uygulamalar (Todoist, TickTick, Any.do, Due, Google Tasks) ve Android'in resmi alarm/arka plan kuralları araştırıldı.

## 2. İşimize yarayacak depolar

| Depo | ★ | Lisans | Ondan ne alacağız |
|---|---|---|---|
| [tasks/tasks](https://github.com/tasks/tasks) (Tasks.org) | 5.8k | GPL-3.0 | Ayar ekranı yapısı; "Sabah/Öğle/Akşam/Gece" ayarlanabilir saat kısayolları; sesli hatırlatma, sessiz saatler, kaydırarak erteleme, kalıcı bildirim ayarları |
| [ProfessorQuantumUniverse/reReminder](https://github.com/ProfessorQuantumUniverse/reReminder) | 44 | GPL-3.0 | **TTS'i arka planda güvenilir okutma** (systemExempted foreground service), konuşma ses kanalı seçimi (bildirim/medya/alarm), Doze/yeniden başlatma/saat dilimi değişimine dayanıklı alarm |
| [bikram-agarwal/Remember](https://github.com/bikram-agarwal/Remember) | 175 | GPL-3.0 | "Yapılana kadar hatırlat", insan diliyle erteleme (*birazdan, bu akşam, yarın sabah*), sessiz kalıcı özet bildirimi, izin onboarding'i |
| [felixwiemuth/SimpleReminder](https://github.com/felixwiemuth/SimpleReminder) | 77 | — | Hızlı ekleme: Quick Settings kutucuğu, `12:30` / `+1:30` yazarak saat, şablonlar, bildirimden yeniden zamanlama |
| [meticha/triggerx](https://github.com/meticha/triggerx) | 99 | **Apache-2.0** | Kilit ekranında tam ekran alarm, Android sürümüne göre doğru bayraklar — doğrudan kütüphane olarak kullanılabilir |
| [FossifyOrg/Clock](https://github.com/FossifyOrg/Clock), [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) | 735 / 622 | GPL-3.0 | Kesin alarm, açılışta yeniden kurma, alarm ekranı örnekleri |
| [waseefakhtar/dose-android](https://github.com/waseefakhtar/dose-android) | 635 | **MIT** | Compose + MVVM + Clean Architecture ile hatırlatma iskeleti |
| [wisnukurniawan/Compose-ToDo](https://github.com/wisnukurniawan/Compose-ToDo) | 447 | **Apache-2.0** | Modüler Compose to-do mimarisi, Room + DataStore |
| [vishal2376/snaptick](https://github.com/vishal2376/snaptick) | 776 | GPL-3.0 | Widget, kaydırma aksiyonları, takvim görünümü, yedekleme |
| [Super12138/VerveDo](https://github.com/Super12138/VerveDo) | 160 | GPL-3.0 | Material 3 Expressive bileşen/animasyon örnekleri |
| [naz013/reminder-kotlin](https://github.com/naz013/reminder-kotlin) | 16 | Apache-2.0 | Geniş özellik listesi (akıllı listeler: Bugün/Gecikmiş/Bu hafta) |
| [HayaiApp/HayaiTTS](https://github.com/HayaiApp/HayaiTTS) | 95 | — | Cihazda Türkçe ses yoksa çevrimdışı nöral TTS alternatifi (ileride) |

> **Lisans kararı (19 Kasım 2026):** Uygulama açık kaynak (public) olacak, bu yüzden GPL-3.0 depolardan kod doğrudan alınabilir. KAİROS'un lisansı **GPL-3.0** olur. Alınan her dosyanın başına kaynak deposu ve orijinal telif satırı yazılır, ayrıca `NOTICE.md` dosyasında listelenir.

## 2.1 Hangi depodan hangi kodu alacağız

| Alınacak dosya | Kaynak | KAİROS'ta karşılığı | Ne yapılacak |
|---|---|---|---|
| `service/BootReceiver.kt` | reReminder | `alarm/RescheduleReceiver.kt` | Neredeyse aynen alınır. Telefon yeniden açılınca, kilit açılmadan önce, uygulama güncellenince, saat ya da saat dilimi değişince ve alarm izni geri verilince alarmları yeniden kurar. Telefon kapalıyken kaçırılan hatırlatma için sessiz bir "kaçırıldı" bildirimi gösterir. |
| `service/ReminderScheduler.kt` | reReminder | `alarm/AlarmScheduler.kt` | Yapısı alınır: her hatırlatmaya bir alarm düşer, tekrar tekrar çalıştırmak aynı sonucu verir, erteleme ayrı tutulur. Aralıklı tekrar mantığı çıkarılır; bizde tarih ve saat sabittir. |
| `service/AlertSoundService.kt` + `utils/SoundChannels.kt` | reReminder | `speech/DailyBriefService.kt` | **Sesli özetin kalbi.** `systemExempted` tipinde kısa bir ön plan servisi açılır, Türkçe sesle okutulur, sonra kapanır. Bildirim / medya / alarm ses kanalı seçimi buradan gelir. |
| `utils/Reliability.kt` | reReminder | `reliability/Reliability.kt` | Aynen alınır. Telefon markasını tanır (Xiaomi, Samsung vb.), alarm ve pil izinlerini denetler, dontkillmyapp.com rehberine yönlendirir. |
| `ui/setup/SetupScreen.kt` | reReminder | `onboarding/` | İzin isteme akışının iskeleti. Bizde ayrıca ilk açılışta "Günlük sesli özet ister misiniz?" diye sorulur, varsayılan cevap **hayır**. |
| `data/Recurrence.kt` | Remember | `data/Recurrence.kt` | Aynen alınır: günlük / haftalık (gün seçimli) / aylık (ayın 15'i veya son cuması) / yıllık; bitiş tarihi veya tekrar sayısı. Varsayılan değer **tekrar yok**. |
| `reminders/ReminderPresets.kt` | Remember | `snooze/SnoozePresets.kt` | Erteleme seçenekleri: birazdan, 1 saat, bu akşam, yarın sabah. Türkçeleştirilir. |
| `reminders/ActionReceiver.kt` | Remember | `notification/ActionReceiver.kt` | Bildirimdeki "Tamamlandı / Ertele" düğmelerinin işleyicisi. |
| `reminders/CriticalReminderRepeat.kt` | Remember | `notification/PersistentRepeat.kt` | Israrcı mod. Varsayılan olarak kapalı; görev başına veya ayarlardan açılır. |
| `ui/onboarding/OnboardingPermissionsScreen.kt` | Remember | `onboarding/` | Pil optimizasyonu ve alarm izni ekranları için ikinci referans. |
| `VoiceOutputAssistant.java` | Tasks.org | — | Sadece referans: TTS'in başlatılma ve kapatılma sırası. |
| `preferences_notifications.xml`, `preferences_date_and_time.xml` | Tasks.org | `settings/` | Ayar listesi ve "Sabah / Öğle / Akşam / Gece" saatlerinin düzenlenebilir olması. |
| triggerx kütüphanesi | meticha/triggerx | Gradle bağımlılığı | "Önemli" görevlerde kilit ekranında tam ekran alarm. |

**Almayacağımız:** reReminder'ın aralıklı tekrar sistemi (her 20 dakikada bir vb.), Remember'ın not ve Markdown editörü, hesap ve senkronizasyon kodları. KAİROS sadece hatırlatıcıdır.

## 3. Profesyonel uygulamalarda neler var
- **Todoist:** Saatli her göreve otomatik "varsayılan hatırlatma" (örn. 30 dk önce), bildirim kanalı seçimi, konum hatırlatması, doğal dille tarih ("yarın 12'de").
- **Any.do "My Day":** Her sabah gün planlama ekranı; dünden kalanları gösterir; sabah bildirim saati ayarlardan seçilir — bizim "günlük sesli özet" fikrimizin en yakın karşılığı.
- **Due (yalnızca iOS):** "Auto snooze" — tamamlanana kadar 1/5/10/15/30/60 dk'da bir tekrar eden ısrarcı hatırlatma; bildirimde 3 erteleme seçeneği.
- **TickTick:** Görev başına birden çok hatırlatma, takvim senkronu, alışkanlıklar.
- **Tasks.org:** Ayarlanabilir gün dilimi saatleri, sessiz saatler, sesli hatırlatma, bildirimleri gruplama, rozet sayısı.

## 4. Önerilen ürün tasarımı

### 4.1 Temel akış
1. **Hızlı ekleme sayfası (alt sayfa / bottom sheet):** Tek metin alanı + altında tarih çipleri `Bugün · Yarın · Hafta sonu · Tarih seç` ve saat çipi.
2. **Akıllı varsayılan saat:** Son seçilen saat hatırlanır (senin fikrin). Ek kural: tarih "Bugün" ise ve o saat geçmişse, bir sonraki tam saat önerilir (geçmişe hatırlatma kurulamaz).
3. **Doğal dil (Türkçe):** "yarın 12", "cuma akşam", "3 gün sonra 9:30", "her pazartesi 8" yazınca tarih/saat çiplere otomatik dolar; yazılan zaman metinden silinir. Türkçe için hazır kütüphane yok → kendi küçük ayrıştırıcımızı yazacağız.
4. **Kaydet** — klavyeden "Enter" ile de. Kayıt sonrası sayfa açık kalabilir (art arda ekleme modu).

### 4.0 Temel ilke: hiçbir şey dayatılmaz
- Günlük sesli özet, tekrarlayan hatırlatma, ısrarcı mod, önceden hatırlatma: **hepsi varsayılan olarak kapalı.** Kullanıcı isterse açar.
- İlk açılışta tek bir soru sorulur: "Her sabah size bugünkü hatırlatma sayısını sesli söyleyelim mi?" Cevap "Hayır" ise bir daha sorulmaz.
- Uygulama kendiliğinden "hatırlatmanız var" demez. Sadece kullanıcının kurduğu hatırlatmalar ve açtığı özet çalışır.
- Ayarlar ekranı dolu dolu olur; her davranışın bir anahtarı vardır.

### 4.2 Günlük sesli özet (opsiyonel)
- **Varsayılan kapalı.** Açılırsa saat 08:00 önerilir, ayarlardan değişir. Hafta içi/hafta sonu ayrı saat seçeneği.
- Okunan metin: _"Günaydın. Bugün 4 hatırlatmanız var."_ İçerik **asla** okunmaz.
- Ek bilgi (varsayılan kapalı): _"…ayrıca dünden kalan 1 hatırlatmanız var."_
- Hiç hatırlatma yoksa: varsayılan **sessiz kal**; ayarla "Bugün hatırlatmanız yok" dedirtilebilir.
- Telefon sessiz/rahatsız etmeyin modundaysa: varsayılan **konuşma, sadece bildirim göster**; ayarla değiştirilebilir.
- Ses kanalı seçimi: Bildirim / Medya / Alarm sesi (reReminder yaklaşımı).
- Konuşmadan önce kısa bir "ding" sesi; Türkçe ses paketi cihazda yoksa kullanıcıyı kurulum ekranına yönlendir.
- Bildirime dokununca "Bugün" ekranı açılır.
- Opsiyonel **akşam önizlemesi:** "Yarın 3 hatırlatmanız var."

### 4.3 Görev hatırlatmaları
- Bildirim aksiyonları: **Tamamlandı · 10 dk ertele · Ertele…** (birazdan / 1 saat / bu akşam / yarın sabah).
- **Israrcı mod (Due tarzı):** Tamamlanana kadar X dakikada bir tekrar. **Varsayılan kapalı**; görev başına veya ayarlardan açılır.
- **Önemli** işaretli görev: kilit ekranında tam ekran alarm, sessizde bile çalabilir. Varsayılan önem "Normal".
- Tekrarlayan: günlük / hafta içi / haftalık (gün seçimi) / aylık / yıllık / özel. **Varsayılan "Tekrar yok"**; sadece kullanıcı seçerse tekrarlar.
- Görev başına birden fazla hatırlatma (örn. 1 gün önce + tam saatinde) — v2.

### 4.4 Ekranlar
- **Bugün** (ana ekran): Gecikmiş (üstte, vurgulu) → Bugün → saat sırasıyla.
- **Yakında:** Yarın / Bu hafta / Daha sonra grupları.
- **Tamamlananlar** (geri alınabilir, 30 gün sonra otomatik temizlik).
- Kaydırma: sağa = tamamla, sola = ertele (ayarlanabilir); silmede "Geri al" snackbar'ı.
- **Ana ekran widget'ı** (bugünün listesi + hızlı ekle butonu) ve **Quick Settings kutucuğu** (tek dokunuşla ekle).

### 4.5 Ayarlar ekranı
**Günlük özet**
- Aç/Kapa · Saat · Hafta sonu farklı saat · Gecikmişleri de say · Hiç yoksa söyle · Sessiz modda konuş · Ses kanalı · Konuşma hızı · Test et ("Şimdi dinle" butonu)

**Hatırlatmalar**
- Varsayılan saat davranışı: _Son kullanılan_ / _Sabit saat_
- Gün dilimi kısayolları: Sabah 09:00 · Öğle 12:00 · Akşam 19:00 · Gece 22:00 (düzenlenebilir)
- Varsayılan erteleme süresi · Israrcı mod varsayılanı ve aralığı
- Bildirim sesi / titreşim (Android bildirim kanalı ayarlarına kısayol)
- Sessiz saatler (örn. 23:00–07:00 arası bildirimleri biriktir)

**Görünüm**
- Tema: Banker / Lacivert / Bordo / Taş / Grafit · Açık / Koyu / Telefonla aynı · Haftanın ilk günü · 24 saat / 12 saat

**Güvenilirlik (kritik)**
- İzin durumu kartı: Bildirim izni ✓ · Kesin alarm izni ✓ · Pil optimizasyonu muafiyeti ✓ — eksik olan tek dokunuşla düzeltilir
- Üreticiye özel rehber (Xiaomi, Samsung, Oppo vb. arka planda uygulama öldürür)

**Veri**
- Yedekle / Geri yükle (JSON dosyası) · Otomatik yedek

## 5. Teknik sistem önerisi

| Konu | Öneri | Neden |
|---|---|---|
| Dil / UI | **Kotlin + Jetpack Compose (native)** | Alarm, TTS, widget, Quick Settings, bildirim aksiyonları gibi sistem özellikleri native'de en güvenilir; Flutter'da hepsi eklenti + yerel köprü ister |
| Mimari | MVVM + tek yönlü veri akışı, Hilt | Test edilebilir, sade |
| Veritabanı | Room (SQLite) | Çevrimdışı, hızlı, sorgulanabilir |
| Ayarlar | DataStore | "Son kullanılan saat" vb. |
| Zamanlama | `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` izni | Doze'da bile tam saatinde; WorkManager kesin saat garantisi vermez |
| Yeniden kurma | `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, izin değişti yayını | Telefon kapanıp açılınca / saat değişince alarmlar kaybolmasın |
| Sesli okuma | Android `TextToSpeech` (tr-TR), `systemExempted` tipinde kısa ön plan servisi, geçici audio focus (müziği kısar) | Kesin alarmlar ön plan servisi başlatma kısıtlamasından muaf; reReminder bu yöntemi kullanıyor |
| Tam ekran alarm | `USE_FULL_SCREEN_INTENT` (sadece "önemli" görevler) | Kilit ekranında görünür |
| Widget | Jetpack Glance | Compose ile aynı mantık |
| Min SDK | 26 (Android 8) | Bildirim kanalları; cihazların büyük kısmını kapsar |

**Önemli Android kuralları**
- Android 14+'da `SCHEDULE_EXACT_ALARM` **varsayılan olarak kapalı**; kullanıcıdan istenmeli, izin kaldırılırsa tüm kesin alarmlar iptal olur → onboarding'de ve ayarlarda kontrol şart.
- `USE_EXACT_ALARM` otomatik verilir ama Google Play bunu alarm saati/takvim uygulamalarıyla sınırlıyor; hatırlatıcı için güvenli yol `SCHEDULE_EXACT_ALARM`.
- Android 13+'da bildirim izni (`POST_NOTIFICATIONS`) çalışma anında istenmeli.

## 6. Tasarım yönü — "AI varsayılan renkleri yok"
**Yasak:** Mor/indigo gradyanlar, Material varsayılan moru (#6750A4), mavi-mor "AI" degradeleri, her yerde aynı yuvarlak kart + gölge kalıbı.

**Not:** İlk taslaktaki "kemik beyazı + kiremit turuncusu" önerisi, yapay zekânın en sık ürettiği kalıplardan biri olduğu için iptal edildi.

**Onaylanan kimlik:** Masa başı nesnelerinden türetildi: banker lambası, pirinç saat kadranı, deri ajanda, takım elbise. Taslak tuvali: [KAİROS Tasarım Yönü](https://claude.ai/artifact/6gy8Qg6Sw9YPwtnnrKcB5s)

**Temalar** (kullanıcı ayarlardan seçer; her temanın açık ve koyu hâli var; varsayılan Banker):

| Tema | Zemin | Metin | Ana renk | Vurgu | Gecikmiş |
|---|---|---|---|---|---|
| Banker | #EEF0EC | #17201B | #1F4A38 | #9A7536 | #7B2D2B |
| Lacivert | #E8EBEF | #121A26 | #1C2C44 | #8A5532 | #8E2F2A |
| Bordo | #EFECEB | #1E1716 | #5C1E22 | #5F6B4E | #A0522D |
| Taş | #F4F4F2 | #0E0E0D | #0E0E0D | #4D4D4A | #6E2B26 |
| Grafit (koyu) | #141614 | #E7E8E3 | #C9A35E | #C9A35E | #D98579 |

**Yazı tipleri:** Saatler ve rakamlar için Barlow Semi Condensed, metinler için Schibsted Grotesk. İkisi de Türkçe karakterleri tam destekler ve uygulamaya gömülür.

**İmza unsuru:** Kol saati kadranı gibi işaretli bir **saat cetveli**. Kaydırarak saat seçilir, pirinç ibre seçili anı gösterir.

**Kurallar:** Düz renk alanları, ince çizgiler, 10–12 px köşe yuvarlaklığı. Material You dinamik renk varsayılan olarak kapalı.

## 7. Yol haritası
**v1 (MVP):** Hızlı ekleme + son kullanılan saat + tarih/saat çipleri · Bugün/Yakında/Tamamlanan ekranları · Kesin alarmlı bildirim (Tamamlandı/Ertele aksiyonları) · Günlük sesli özet + ayarları · Tekrarlayan görevler · İzin/güvenilirlik onboarding'i · Açık/koyu tema · Yedekle/geri yükle

**v1.1:** Türkçe doğal dil ayrıştırıcı · Israrcı mod · Widget · Quick Settings kutucuğu · Akşam önizlemesi · Sessiz saatler

**v2:** Görev başına çoklu hatırlatma · Önemli görevler için tam ekran alarm · Sesle görev ekleme · Kategoriler/etiketler · Konum hatırlatması · Wear OS
