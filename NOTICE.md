# KAİROS — Üçüncü taraf kodlar ve lisanslar

KAİROS, GNU General Public License v3.0 (GPL-3.0) ile lisanslanmıştır. Tam metin: `LICENSE`.

Bu projede aşağıdaki açık kaynak projelerden uyarlanmış kodlar bulunur. Uyarlanan her dosyanın başında kaynağı belirtilir.

| Proje | Lisans | Bağlantı |
|---|---|---|
| reReminder | GPL-3.0 | https://github.com/ProfessorQuantumUniverse/reReminder |
| Remember | GPL-3.0 | https://github.com/bikram-agarwal/Remember |
| Tasks.org | GPL-3.0 | https://github.com/tasks/tasks |

## Yazı tipleri
| Yazı tipi | Lisans | Telif | Lisans metni |
|---|---|---|---|
| Barlow Semi Condensed | SIL Open Font License 1.1 | Copyright 2017 The Barlow Project Authors | `licenses/OFL-Barlow.txt` |
| Schibsted Grotesk | SIL Open Font License 1.1 | Schibsted Grotesk Project Authors | `licenses/OFL-SchibstedGrotesk.txt` |

## Uyarlanan dosyalar
| KAİROS dosyası | Kaynak | Değişiklik |
|---|---|---|
| `app/src/main/java/app/kairos/data/Recurrence.kt` | Remember `data/Recurrence.kt` (80d099c) | Tarih tabanlı (LocalDate), saatlik tekrar çıkarıldı, ISO hafta günleri |
| `app/src/main/java/app/kairos/alarm/AlarmScheduler.kt` | reReminder `service/ReminderScheduler.kt` (a963087) | Aralıklı ızgara çıkarıldı; hatırlatma başına DUE/LEAD alarmı |
| `app/src/main/java/app/kairos/alarm/RescheduleReceiver.kt` | reReminder `service/BootReceiver.kt` (a963087) | Aynı hatırlatma iki kez "kaçırıldı" bildirilmez |
| `app/src/main/java/app/kairos/alarm/ReminderAlarmReceiver.kt` | reReminder `service/AlarmReceiver.kt` (a963087) | Ses/alarm stili çıkarıldı; sadece bildirim |
| `app/src/main/java/app/kairos/notification/Notifications.kt` | reReminder `utils/NotificationHelper.kt` (a963087) | Türkçe kanallar, sessiz saatler |
| `app/src/main/java/app/kairos/notification/ActionReceiver.kt` | Remember `reminders/ActionReceiver.kt` (80d099c) | Sadece Tamamlandı / Ertele |
| `app/src/main/java/app/kairos/snooze/SnoozePresets.kt` | Remember `reminders/ReminderPresets.kt` (80d099c) | Türkçe, kullanıcının gün dilimi saatleri |
| `app/src/main/java/app/kairos/speech/BriefSpeaker.kt` | reReminder `utils/AlertPlayer.kt` (a963087) | Sadece TTS; Türkçe ses kontrolü, 30 sn güvenlik süresi |
| `app/src/main/java/app/kairos/reliability/Reliability.kt` | reReminder `utils/Reliability.kt` (a963087) | Bildirim izni eklendi; pil için kısıtlı izin kullanılmıyor |
| `app/src/main/java/app/kairos/ui/onboarding/OnboardingScreen.kt` | reReminder `ui/setup/SetupScreen.kt` (a963087) | Esinlenme: tek ekran, hiçbir adım zorunlu değil |
| `app/src/main/java/app/kairos/speech/DailyBriefService.kt` | reReminder `service/AlertSoundService.kt` (a963087) | Hatırlatma sesi yerine günlük özet cümlesi |
