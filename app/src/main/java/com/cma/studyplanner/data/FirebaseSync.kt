package com.cma.studyplanner.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseSync {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun signInIfNeeded() {
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun userId(): String? = auth.currentUser?.uid
    fun getBackupId(): String = userId()?.take(8) ?: "not synced"

    // ─── Auto sync single schedule ───
    suspend fun syncSchedule(s: Schedule) {
        val uid = userId() ?: return
        try {
            db.collection("users").document(uid).collection("schedules").document(s.id)
                .set(mapOf("id" to s.id, "subject" to s.subject, "chapter" to s.chapter, "date" to s.date, "startTime" to s.startTime, "endTime" to s.endTime, "completed" to s.completed), SetOptions.merge()).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteSchedule(id: String) {
        val uid = userId() ?: return
        try { db.collection("users").document(uid).collection("schedules").document(id).delete().await() } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Auto sync single revision ───
    suspend fun syncRevision(r: Revision) {
        val uid = userId() ?: return
        try {
            db.collection("users").document(uid).collection("revisions").document(r.id)
                .set(mapOf("id" to r.id, "subjectId" to r.subjectId, "subjectName" to r.subjectName, "chapterId" to r.chapterId, "chapterName" to r.chapterName, "date" to r.date, "startTime" to r.startTime, "endTime" to r.endTime, "notes" to r.notes, "completed" to r.completed), SetOptions.merge()).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteRevision(id: String) {
        val uid = userId() ?: return
        try { db.collection("users").document(uid).collection("revisions").document(id).delete().await() } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Auto sync subject + chapters ───
    suspend fun syncSubject(swc: SubjectWithChapters) {
        val uid = userId() ?: return
        try {
            db.collection("users").document(uid).collection("subjects").document(swc.subject.id)
                .set(mapOf("id" to swc.subject.id, "name" to swc.subject.name, "colorIndex" to swc.subject.colorIndex,
                    "chapters" to swc.chapters.map { ch -> mapOf("id" to ch.id, "name" to ch.name, "done" to ch.done, "importantQuestions" to ch.importantQuestions) }
                ), SetOptions.merge()).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteSubject(id: String) {
        val uid = userId() ?: return
        try { db.collection("users").document(uid).collection("subjects").document(id).delete().await() } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Full backup ───
    suspend fun backupAll(schedules: List<Schedule>, revisions: List<Revision>, subjects: List<SubjectWithChapters>, prefs: PrefsManager) {
        val uid = userId() ?: return
        try {
            db.collection("users").document(uid).set(mapOf(
                "name" to prefs.userName, "exam" to prefs.examName, "examDate" to prefs.examDate,
                "lastBackup" to com.google.firebase.Timestamp.now()
            ), SetOptions.merge()).await()
            schedules.forEach { syncSchedule(it) }
            revisions.forEach { syncRevision(it) }
            subjects.forEach { syncSubject(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Restore from cloud ───
    suspend fun restoreData(): RestoredData? {
        val uid = userId() ?: return null
        return try {
            val scheduleDocs = db.collection("users").document(uid).collection("schedules").get().await()
            val revisionDocs = db.collection("users").document(uid).collection("revisions").get().await()
            val subjectDocs = db.collection("users").document(uid).collection("subjects").get().await()

            val schedules = scheduleDocs.documents.mapNotNull { doc ->
                try { Schedule(id = doc.getString("id") ?: return@mapNotNull null, subject = doc.getString("subject") ?: "", chapter = doc.getString("chapter") ?: "", date = doc.getString("date") ?: "", startTime = doc.getString("startTime") ?: "09:00", endTime = doc.getString("endTime") ?: "10:00", completed = doc.getBoolean("completed") ?: false) } catch (_: Exception) { null }
            }
            val revisions = revisionDocs.documents.mapNotNull { doc ->
                try { Revision(id = doc.getString("id") ?: return@mapNotNull null, subjectId = doc.getString("subjectId") ?: "", subjectName = doc.getString("subjectName") ?: "", chapterId = doc.getString("chapterId") ?: "", chapterName = doc.getString("chapterName") ?: "", date = doc.getString("date") ?: "", startTime = doc.getString("startTime") ?: "09:00", endTime = doc.getString("endTime") ?: "10:00", notes = doc.getString("notes") ?: "", completed = doc.getBoolean("completed") ?: false) } catch (_: Exception) { null }
            }
            val subjects = subjectDocs.documents.mapNotNull { doc ->
                try {
                    val subj = Subject(id = doc.getString("id") ?: return@mapNotNull null, name = doc.getString("name") ?: "", colorIndex = (doc.getLong("colorIndex") ?: 0L).toInt())
                    @Suppress("UNCHECKED_CAST")
                    val chapList = doc.get("chapters") as? List<Map<String, Any>> ?: emptyList()
                    val chapters = chapList.map { cm -> Chapter(id = cm["id"] as? String ?: "", subjectId = subj.id, name = cm["name"] as? String ?: "", done = cm["done"] as? Boolean ?: false, importantQuestions = cm["importantQuestions"] as? String ?: "") }
                    SubjectWithChapters(subj, chapters)
                } catch (_: Exception) { null }
            }
            RestoredData(schedules, revisions, subjects)
        } catch (e: Exception) { null }
    }

    // Admin announcement
    suspend fun getAnnouncement(): Pair<String, String>? {
        return try {
            val doc = db.collection("admin").document("announcement").get().await()
            val title = doc.getString("title") ?: return null
            val message = doc.getString("message") ?: return null
            Pair(title, message)
        } catch (_: Exception) { null }
    }
}

data class RestoredData(
    val schedules: List<Schedule>,
    val revisions: List<Revision>,
    val subjects: List<SubjectWithChapters>
)
