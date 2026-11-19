# Alınan kaynak kodlar

Tüm kaynaklar GPL-3.0 lisanslıdır; KAİROS da GPL-3.0'dır. Bir dosya uygulamaya taşındığında başına şu biçimde not eklenir:

```
// Bu dosya <depo> (<commit>) projesindeki <yol> dosyasından uyarlanmıştır.
// Orijinal: Copyright (C) <yazar> — GPL-3.0
```

| Depo | Commit | Klasör |
|---|---|---|
| github.com/ProfessorQuantumUniverse/reReminder | a963087 | `secilenler/reReminder/` |
| github.com/bikram-agarwal/Remember | 80d099c | `secilenler/Remember/` |
| github.com/tasks/tasks | main (2026-11-19) | `secilenler/tasks/` |

## Dosya → KAİROS karşılığı

| Dosya | KAİROS'ta | Plan adımı |
|---|---|---|
| reReminder/ReminderScheduler.kt | `alarm/AlarmScheduler.kt` | 4 |
| reReminder/BootReceiver.kt | `alarm/RescheduleReceiver.kt` | 4 |
| reReminder/AlarmReceiver.kt | `alarm/ReminderReceiver.kt` (referans) | 4 |
| reReminder/NotificationHelper.kt | `notification/Notifications.kt` | 4 |
| Remember/ActionReceiver.kt | `notification/ActionReceiver.kt` | 4 |
| Remember/ReminderPresets.kt | `snooze/SnoozePresets.kt` | 4 |
| Remember/Recurrence.kt | `data/Recurrence.kt` | 3 |
| Remember/BootReceiver.kt | referans | 4 |
| reReminder/AlertSoundService.kt, SoundChannels.kt, AlertPlayer.kt | `speech/DailyBriefService.kt` | 8 |
| tasks/VoiceOutputAssistant.java | referans | 8 |
| reReminder/Reliability.kt | `reliability/Reliability.kt` | 9 |
| reReminder/SetupScreen.kt, SetupState.kt | `onboarding/` | 9 |
| Remember/OnboardingPermissionsScreen.kt | referans | 9 |
| tasks/preferences_*.xml | `settings/` (referans) | 7 |
| Remember/CriticalReminderRepeat.kt | `notification/PersistentRepeat.kt` | 1.1 |
| reReminder/AndroidManifest.xml | izin ve receiver tanımları (referans) | 4, 8 |

`reReminder/` ve `Remember/` klasörlerinin tamamı sadece okuma amaçlı yerel kopyadır; git'e eklenmez.
