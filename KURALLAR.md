# KAİROS — Değişmez Kurallar

Bu dosyadaki kararlar proje sahibinin onayıyla alınmıştır. **Hiçbir adımda bu kuralların dışına çıkılmaz.**

## 0. Sapma protokolü
- Bir kuralı esnetmek, değiştirmek ya da kuralda olmayan büyük bir karar almak gerekirse: **çalışma durdurulur, proje sahibine ne değişeceği ve neden gerektiği sorulur.** Onay gelmeden uygulanmaz.
- Onaylanan her değişiklik bu dosyaya tarihiyle yazılır (en alttaki "Değişiklik geçmişi" bölümü).
- Sessizce "daha iyisi bu" diyerek kural değiştirmek yasaktır.

## 1. Ürün kuralları
1. **Kullanıcıya hiçbir şey dayatılmaz.** Her davranışın ayarlarda bir anahtarı vardır.
2. **Varsayılanlar sakin ve kapalıdır:** günlük sesli özet, tekrar, ısrarcı mod, önceden hatırlatma, gecikmişleri söyleme, tam ekran alarm → hepsi kapalı.
3. **Tekrarlayan hatırlatma sadece kullanıcı isterse.** Yeni hatırlatmada varsayılan "Tekrar yok".
4. **Günlük sesli özet opsiyoneldir.** İlk açılışta bir kez sorulur (varsayılan cevap "Hayır"), sonra sadece Ayarlar'dan açılıp kapatılır. Saat kullanıcı tarafından seçilir.
5. **Sesli özet içerik okumaz.** Sadece sayı söyler: "Bugün 4 hatırlatmanız var."
6. Uygulama kendiliğinden "hatırlatmanız var" diye rahatsız etmez; sadece kullanıcının kurduğu hatırlatmalar çalışır.
7. **Yeni hatırlatmada saat, son seçilen saattir.** Seçilen tarih bugünse ve o saat geçmişse bir sonraki tam saat önerilir. Kullanıcı dokunup değiştirebilir.
8. Ayarlar bölümü zengin ve bol seçeneklidir.
9. Hedef kitle yetişkin profesyoneller: dil ciddi, kısa, "siz" hitabıyla. Emoji, maskot, konfeti, "Harika iş!" gibi tezahürat yok.

