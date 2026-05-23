package com.calico.tutor.ui.screen

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "app_database.db"
        private const val DATABASE_VERSION = 6

        const val TABLE_COURSES = "courses"
        const val TABLE_APPROVED_COURSES = "approved_courses"
        const val TABLE_APPLICATIONS = "applications"
        const val TABLE_TUTOR_PROFILE = "tutor_profile"
        const val TABLE_COURSE_NOTES = "course_notes"
        const val TABLE_PENDING_COURSE_NOTES = "pending_course_notes"

        const val COLUMN_COURSE_ID = "id"
        const val COLUMN_COURSE_TITLE = "title"
        const val COLUMN_COURSE_DESCRIPTION = "description"
        const val COLUMN_COURSE_CATEGORY = "category"

        const val COLUMN_TUTOR_ID = "id"
        const val COLUMN_TUTOR_NAME = "name"
        const val COLUMN_TUTOR_EMAIL = "email"
        const val COLUMN_TUTOR_SUBJECT = "subject"
        const val COLUMN_NOTE_COURSE_ID = "course_id"
        const val COLUMN_NOTE_TEXT = "note"
        const val COLUMN_NOTE_IS_FAVORITE = "is_favorite"
        const val COLUMN_NOTE_UPDATED_AT = "updated_at"

        private val CREATE_TABLE_COURSES = (
            "CREATE TABLE $TABLE_COURSES ("
            + "$COLUMN_COURSE_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "api_id TEXT, "
            + "$COLUMN_COURSE_TITLE TEXT NOT NULL, "
            + "$COLUMN_COURSE_DESCRIPTION TEXT, "
            + "$COLUMN_COURSE_CATEGORY TEXT"
            + ")"
        )

        private val CREATE_TABLE_APPROVED_COURSES = (
            "CREATE TABLE $TABLE_APPROVED_COURSES ("
            + "$COLUMN_COURSE_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "$COLUMN_COURSE_TITLE TEXT NOT NULL, "
            + "$COLUMN_COURSE_DESCRIPTION TEXT, "
            + "$COLUMN_COURSE_CATEGORY TEXT"
            + ")"
        )

        private val CREATE_TABLE_APPLICATIONS = (
            "CREATE TABLE $TABLE_APPLICATIONS ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "course_id TEXT, "
            + "course_name TEXT, "
            + "course_code TEXT, "
            + "status TEXT, "
            + "rejection_reason TEXT"
            + ")"
        )

        private const val TABLE_PENDING_APPLICATIONS = "pending_applications"

        private val CREATE_TABLE_PENDING_APPLICATIONS = (
            "CREATE TABLE $TABLE_PENDING_APPLICATIONS ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "course_id TEXT, "
            + "course_name TEXT, "
            + "course_code TEXT, "
            + "notes TEXT, "
            + "created_at TEXT"
            + ")"
        )

