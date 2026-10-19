package com.krishna.personalos.core.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.krishna.personalos.MainActivity
import com.krishna.personalos.R
import com.krishna.personalos.core.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class TaskReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private fun intent(id: String) = Intent(context, ReminderReceiver::class.java).setAction("TASK_REMINDER").setData(Uri.parse("personal-tracker://task/$id")).putExtra("taskId", id)
    fun cancel(id: String) {
        PendingIntent.getBroadcast(context, 0, intent(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarms.cancel(it); it.cancel() }
        context.getSystemService(NotificationManager::class.java).cancel(id, 0)
    }
    fun schedule(task: TaskEntity) {
        cancel(task.id)
        val time = triggerAt(task) ?: return
        if (time <= System.currentTimeMillis()) return
        val pending = PendingIntent.getBroadcast(context, 0, intent(task.id).putExtra("triggerAt", time), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }
    companion object {
        const val OPEN_TASK = "openTaskId"
        fun triggerAt(task: TaskEntity): Long? {
            if (task.deletedAt != null || task.status == TaskStatus.DONE || (task.repeatDays > 0 && task.repeatPaused) || task.reminderMinutes == null || task.dueEpochDay == null) return null
            return LocalDate.ofEpochDay(task.dueEpochDay).atStartOfDay().plusMinutes(task.reminderMinutes.toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = AppDatabase.get(context)
                if (intent.action != "TASK_REMINDER") {
                    database.appDao().observeTasks().first().forEach { TaskReminders(context).schedule(it) }
                    return@launch
                }
                val id = intent.getStringExtra("taskId") ?: return@launch
                val task = database.appDao().task(id) ?: return@launch
                val triggerAt = TaskReminders.triggerAt(task) ?: return@launch
                if (triggerAt != intent.getLongExtra("triggerAt", -1) || triggerAt > System.currentTimeMillis()) return@launch
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@launch
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel("tasks", "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
                val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).setData(Uri.parse("personal-tracker://open/$id")).putExtra(TaskReminders.OPEN_TASK, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.notify(id, 0, NotificationCompat.Builder(context, "tasks")
                    .setSmallIcon(R.drawable.ic_vector_launcher).setContentTitle(task.title).setContentText("Open your plan when you’re ready.")
                    .setContentIntent(open).setAutoCancel(true).build())
            } catch (failure: Exception) {
                android.util.Log.e("TaskReminders", "Could not process task reminder", failure)
            } finally { pending.finish() }
        }
    }
}
