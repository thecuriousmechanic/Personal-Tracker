package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun taskRoundTripsThroughRoom() = runBlocking {
        val task = TaskEntity(
            id = "persisted-task",
            title = "Verify offline storage",
            status = TaskStatus.INBOX,
            priority = Priority.HIGH,
            dueEpochDay = null,
            projectId = null,
            createdAt = 10L,
        )

        database.appDao().insertTask(task)

        assertEquals(task, database.appDao().observeTasks().first().single())
    }
}