private val CREATE_TABLE_TUTOR_PROFILE = (
            "CREATE TABLE $TABLE_TUTOR_PROFILE ("
            + "$COLUMN_TUTOR_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "$COLUMN_TUTOR_NAME TEXT NOT NULL, "
            + "$COLUMN_TUTOR_EMAIL TEXT NOT NULL, "
            + "$COLUMN_TUTOR_SUBJECT TEXT, "
            + "profile_image_url TEXT"
            + ")"
        )

        private val CREATE_TABLE_COURSE_NOTES = (
            "CREATE TABLE $TABLE_COURSE_NOTES ("
            + "$COLUMN_NOTE_COURSE_ID TEXT PRIMARY KEY, "
            + "$COLUMN_NOTE_TEXT TEXT, "
            + "$COLUMN_NOTE_IS_FAVORITE INTEGER NOT NULL DEFAULT 0, "
            + "$COLUMN_NOTE_UPDATED_AT INTEGER NOT NULL"
            + ")"
        )

        private val CREATE_TABLE_PENDING_COURSE_NOTES = (
            "CREATE TABLE $TABLE_PENDING_COURSE_NOTES ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "$COLUMN_NOTE_COURSE_ID TEXT NOT NULL, "
            + "$COLUMN_NOTE_TEXT TEXT, "
            + "$COLUMN_NOTE_UPDATED_AT INTEGER NOT NULL"
            + ")"
        )
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_COURSES)
        db?.execSQL(CREATE_TABLE_APPROVED_COURSES)
        db?.execSQL(CREATE_TABLE_APPLICATIONS)
        db?.execSQL(CREATE_TABLE_PENDING_APPLICATIONS)
        db?.execSQL(CREATE_TABLE_TUTOR_PROFILE)
        db?.execSQL(CREATE_TABLE_COURSE_NOTES)
        db?.execSQL(CREATE_TABLE_PENDING_COURSE_NOTES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {
            db?.execSQL(CREATE_TABLE_COURSE_NOTES)
            db?.execSQL(CREATE_TABLE_PENDING_COURSE_NOTES)
            return
        }
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_COURSES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_APPROVED_COURSES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_APPLICATIONS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_APPLICATIONS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TUTOR_PROFILE")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_COURSE_NOTES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_COURSE_NOTES")
        onCreate(db)
    }

    fun saveCourses(courses: List<Course>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_COURSES, null, null)
            for (course in courses) {
                val values = ContentValues().apply {
                    put("api_id", course.apiId)
                    put(COLUMN_COURSE_TITLE, course.title)
                    put(COLUMN_COURSE_DESCRIPTION, course.description)
                    put(COLUMN_COURSE_CATEGORY, course.category)
                }
                db.insert(TABLE_COURSES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getCourses(): List<Course> {
        val courses = mutableListOf<Course>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.query(
            TABLE_COURSES,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val apiIdCol = it.getColumnIndex("api_id")
                val apiId = if (apiIdCol >= 0) it.getString(apiIdCol) else null
                val course = Course(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_COURSE_ID)),
                    apiId = apiId,
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_DESCRIPTION)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_CATEGORY))
                )
                courses.add(course)
            }
        }
        db.close()
        return courses
    }

    fun saveApprovedCourses(courses: List<Course>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_APPROVED_COURSES, null, null)
            for (course in courses) {
                val values = ContentValues().apply {
                    put(COLUMN_COURSE_TITLE, course.title)
                    put(COLUMN_COURSE_DESCRIPTION, course.description)
                    put(COLUMN_COURSE_CATEGORY, course.category)
                }
                db.insert(TABLE_APPROVED_COURSES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getApprovedCourses(): List<Course> {
        val courses = mutableListOf<Course>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.query(
            TABLE_APPROVED_COURSES,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val course = Course(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_COURSE_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_DESCRIPTION)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_COURSE_CATEGORY))
                )
                courses.add(course)
            }
        }
        db.close()
        return courses
    }

    fun saveCourse(course: Course): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE_TITLE, course.title)
            put(COLUMN_COURSE_DESCRIPTION, course.description)
            put(COLUMN_COURSE_CATEGORY, course.category)
        }
        return db.insert(TABLE_COURSES, null, values).also { db.close() }
    }

    data class Course(
        val id: Long = 0,
        val apiId: String? = null,
        val title: String,
        val description: String? = null,
        val category: String? = null
    )

    fun saveTutorProfile(tutor: TutorProfile): Long {
        val db = this.writableDatabase
        db.delete(TABLE_TUTOR_PROFILE, null, null)
        val values = ContentValues().apply {
            put(COLUMN_TUTOR_NAME, tutor.name)
            put(COLUMN_TUTOR_EMAIL, tutor.email)
            put(COLUMN_TUTOR_SUBJECT, tutor.subject)
            put("profile_image_url", tutor.profileImageUrl)
        }
        return db.insert(TABLE_TUTOR_PROFILE, null, values).also { db.close() }
    }

    fun getTutorProfiles(): List<TutorProfile> {
        val tutors = mutableListOf<TutorProfile>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.query(
            TABLE_TUTOR_PROFILE,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val tutor = TutorProfile(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_TUTOR_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_EMAIL)),
                    subject = it.getString(it.getColumnIndexOrThrow(COLUMN_TUTOR_SUBJECT)),
                    profileImageUrl = it.getString(it.getColumnIndexOrThrow("profile_image_url"))
                )
                tutors.add(tutor)
            }
        }
        db.close()
        return tutors
    }

    data class TutorProfile(
        val id: Long = 0,
        val name: String,
        val email: String,
        val subject: String? = null,
        val profileImageUrl: String? = null
    )

    data class Application(
        val id: Long = 0,
        val courseId: String,
        val courseName: String,
        val courseCode: String,
        val status: String,
        val rejectionReason: String? = null
    )

    fun saveApplications(applications: List<Application>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_APPLICATIONS, null, null)
            for (app in applications) {
                val values = ContentValues().apply {
                    put("course_id", app.courseId)
                    put("course_name", app.courseName)
                    put("course_code", app.courseCode)
                    put("status", app.status)
                    put("rejection_reason", app.rejectionReason)
                }
                db.insert(TABLE_APPLICATIONS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getApplications(): List<Application> {
        val applications = mutableListOf<Application>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.query(
            TABLE_APPLICATIONS,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val app = Application(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    courseId = it.getString(it.getColumnIndexOrThrow("course_id")),
                    courseName = it.getString(it.getColumnIndexOrThrow("course_name")),
                    courseCode = it.getString(it.getColumnIndexOrThrow("course_code")),
                    status = it.getString(it.getColumnIndexOrThrow("status")),
                    rejectionReason = it.getString(it.getColumnIndexOrThrow("rejection_reason"))
                )
                applications.add(app)
            }
        }
        db.close()
        return applications
    }

    data class PendingApplication(
        val id: Long = 0,
        val courseId: String,
        val courseName: String,
        val courseCode: String,
        val notes: String? = null,
        val createdAt: String
    )

    data class CourseNote(
        val courseId: String,
        val note: String? = null,
        val isFavorite: Boolean = false,
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class PendingCourseNote(
        val id: Long = 0,
        val courseId: String,
        val note: String? = null,
        val updatedAt: Long
    )

    fun savePendingApplication(app: PendingApplication): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("course_id", app.courseId)
            put("course_name", app.courseName)
            put("course_code", app.courseCode)
            put("notes", app.notes)
            put("created_at", app.createdAt)
        }
        return db.insert(TABLE_PENDING_APPLICATIONS, null, values).also { db.close() }
    }

    fun getPendingApplications(): List<PendingApplication> {
        val applications = mutableListOf<PendingApplication>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.query(
            TABLE_PENDING_APPLICATIONS,
            null,
            null,
            null,
            null,
            null,
            "created_at ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val app = PendingApplication(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    courseId = it.getString(it.getColumnIndexOrThrow("course_id")),
                    courseName = it.getString(it.getColumnIndexOrThrow("course_name")),
                    courseCode = it.getString(it.getColumnIndexOrThrow("course_code")),
                    notes = it.getString(it.getColumnIndexOrThrow("notes")),
                    createdAt = it.getString(it.getColumnIndexOrThrow("created_at"))
                )
                applications.add(app)
            }
        }
        db.close()
        return applications
    }

    fun deletePendingApplication(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_PENDING_APPLICATIONS, "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun upsertCourseNote(note: CourseNote) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOTE_COURSE_ID, note.courseId)
            put(COLUMN_NOTE_TEXT, note.note)
            put(COLUMN_NOTE_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(COLUMN_NOTE_UPDATED_AT, note.updatedAt)
        }
        db.insertWithOnConflict(TABLE_COURSE_NOTES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getCourseNote(courseId: String): CourseNote? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_COURSE_NOTES,
            arrayOf(COLUMN_NOTE_COURSE_ID, COLUMN_NOTE_TEXT, COLUMN_NOTE_IS_FAVORITE, COLUMN_NOTE_UPDATED_AT),
            "$COLUMN_NOTE_COURSE_ID = ?",
            arrayOf(courseId),
            null,
            null,
            null
        )
        val result = cursor.use {
            if (it.moveToFirst()) {
                CourseNote(
                    courseId = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_COURSE_ID)),
                    note = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_TEXT)),
                    isFavorite = it.getInt(it.getColumnIndexOrThrow(COLUMN_NOTE_IS_FAVORITE)) == 1,
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_UPDATED_AT))
                )
            } else {
                null
            }
        }
        db.close()
        return result
    }

    fun getFavoriteCourses(): List<CourseNote> {
        val favorites = mutableListOf<CourseNote>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_COURSE_NOTES,
            arrayOf(COLUMN_NOTE_COURSE_ID, COLUMN_NOTE_TEXT, COLUMN_NOTE_IS_FAVORITE, COLUMN_NOTE_UPDATED_AT),
            "$COLUMN_NOTE_IS_FAVORITE = 1",
            null,
            null,
            null,
            "$COLUMN_NOTE_UPDATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                favorites.add(
                    CourseNote(
                        courseId = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_COURSE_ID)),
                        note = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_TEXT)),
                        isFavorite = it.getInt(it.getColumnIndexOrThrow(COLUMN_NOTE_IS_FAVORITE)) == 1,
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_UPDATED_AT))
                    )
                )
            }
        }
        db.close()
        return favorites
    }

    fun enqueuePendingCourseNote(courseId: String, note: String?, updatedAt: Long = System.currentTimeMillis()): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOTE_COURSE_ID, courseId)
            put(COLUMN_NOTE_TEXT, note)
            put(COLUMN_NOTE_UPDATED_AT, updatedAt)
        }
        return db.insert(TABLE_PENDING_COURSE_NOTES, null, values).also { db.close() }
    }

    fun getPendingCourseNotes(): List<PendingCourseNote> {
        val pending = mutableListOf<PendingCourseNote>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PENDING_COURSE_NOTES,
            arrayOf("id", COLUMN_NOTE_COURSE_ID, COLUMN_NOTE_TEXT, COLUMN_NOTE_UPDATED_AT),
            null,
            null,
            null,
            null,
            "$COLUMN_NOTE_UPDATED_AT ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                pending.add(
                    PendingCourseNote(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        courseId = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_COURSE_ID)),
                        note = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_TEXT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_UPDATED_AT))
                    )
                )
            }
        }
        db.close()
        return pending
    }

    fun deletePendingCourseNote(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_PENDING_COURSE_NOTES, "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun deletePendingCourseNotesForCourse(courseId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_PENDING_COURSE_NOTES, "$COLUMN_NOTE_COURSE_ID = ?", arrayOf(courseId))
        db.close()
    }

    fun clearAllTables() {
        Log.d("DB", "Clearing DB")
        val db = this.writableDatabase
        db.delete(TABLE_COURSES, null, null)
        db.delete(TABLE_APPROVED_COURSES, null, null)
        db.delete(TABLE_APPLICATIONS, null, null)
        db.delete(TABLE_TUTOR_PROFILE, null, null)
        db.delete(TABLE_COURSE_NOTES, null, null)
        db.delete(TABLE_PENDING_COURSE_NOTES, null, null)
        db.close()
        Log.d("DB", "DB Cleared")
    }
}