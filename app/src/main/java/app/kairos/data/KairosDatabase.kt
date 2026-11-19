package app.kairos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Reminder::class], version = 1, exportSchema = true)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun reminders(): ReminderDao

    companion object {
        /**
         * Veritabanı Android'in normal (kullanıcı şifresiyle korunan) alanındadır (KURALLAR §3.5):
         * telefon yeniden başladıktan sonra şifre ilk kez girilene kadar hiçbir uygulama okuyamaz.
         * Bedeli: o arada zamanı gelen hatırlatma, kilit açılınca "Kaçırıldı" olarak bildirilir.
         * Tek örneği `AppGraph` tutar; doğrudan çağırmayın.
         */
        fun create(context: Context): KairosDatabase =
            Room.databaseBuilder(context.applicationContext, KairosDatabase::class.java, NAME).build()

        private const val NAME = "kairos.db"
    }
}
