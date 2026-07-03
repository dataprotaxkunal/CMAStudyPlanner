package com.cma.studyplanner.data

import androidx.room.*
import java.util.UUID

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(7),
    val name: String,
    val colorIndex: Int = 0
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class Chapter(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(7),
    val subjectId: String,
    val name: String,
    val done: Boolean = false
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(7),
    val subject: String,
    val chapter: String,
    val date: String,        // YYYY-MM-DD
    val startTime: String,   // HH:mm
    val endTime: String,     // HH:mm
    val completed: Boolean = false,
    val reminded: Boolean = false,
    val warned: Boolean = false
)

@Entity(tableName = "revisions")
data class Revision(
    @PrimaryKey val id: String = UUID.randomUUID().toString().take(7),
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterName: String,
    val date: String,        // YYYY-MM-DD
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val notes: String = "",
    val completed: Boolean = false
)

// Relationship helper
data class SubjectWithChapters(
    @Embedded val subject: Subject,
    @Relation(parentColumn = "id", entityColumn = "subjectId")
    val chapters: List<Chapter>
)
