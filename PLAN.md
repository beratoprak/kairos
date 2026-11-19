# KAİROS — Uygulama Planı

Kurallar: `KURALLAR.md` · İlerleme: `ILERLEME.md` · Araştırma: `ARASTIRMA.md`

Durum işaretleri: `[ ]` bekliyor · `[~]` devam ediyor · `[x]` bitti

## Sürüm 1 (MVP)

### Adım 0 — Belgeler ve kaynak kodlar
- [x] Araştırma raporu
- [x] Kurallar, plan, ilerleme dosyaları
- [x] Kaynak depoları indir; alınacak dosyaları `kaynak/` altına ayır

### Adım 1 — Proje iskeleti
- [x] Gradle (Kotlin DSL, version catalog), AGP 8.11, Kotlin 2.1.20, Compose
- [x] Paket `app.kairos`, minSdk 26, targetSdk 36
- [x] `LICENSE` (GPL-3.0), `NOTICE.md`, `.gitignore`, git deposu
- [x] Boş uygulama derleniyor (`assembleDebug`)

### Adım 2 — Tema sistemi
- [x] 5 tema × açık/koyu renk tabloları (KURALLAR §2)
- [x] Gömülü yazı tipleri (Barlow Semi Condensed, Schibsted Grotesk)
- [x] `KairosTheme`, tema seçimi DataStore'da
- [x] Kontrast + mor yasağı testleri, 10 tema ekran görüntüsü (Roborazzi)

### Adım 3 — Veri katmanı
- [x] Room: `Reminder` tablosu, DAO, repository
- [x] `Recurrence` (Remember'dan), varsayılan "Tekrar yok"
- [x] Ayarlar DataStore (tüm opsiyonel davranışlar varsayılan kapalı), son kullanılan saat
- [x] Akıllı varsayılan saat (`TimeSuggestion`), ekran grupları (`ReminderBuckets`)

### Adım 4 — Alarm ve bildirim altyapısı
- [x] `AlarmScheduler` (reReminder'dan uyarlanmış)
- [x] `RescheduleReceiver` (reReminder BootReceiver)
- [x] Bildirim kanalları, hatırlatma bildirimi, Tamamlandı / Ertele düğmeleri (Remember ActionReceiver)
- [x] Erteleme seçenekleri (Remember ReminderPresets, Türkçe)
- [x] Birim testleri: sonraki tetiklenme zamanı, tekrar hesapları, uçtan uca alarm akışı

### Adım 5 — Ana ekranlar
- [x] Bugün (Gecikmiş + saat sütunlu liste), Yakında, Tamamlanan
- [x] Alt menü, kaydırma ile tamamla/ertele, geri al

### Adım 6 — Hatırlatma ekleme
- [x] Alt sayfa: metin, gün çipleri, büyük saat
- [x] **Saat cetveli** (imza bileşeni)
- [x] Son kullanılan saat + geçmiş saat kuralı; gün dilimi kısayolları
- [x] Tekrar / Önceden hatırlat / Önem (hepsi varsayılan kapalı/normal)
- [x] Satıra dokununca düzenleme, düzenlemede Sil
- [ ] Özel tekrar ("Özel…": aralık + birim + günler) — hazır seçenekler var, özel ekran eksik

### Adım 7 — Ayarlar ekranı
- [x] Günlük sesli özet, Yeni hatırlatma, Bildirimler, Liste, Görünüm (tema), Hakkında bölümleri
- [ ] Güvenilirlik (Adım 9) ve Veri / Yedekleme (Adım 10) bölümleri — o adımlarda eklenecek

### Adım 8 — Günlük sesli özet
- [x] Zamanlayıcı + `DailyBriefService` (reReminder AlertSoundService'ten)
- [x] "Bugün N hatırlatmanız var", gecikmiş seçeneği, yoksa sessiz kal, sessiz modda sadece bildirim, ses kanalı, hız, "Dinle" düğmesi

### Adım 9 — İlk açılış ve güvenilirlik
- [x] Bildirim izni, kesin alarm izni, pil optimizasyonu (reReminder Reliability)
- [x] Sesli özet sorusu (varsayılan "Hayır")
- [x] Ayarlar'da izin durumu kartı, Bugün'de kapatılabilir uyarı şeridi

### Adım 10 — Yedekle / geri yükle, son kontroller
- [x] JSON dışa/içe aktarma (onaylı geri yükleme)
- [x] Tüm testler, lint, küçültülmüş release derlemesi
- [ ] **Gerçek cihaz/emülatör testi** — cihaz bağlanınca ya da emülatör kurulunca

### Sürüm 1 için kalan küçük işler
- [ ] "Özel…" tekrar ekranı (örn. 3 haftada bir salı + perşembe)
- [x] ~~Okumadan önce kısa "ding" sesi~~ — yapılmayacak (proje sahibi kararı)
- [x] ~~Pil için tek dokunuşlu izin penceresi~~ — yapılmayacak (proje sahibi kararı)
- [x] Hatırlatmaların saklandığı yer → normal, şifreyle korunan alana taşındı (proje sahibi kararı)
- [ ] Kütüphanelerin güncel sürümlere toplu yükseltilmesi
- [ ] Yayın imzalama anahtarı ve Play Store hazırlığı

## Sürüm 1.1
- [ ] Türkçe doğal dil ayrıştırıcı ("yarın 12", "cuma akşam")
- [ ] Israrcı mod (Remember CriticalReminderRepeat)
- [ ] Ana ekran widget'ı (Glance), Quick Settings kutucuğu
- [ ] Akşam önizlemesi, sessiz saatler

## Sürüm 2
- [ ] Görev başına çoklu hatırlatma, önemli görevlerde tam ekran alarm (triggerx)
- [ ] Sesle görev ekleme, etiketler, konum hatırlatması, Wear OS
