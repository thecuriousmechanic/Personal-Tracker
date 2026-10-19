package com.krishna.personalos.core.database

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.krishna.personalos.MainActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.krishna.personalos.core.reminders.ReminderReceiver
import com.krishna.personalos.core.reminders.TaskReminders
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TaskReminderTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun completedDeletedAndPausedTasksDoNotScheduleReminders() {
        val task = TaskEntity("reminder", "Read", TaskStatus.UPCOMING, Priority.MEDIUM, LocalDate.now().plusDays(1).toEpochDay(), null, 1, repeatDays = 1, reminderMinutes = 540)
        val expected = LocalDate.now().plusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, TaskReminders.triggerAt(task))
        assertNull(TaskReminders.triggerAt(task.copy(status = TaskStatus.DONE)))
        assertNull(TaskReminders.triggerAt(task.copy(deletedAt = 2)))
        assertNull(TaskReminders.triggerAt(task.copy(repeatPaused = true)))
        assertNull(TaskReminders.triggerAt(task.copy(reminderMinutes = null)))
    }

    @Test fun reminderBroadcastChecksSavedTaskAndPostsAnOpenTaskNotification() = runBlocking {
        Assume.assumeTrue("Notification delivery is tested only in the isolated QA package", context.packageName.endsWith(".qa"))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        instrumentation.runOnMainSync { activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        android.util.Log.i("ReminderDeliveryQA", "QA activity opened")
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Some phones disallow shell permission grants. Exercise the actual Android permission dialog.
            instrumentation.runOnMainSync { activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 321) }
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
            fun allowButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (node == null) return null
                if (node.viewIdResourceName?.endsWith(":id/permission_allow_button") == true) return node
                repeat(node.childCount) { index -> allowButton(node.getChild(index))?.let { return it } }
                return null
            }
            val deadline = System.currentTimeMillis() + 5_000
            while (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && System.currentTimeMillis() < deadline) {
                allowButton(automation.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                kotlinx.coroutines.delay(100)
            }
            assertEquals("Allow notification permission in the QA app", PackageManager.PERMISSION_GRANTED, ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS))
        }
        android.util.Log.i("ReminderDeliveryQA", "Notification permission allowed")
        val database = AppDatabase.get(context)
        val id = "reminder-qa-${UUID.randomUUID()}"
        val task = TaskEntity(id, "QA reminder", TaskStatus.UPCOMING, Priority.MEDIUM, LocalDate.now().minusDays(1).toEpochDay(), null, 1, reminderMinutes = 540)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            database.appDao().insertTask(task)
            val intent = Intent(context, ReminderReceiver::class.java).setAction("TASK_REMINDER").setData(Uri.parse("personal-tracker://task/$id")).putExtra("taskId", id).putExtra("triggerAt", TaskReminders.triggerAt(task)!!)
            context.sendBroadcast(intent)
            val deadline = System.currentTimeMillis() + 5_000
            while (manager.activeNotifications.none { it.tag == id } && System.currentTimeMillis() < deadline) kotlinx.coroutines.delay(100)
            val notification = manager.activeNotifications.firstOrNull { it.tag == id }
            assertNotNull(notification)
            assertNotNull(notification!!.notification.contentIntent)
            assertEquals("QA reminder", notification.notification.extras.getCharSequence("android.title").toString())
            android.util.Log.i("ReminderDeliveryQA", "Notification delivery verified")
            notification.notification.contentIntent.send()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Edit task", substring = false).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            compose.onNode(hasSetTextAction() and hasText("QA reminder", substring = false)).assertExists()
            android.util.Log.i("ReminderDeliveryQA", "Notification task opening verified")
            compose.onNodeWithContentDescription("Cancel", substring = false).performClick()
            TaskReminders(context).cancel(id)
            database.appDao().updateTask(task.copy(status = TaskStatus.DONE))
            context.sendBroadcast(intent)
            kotlinx.coroutines.delay(500)
            assertTrue(manager.activeNotifications.none { it.tag == id })
        } finally {
            TaskReminders(context).cancel(id); database.appDao().deleteTask(id, System.currentTimeMillis())
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
        }
    }
}
