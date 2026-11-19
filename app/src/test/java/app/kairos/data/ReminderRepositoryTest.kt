package app.kairos.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Gerçek Room veritabanıyla (bellekte) repository davranışı. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderRepositoryTest {
    private lateinit var db: KairosDatabase
    private lateinit var repo: ReminderRepository
    private val clock = Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneId.of("Europe/Istanbul"))
    private val today = LocalDate.parse("2026-11-19")

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KairosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ReminderRepository(db.reminders(), clock)
    }

    @After fun tearDown() = db.close()

    @Test fun newReminderHasEverythingOptionalOff() = runTest {
        val r = repo.add("  Sözleşmeyi gönder  ", today, 720)
        assertEquals("Sözleşmeyi gönder", r.title)
        assertNull(r.recurrence)
        assertNull(r.leadMinutes)
        assertEquals(false, r.important)
        assertEquals(false, r.persistent)
        assertEquals(listOf(r), repo.open.first())
    }

    @Test fun completingOneOffMovesToCompletedAndUndoRestores() = runTest {
        val r = repo.add("Ara", today, 600)
        val previous = repo.complete(r.id)
        assertNotNull(previous)
        assertTrue(repo.open.first().isEmpty())
        assertEquals(1, repo.completed.first().size)

        repo.restore(previous!!)
        assertEquals(listOf(r), repo.open.first())
        assertTrue(repo.completed.first().isEmpty())
    }

    @Test fun completingRecurringAdvancesToNextDate() = runTest {
        val r = repo.add("İlaç", today, 1110, recurrence = RecurrenceRule(RecurrenceUnit.WEEK))
        repo.complete(r.id)
        val moved = repo.get(r.id)!!
        assertNull(moved.completedAt)
        assertEquals(today.plusWeeks(1), moved.dueDate)
        assertEquals(1110, moved.dueMinute)
    }

    @Test fun recurringWithLastCountCompletesForGood() = runTest {
        val r = repo.add(
            "Son", today, 600,
            recurrence = RecurrenceRule(RecurrenceUnit.DAY, endKind = RecurrenceEndKind.AFTER_COUNT, endCount = 1),
        )
        repo.complete(r.id) // son tekrar tüketildi → yarına taşınır, sayaç 0
        repo.complete(r.id) // artık sonraki tarih yok → tamamlanır
        assertNotNull(repo.get(r.id)!!.completedAt)
    }

    @Test fun deleteAndUndo() = runTest {
        val r = repo.add("Sil", today, 600)
        val deleted = repo.delete(r.id)!!
        assertNull(repo.get(r.id))
        repo.restore(deleted)
        assertEquals(r, repo.get(r.id))
    }

    @Test fun changingTimeClearsSnooze() = runTest {
        val r = repo.add("Ertele", today, 600)
        repo.snooze(r.id, untilMillis = 123_456)
        val snoozed = repo.get(r.id)!!
        repo.update(snoozed.copy(dueMinute = 660))
        assertNull(repo.get(r.id)!!.snoozedUntil)
    }

    @Test fun briefCounts() = runTest {
        repo.add("a", today, 600)
        repo.add("b", today, 700)
        repo.add("dün", today.minusDays(1), 600)
        val done = repo.add("c", today, 800)
        repo.complete(done.id)
        assertEquals(2, repo.countForBrief(today))
        assertEquals(1, repo.countOverdueBefore(today))
    }
}
