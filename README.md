# Kairos

Android için sade, çevrimdışı çalışan bir hatırlatıcı.

- Hatırlatma ekle: saati kadran cetveliyle seç; son seçtiğin saat bir sonraki eklemede hazır gelir.
- Bugün, Yakında ve Tamamlanan ekranları; kaydırarak tamamla ya da ertele, her işlemi geri al.
- Tam saatinde bildirim; bildirimden Tamamlandı / Ertele.
- İsteğe bağlı günlük sesli özet: “Bugün 4 hatırlatmanız var.” (içerik okunmaz)
- Beş tema (açık ve koyu), dolu dolu ayarlar, yedekleme ve geri yükleme.
- Hesap yok, reklam yok, izleme yok. Hatırlatmalar sadece telefonda durur.

## Kurulum

1. [Releases](../../releases/latest) sayfasından en son `Kairos-x.y.z.apk` dosyasını telefona indir.
2. Dosyaya dokun. Telefon “bilinmeyen kaynaklardan yükleme” izni isterse ver.
3. Google Play Protect “uygulama taranmadı” diyebilir: **Yine de yükle**.

Sonraki sürümler uygulamanın içinden gelir: Bugün ekranında **Yeni sürüm hazır · Güncelle**.
Android, güncellemenin aynı anahtarla imzalandığını denetler; başka bir imzayla gelen dosya kurulamaz.

İmza sertifikası (SHA-256):
`4D:76:FE:12:CA:84:8E:09:02:89:C8:67:55:9E:BA:F7:A1:AE:0F:85:93:3B:D4:BA:B2:63:40:6C:A8:11:C8:42`

### Xiaomi, Oppo, Vivo, Huawei telefonlar

Bu telefonlar uygulamaları arka planda durdurabilir; bildirim ancak uygulama açılınca gelir.
Kairos > Ayarlar > **Güvenilirlik** bölümünden “Alarmlar ve hatırlatıcılar” iznini ve
“otomatik başlatma”yı açın. Ardından Ayarlar > Bildirimler > **Deneme bildirimi** ile kontrol edin.

## Gizlilik

Uygulama internete sadece yeni sürüm olup olmadığını öğrenmek ve güncellemeyi indirmek için bağlanır
(GitHub Releases). Hatırlatmalar, ayarlar ya da kişisel bilgiler hiçbir yere gönderilmez.
Açılıştaki denetim Ayarlar > Güncellemeler’den kapatılabilir.

## Geliştirme

Kotlin + Jetpack Compose, minSdk 26, targetSdk 36.

```bash
./gradlew testDebugUnitTest   # testler
./gradlew assembleDebug       # geliştirme APK'sı
```

Derleme çıktıları `~/.kairos-build/` altına yazılır. Proje kuralları `KURALLAR.md`, plan `PLAN.md`,
ilerleme `ILERLEME.md` dosyalarındadır.

## Lisans

GPL-3.0 — bkz. `LICENSE`. Kairos, [reReminder](https://github.com/ProfessorQuantumUniverse/reReminder),
[Remember](https://github.com/bikram-agarwal/Remember) ve [Tasks.org](https://github.com/tasks/tasks)
projelerinden uyarlanmış kodlar içerir; ayrıntılar `NOTICE.md` dosyasında.
Yazı tipleri (Barlow Semi Condensed, Schibsted Grotesk) SIL Open Font License 1.1 ile lisanslıdır.
