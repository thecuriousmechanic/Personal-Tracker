package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class AppConverters {
    @TypeConverter fun taskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
    @TypeConverter fun taskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun priority(value: String): Priority = Priority.valueOf(value)
    @TypeConverter fun priority(value: Priority): String = value.name
    @TypeConverter fun projectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)
    @TypeConverter fun projectStatus(value: ProjectStatus): String = value.name
    @TypeConverter fun relationType(value: String): RelationType = RelationType.valueOf(value)
    @TypeConverter fun relationType(value: RelationType): String = value.name
    @TypeConverter fun activityType(value: String): ActivityType = ActivityType.valueOf(value)
    @TypeConverter fun activityType(value: ActivityType): String = value.name
}

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ProjectEntity::class,
        GoalEntity::class,
        NoteEntity::class,
        EntityRelationEntity::class,
        ActivityEventEntity::class,
        TrackerEntity::class,
        TrackerFieldEntity::class,
        TrackerEntryEntity::class,
        TrackerCellEntity::class,
        TemplatePageEntity::class,
    ],
    version = 4,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
    exportSchema = true,
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun trackerDao(): TrackerDao
    abstract fun templatePageDao(): TemplatePageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "personal-os.db",
            ).build().also { instance = it }
        }
    }
}
