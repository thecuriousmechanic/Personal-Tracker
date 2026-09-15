package com.krishna.personalos.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "template_pages")
data class TemplatePageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val kind: String,
    val cover: String,
    val sourceUrl: String,
    val importedAt: String,
    val data: String,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

@Dao
interface TemplatePageDao {
    @Query("SELECT * FROM template_pages WHERE deleted = 0 ORDER BY rowid") fun observe(): Flow<List<TemplatePageEntity>>
    @Query("SELECT * FROM template_pages WHERE id = :id") suspend fun page(id: String): TemplatePageEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun importPage(page: TemplatePageEntity)
    @Update suspend fun update(page: TemplatePageEntity)
}
