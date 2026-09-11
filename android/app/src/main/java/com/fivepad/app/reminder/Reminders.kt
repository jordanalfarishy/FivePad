package com.fivepad.app.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.fivepad.app.FivePadApplication
import com.fivepad.app.MainActivity
import com.fivepad.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "task_reminders"
private const val EXTRA_TASK_ID = "task_id"
private const val EXTRA_TASK_TEXT = "task_text"

/**
 * Pengingat dijadwalkan di perangkat, bukan dikirim dari server (FR-2.11).
 *
 * Konsekuensinya disengaja: pengingat tetap menyala tanpa koneksi, dan isi tugas
 * tidak pernah meninggalkan perangkat hanya untuk keperluan notifikasi — sejalan
 * dengan NFR-7 yang melarang data pengguna keluar tanpa alasan.
 */
object Reminders {

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    fun schedule(context: Context, taskId: String, text: String, dueAt: Long) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val pending = pendingIntent(context, taskId, text)

        // Alarm presisi butuh izin tersendiri sejak Android 12, dan izin itu
        // diperuntukkan bagi aplikasi jam/kalender. Pengingat tugas tidak menuntut
        // ketepatan detik, jadi presisi dipakai hanya bila sistem mengizinkan dan
        // selebihnya turun ke versi longgar — bukan meminta izin yang berlebihan.
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending)
        }
    }

    fun cancel(context: Context, taskId: String) {
        context.getSystemService<AlarmManager>()
            ?.cancel(pendingIntent(context, taskId, text = ""))
    }

    private fun pendingIntent(context: Context, taskId: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            // Data unik per tugas: tanpa ini PendingIntent dianggap sama dan
            // menjadwalkan tugas kedua akan menimpa alarm tugas pertama.
            data = "fivepad://task/$taskId".toUri()
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun String.toUri() = android.net.Uri.parse(this)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val text = intent.getStringExtra(EXTRA_TASK_TEXT).orEmpty()

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = androidx.core.app.NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            // id.hashCode() menjaga satu notifikasi per tugas: menjadwalkan ulang
            // tugas yang sama menggantikan notifikasinya, bukan menumpuk.
            @Suppress("MissingPermission")
            manager.notify(id.hashCode(), notification)
        }
    }
}

/**
 * Alarm tidak bertahan melewati reboot, jadi seluruh pengingat yang belum lewat
 * dijadwalkan ulang saat perangkat menyala. Tanpa ini, satu kali restart membuat
 * semua pengingat hilang diam-diam — kegagalan yang tidak akan pernah disadari
 * pengguna sampai mereka melewatkan sesuatu.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as FivePadApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.pendingReminders(System.currentTimeMillis())
                    .forEach { Reminders.schedule(context, it.id, it.text, it.dueAt ?: return@forEach) }
            } finally {
                pending.finish()
            }
        }
    }
}
