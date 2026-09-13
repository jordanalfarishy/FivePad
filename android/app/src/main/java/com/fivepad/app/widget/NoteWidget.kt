package com.fivepad.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.fivepad.app.FivePadApplication
import com.fivepad.app.MainActivity
import com.fivepad.app.R
import com.fivepad.app.data.Note
import com.fivepad.app.data.ThemeMode
import com.fivepad.app.ui.LaunchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Each widget keeps its own slot; its five buttons change the preview in place. */
class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SELECT) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val slot = intent.getIntExtra(LaunchRequest.EXTRA_SLOT, 0)
            val manager = AppWidgetManager.getInstance(context)
            if (slot in 1..Note.SLOT_COUNT && manager.getAppWidgetInfo(id)?.provider?.className == javaClass.name) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("slot-$id", slot).apply()
                refresh(context)
            }
        } else super.onReceive(context, intent)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            ids.forEach { remove("slot-$it") }
        }.apply()
    }

    private fun refresh(context: Context) {
        val pending = goAsync()
        val app = context.applicationContext as FivePadApplication
        app.applicationScope.launch {
            try {
                val notes = app.repository.allNotes()
                withContext(Dispatchers.IO) { updateAll(context, notes) }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                app.noteErrors.value = true
            } finally { pending?.finish() }
        }
    }

    companion object {
        private const val ACTION_SELECT = "com.fivepad.app.widget.SELECT_SLOT"
        private const val PREFS = "note-widgets"
        private val buttonIds = intArrayOf(R.id.widget_slot_1, R.id.widget_slot_2, R.id.widget_slot_3, R.id.widget_slot_4, R.id.widget_slot_5)
        private val darkAccents = intArrayOf(0xFFEF7A5A.toInt(), 0xFFE0A63F.toInt(), 0xFF63BC85.toInt(), 0xFF48BEDD.toInt(), 0xFFA186D6.toInt())
        private val lightAccents = intArrayOf(0xFFDB2F00.toInt(), 0xFFA06700.toInt(), 0xFF1A8442.toInt(), 0xFF0E7D9B.toInt(), 0xFF5320B7.toInt())

        fun updateAll(context: Context, notes: List<Note>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, NoteWidget::class.java))
            val app = context.applicationContext as FivePadApplication
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val dark = app.preferences.theme.value == ThemeMode.DARK
            val accents = if (dark) darkAccents else lightAccents
            ids.forEach { id ->
                val slot = prefs.getInt("slot-$id", app.preferences.lastSlot).coerceIn(1, Note.SLOT_COUNT)
                val note = notes.firstOrNull { it.slot == slot } ?: return@forEach
                val views = RemoteViews(context.packageName, R.layout.note_widget)
                views.setInt(R.id.widget_root, "setBackgroundColor", if (dark) 0xFF232324.toInt() else 0xFFF9F9F9.toInt())
                views.setTextViewText(R.id.widget_title, note.label.ifEmpty { context.getString(R.string.slot_description, slot) })
                views.setTextColor(R.id.widget_title, accents[slot - 1])
                views.setTextViewText(R.id.widget_body, note.body.take(600).ifEmpty { context.getString(R.string.widget_empty) })
                views.setTextColor(R.id.widget_body, if (dark) Color.WHITE else 0xFF25242C.toInt())
                val open = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("fivepad://slot/$slot")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingOpen = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_body, pendingOpen)
                views.setOnClickPendingIntent(R.id.widget_title, pendingOpen)
                buttonIds.forEachIndexed { index, button ->
                    val number = index + 1
                    views.setTextViewText(button, if (slot == number) "● $number" else "○ $number")
                    views.setTextColor(button, accents[index])
                    views.setContentDescription(button, context.getString(R.string.slot_description, number))
                    val select = Intent(context, NoteWidget::class.java).apply {
                        action = ACTION_SELECT
                        data = Uri.parse("fivepad-widget://$id/$number")
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        putExtra(LaunchRequest.EXTRA_SLOT, number)
                    }
                    views.setOnClickPendingIntent(button, PendingIntent.getBroadcast(context, id, select, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
