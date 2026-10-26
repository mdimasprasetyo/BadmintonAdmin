package app.shuttlers.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written schema migrations. The destructive fallback is off, so every version bump needs
 * one of these, otherwise opening the database fails instead of wiping the club roster.
 */
object Migrations {

    /** v5: tonight's wins and losses per checked-in player (the permanent totals already exist). */
    val MIGRATION_4_5_STATEMENTS = listOf(
        "ALTER TABLE session_participants ADD COLUMN wins INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE session_participants ADD COLUMN losses INTEGER NOT NULL DEFAULT 0"
    )

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_4_5_STATEMENTS.forEach { db.execSQL(it) }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_4_5)
}
