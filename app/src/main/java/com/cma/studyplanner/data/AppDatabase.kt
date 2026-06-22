package com.cma.studyplanner.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name")
    fun getAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY name")
    suspend fun getAllOnce(): List<Subject>

    @Transaction
    @Query("SELECT * FROM subjects ORDER BY name")
    fun getAllWithChapters(): Flow<List<SubjectWithChapters>>

    @Transaction
    @Query("SELECT * FROM subjects ORDER BY name")
    suspend fun getAllWithChaptersOnce(): List<SubjectWithChapters>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY name")
    fun getBySubject(subjectId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY name")
    suspend fun getBySubjectOnce(subjectId: String): List<Chapter>

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE done = 1")
    suspend fun doneCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: Chapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Update
    suspend fun update(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY date, startTime")
    fun getAll(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules ORDER BY date, startTime")
    suspend fun getAllOnce(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE date = :date ORDER BY startTime")
    fun getByDate(date: String): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE date = :date ORDER BY startTime")
    suspend fun getByDateOnce(date: String): List<Schedule>

    @Query("SELECT * FROM schedules WHERE date BETWEEN :start AND :end ORDER BY date, startTime")
    suspend fun getByDateRange(start: String, end: String): List<Schedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: Schedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<Schedule>)

    @Update
    suspend fun update(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revisions ORDER BY date")
    fun getAll(): Flow<List<Revision>>

    @Query("SELECT * FROM revisions ORDER BY date")
    suspend fun getAllOnce(): List<Revision>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(revision: Revision)

    @Update
    suspend fun update(revision: Revision)

    @Query("DELETE FROM revisions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [Subject::class, Chapter::class, Schedule::class, Revision::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun revisionDao(): RevisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cma_study_planner.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