## 2. Tasarım kuralları
1. **Gradyan ASLA yok.** Geçişli arka plan, parıltı, cam efekti yok. Sadece düz renk alanları.
2. **AI varsayılan renkleri ASLA yok.** Mor, indigo, mor-mavi tonlar, Material varsayılan moru (#6750A4) yasak.
3. **Uygulama "yapay zekâ yapmış gibi" hissettirmemeli.** Bu en önemli tasarım önceliğidir.
4. Kemik beyazı zemin + kiremit turuncusu kalıbı kullanılmaz (iptal edildi).
5. Material You dinamik renk varsayılan olarak **kapalı**.
6. Temalar (kullanıcı seçer, varsayılan **Banker**). Her temanın açık ve koyu hâli vardır. Seçenekler: Açık / Koyu / Telefonla aynı.

| Tema | Zemin | Metin | Ana renk | Vurgu | Gecikmiş |
|---|---|---|---|---|---|
| Banker | #EEF0EC | #17201B | #1F4A38 | #9A7536 | #7B2D2B |
| Lacivert | #E8EBEF | #121A26 | #1C2C44 | #8A5532 | #8E2F2A |
| Bordo | #EFECEB | #1E1716 | #5C1E22 | #5F6B4E | #A0522D |
| Taş | #F4F4F2 | #0E0E0D | #0E0E0D | #4D4D4A | #6E2B26 |
| Grafit | #141614 | #E7E8E3 | #C9A35E | #C9A35E | #D98579 |

   Açık ve koyu hâllerin tüm renk değerleri `app/src/main/java/app/kairos/ui/theme/Palettes.kt` dosyasındadır. Bir renk değişirse `PaletteRulesTest` geçmek zorundadır (metin kontrastı ≥ 4.5:1, ana metin ≥ 7:1, mor/indigo yok). Test kırılıyorsa renk kullanılamaz.
7. Yazı tipleri: **Barlow Semi Condensed** (saatler, rakamlar; tabular rakam), **Schibsted Grotesk** (tüm metinler). Uygulamaya gömülür.
8. İmza unsuru: **saat cetveli** (kadran bölmeli, kaydırmalı saat seçici, pirinç ibre). Uygulamanın tek gösterişli unsurudur; geri kalan her şey sadedir.
9. Köşe yuvarlaklığı 10–12 px. Gölge yerine ince çizgi (1 px). Dokunma alanları en az 44 px.
10. Taslak referansı: https://claude.ai/artifact/6gy8Qg6Sw9YPwtnnrKcB5s

## 3. Sistem kuralları
1. **Kotlin + Jetpack Compose**, native Android. Flutter/React Native yok.
2. minSdk 26, targetSdk/compileSdk 36. Paket adı `app.kairos`.
3. Mimari: MVVM + tek yönlü veri akışı. Veri: **Room**. Ayarlar: **DataStore**.
4. Zamanlama: `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` (kullanıcı izni). `USE_EXACT_ALARM` kullanılmaz (Play politikası). WorkManager kesin saat için kullanılmaz.
5. Veriler (hatırlatmalar ve ayarlar) Android'in **normal, kullanıcı şifresiyle korunan alanında** saklanır. Telefon yeniden başladıktan sonra şifre ilk kez girilene kadar hiçbir uygulama okuyamaz. "Cihaz korumalı / erken açılan" alan kullanılmaz.
   Yeniden kurma: BOOT_COMPLETED (kilit ilk kez açılınca), MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED, exact alarm izni değişimi. Kilit açılana kadar zamanı geçen hatırlatmalar tek seferlik, sessiz "Kaçırıldı" bildirimiyle gösterilir.
6. Sesli okuma: Android `TextToSpeech` (tr-TR), `systemExempted` tipinde kısa ön plan servisi.
7. Çevrimdışı çalışır. Hesap, sunucu, reklam, analitik/izleme yok.
8. Pil kısıtlaması için tek dokunuşlu izin penceresi (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) **kullanılmaz**; sadece uygulamanın ayar sayfasına yönlendirilir.
9. Sesli özetten önce "ding" vb. bir ses **çalınmaz**; sadece cümle okunur.
10. **Lisans GPL-3.0.** Başka depodan alınan her dosyanın başına kaynak ve orijinal telif yazılır; `NOTICE.md`'ye eklenir.

## 4. Çalışma kuralları
1. Plan `PLAN.md`'dedir; adımlar sırayla uygulanır.
2. **Her adım bitince `ILERLEME.md` güncellenir:** ne yapıldı, hangi dosyalar, neler doğrulandı, sıradaki adım.
3. Bir adım doğrulanmadan (derleme / test) "bitti" denmez. Doğrulanamayan şey açıkça yazılır.

## Değişiklik geçmişi
- 2026-11-19: Kurallar oluşturuldu (proje sahibi onayı).
- 2026-11-19: Tüm temaların kullanıcıya sunulması ve her temanın açık+koyu hâli onaylandı. Türetilen koyu hâller (ve Grafit'in açık hâli) `Palettes.kt`'ye yazıldı; proje sahibinin görsel onayı bekleniyor.
- 2026-11-19: Koyu tonlar onaylandı (proje sahibi "Onaylıyorum").
- 2026-11-19: Pil için tek dokunuşlu izin kullanılmayacak; sesli özetten önce ding sesi olmayacak (proje sahibi kararı, §3.8–3.9).
- 2026-11-19: Veriler "cihaz korumalı" alandan normal, şifreyle korunan alana taşındı; LOCKED_BOOT_COMPLETED kaldırıldı (proje sahibi kararı, §3.5). Bedeli: telefon gece yeniden başlarsa kilit açılana kadar hatırlatma çalmaz, sonra "Kaçırıldı" olarak gelir.
