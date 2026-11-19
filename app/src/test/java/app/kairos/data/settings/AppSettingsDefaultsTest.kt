package app.kairos.data.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.emptyPreferences
import app.kairos.ui.theme.ThemeFamily
import app.kairos.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** KURALLAR §1.2: kullanıcı açmadıkça hiçbir opsiyonel davranış çalışmaz. */
class AppSettingsDefaultsTest {

    private val fresh = SettingsStore.read(emptyPreferences())

    @Test fun dailyBriefIsOffByDefault() = assertFalse(fresh.brief.enabled)
    @Test fun overdueIsNotSpokenByDefault() = assertFalse(fresh.brief.includeOverdue)
    @Test fun briefStaysSilentWhenNothingDue() = assertEquals(WhenNone.STAY_SILENT, fresh.brief.whenNone)
    @Test fun briefDoesNotSpeakInSilentMode() = assertEquals(WhenSilentMode.NOTIFY_ONLY, fresh.brief.whenSilentMode)
    @Test fun persistentModeIsOffByDefault() = assertFalse(fresh.notifications.persistentByDefault)
    @Test fun noLeadReminderByDefault() = assertNull(fresh.newReminder.defaultLeadMinutes)
    @Test fun quietHoursOffByDefault() = assertFalse(fresh.notifications.quietHoursEnabled)
    @Test fun completedAreNeverPurgedByDefault() = assertNull(fresh.list.purgeCompletedAfterDays)
    @Test fun newReminderUsesLastUsedTime() = assertEquals(DefaultTimeMode.LAST_USED, fresh.newReminder.timeMode)
    @Test fun defaultThemeIsBankerFollowingPhone() {
        assertEquals(ThemeFamily.BANKER, fresh.appearance.family)
        assertEquals(ThemeMode.SYSTEM, fresh.appearance.mode)
    }

    @Test fun everySettingSurvivesWriteAndRead() {
        val custom = AppSettings(
            appearance = Appearance(ThemeFamily.BORDO, ThemeMode.DARK, use24Hour = false, firstDayOfWeek = java.time.DayOfWeek.SUNDAY),
            brief = DailyBrief(
                enabled = true, minute = 7 * 60 + 30, days = setOf(1, 2, 3, 4, 5), weekendDifferent = true,
                weekendMinute = 9 * 60, includeOverdue = true, whenNone = WhenNone.SAY_NONE,
                whenSilentMode = WhenSilentMode.SKIP, channel = AudioChannel.ALARM, speechRate = 1.25f,
            ),
            newReminder = NewReminder(
                timeMode = DefaultTimeMode.FIXED, fixedMinute = 600, lastUsedMinute = 720,
                dayParts = DayParts(480, 750, 1080, 1290), defaultLeadMinutes = 15,
            ),
            notifications = Notifications(5, true, 15, true, 22 * 60, 6 * 60),
            list = ListBehavior(SwipeAction.DELETE, SwipeAction.NONE, 30),
            onboarding = Onboarding(completed = true, briefQuestionAsked = true),
        )
        val prefs = mutablePreferencesOf()
        SettingsStore.write(prefs, custom)
        assertEquals(custom, SettingsStore.read(prefs))

        // Nullable alanlar tekrar "yok"a çekilebilmeli.
        val cleared = custom.copy(
            newReminder = custom.newReminder.copy(lastUsedMinute = null, defaultLeadMinutes = null),
            list = custom.list.copy(purgeCompletedAfterDays = null),
        )
        SettingsStore.write(prefs, cleared)
        assertEquals(cleared, SettingsStore.read(prefs))
    }
}
