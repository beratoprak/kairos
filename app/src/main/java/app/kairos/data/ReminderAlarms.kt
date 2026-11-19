package app.kairos.data

/**
 * Repository her değişiklikten sonra alarmları buradan günceller. Android tarafındaki uygulaması
 * `alarm/AlarmScheduler`; testlerde [None] kullanılır.
 */
interface ReminderAlarms {
    fun onChanged(reminder: Reminder)
    fun onRemoved(reminderId: Long)

    object None : ReminderAlarms {
        override fun onChanged(reminder: Reminder) = Unit
        override fun onRemoved(reminderId: Long) = Unit
    }
}
