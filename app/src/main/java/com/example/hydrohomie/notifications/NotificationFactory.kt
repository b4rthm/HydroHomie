package com.example.hydrohomie.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.hydrohomie.R
import com.example.hydrohomie.domain.ReminderSlot
import kotlin.random.Random

/**
 * Creates the reminders that are shown to the user.
 */
class NotificationFactory(private val context: Context) {

    /** Creates the notification channel, has to be called once at startup. */
    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_description)
            enableVibration(true)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the notification for a reminder.
     *
     * @param slot the reminder the notification is for.
     * @param nextSlot the reminder that follows, shown as a hint.
     */
    fun buildNotification(slot: ReminderSlot, nextSlot: ReminderSlot?): Notification {
        val title = context.resources.getStringArray(R.array.motivation_titles).randomOrNull()
            ?: context.getString(R.string.app_name)
        val text = context.getString(R.string.notification_drink_amount, slot.amountMl)
        val subText = if (nextSlot != null) {
            context.getString(
                R.string.notification_next_at,
                formatTime(nextSlot.timestampMillis),
            )
        } else {
            context.getString(R.string.notification_last_of_the_day)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setColor(notificationColor())
            .setColorized(true)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(drinkPendingIntent(slot))
            .setSound(alarmSound())
            .build()
    }

    /** The intent that is fired when the user taps the notification. */
    fun drinkPendingIntent(slot: ReminderSlot): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DRINK
            putExtra(ReminderReceiver.EXTRA_AMOUNT_ML, slot.amountMl)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, slot.id)
            putExtra(ReminderReceiver.EXTRA_TIMESTAMP, slot.timestampMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** The intent that shows the notification when the alarm fires. */
    fun showPendingIntent(slot: ReminderSlot): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
            putExtra(ReminderReceiver.EXTRA_AMOUNT_ML, slot.amountMl)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, slot.id)
            putExtra(ReminderReceiver.EXTRA_TIMESTAMP, slot.timestampMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmSound(): Uri {
        val sounds = intArrayOf(R.raw.sip1, R.raw.fill_cup1, R.raw.fill_cup2)
        val sound = sounds[Random.nextInt(sounds.size)]
        return Uri.parse("android.resource://${context.packageName}/$sound")
    }

    private fun notificationColor(): Int =
        context.getColor(R.color.notification_color)

    private fun formatTime(timestampMillis: Long): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestampMillis))
    }

    companion object {
        const val CHANNEL_ID = "hydrohomie_reminders"
    }
}

/** Schedules the alarms of the reminders. */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /** Whether exact alarms may be scheduled (Android 13+ asks the user for permission). */
    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Schedules the reminder, exactly if possible. */
    fun schedule(slot: ReminderSlot, pendingIntent: PendingIntent) {
        val manager = alarmManager ?: return
        if (canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                slot.timestampMillis,
                pendingIntent,
            )
        } else {
            // Without the permission for exact alarms the reminder is shown in a small
            // time window instead of at an exact moment.
            manager.setWindow(
                AlarmManager.RTC_WAKEUP,
                slot.timestampMillis,
                INEXACT_WINDOW_MILLIS,
                pendingIntent,
            )
        }
    }

    /** Cancels the reminder with the given id. */
    fun cancel(id: Int) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.cancel(pendingIntent)
    }

    private companion object {
        const val INEXACT_WINDOW_MILLIS = 5 * 60 * 1000L
    }
}
