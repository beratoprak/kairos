package app.kairos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /** Açık (tamamlanmamış) tüm hatırlatmalar, zamana göre sıralı. Bugün ve Yakında ekranları. */
    @Query("SELECT * FROM reminders WHERE completed_at IS NULL ORDER BY due_epoch_day, due_minute, id")
    fun observeOpen(): Flow<List<Reminder>>

    /** Tamamlananlar, en yeni üstte. */
    @Query("SELECT * FROM reminders WHERE completed_at IS NOT NULL ORDER BY completed_at DESC")
    fun observeCompleted(): Flow<List<Reminder>>

    /** Alarm kurmak için: açık olan her şey. */
    @Query("SELECT * FROM reminders WHERE completed_at IS NULL")
    suspend fun getOpen(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    /** Günlük sesli özet: o gün için açık hatırlatma sayısı. */
    @Query("SELECT COUNT(*) FROM reminders WHERE completed_at IS NULL AND due_epoch_day = :epochDay")
    suspend fun countOpenOn(epochDay: Long): Int

    /** Günlük sesli özet (opsiyonel): önceki günlerden kalan açık hatırlatmalar. */
    @Query("SELECT COUNT(*) FROM reminders WHERE completed_at IS NULL AND due_epoch_day < :epochDay")
    suspend fun countOpenBefore(epochDay: Long): Int

    @Query("UPDATE reminders SET last_notified_at = :atMillis WHERE id = :id")
    suspend fun markNotified(id: Long, atMillis: Long)

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    /** Geri al: silinen ya da tamamlanan kaydı önceki hâline döndürür. */
    @Upsert
    suspend fun upsert(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    /** Ayarlarda "tamamlananları X gün sonra temizle" seçilirse. Varsayılan: hiç temizleme. */
    @Query("DELETE FROM reminders WHERE completed_at IS NOT NULL AND completed_at < :beforeMillis")
    suspend fun deleteCompletedBefore(beforeMillis: Long): Int

    @Query("SELECT * FROM reminders ORDER BY id")
    suspend fun getAll(): List<Reminder>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)

    /** Yedekten geri yükleme: hepsi tek işlemde değişir; yarıda kalırsa hiçbir şey değişmez. */
    @Transaction
    suspend fun replaceAll(reminders: List<Reminder>) {
        deleteAll()
        insertAll(reminders)
    }
}
