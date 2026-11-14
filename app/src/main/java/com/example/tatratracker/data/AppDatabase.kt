package com.example.tatratracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tatratracker.fit.MAX_ELAPSED_FROM_POINTS_MILLIS

@Database(
    entities = [
        ActivityEntity::class,
        TrackPointEntity::class,
        PlannedRouteEntity::class,
        PlannedRoutePointEntity::class,
        SavedPlaceEntity::class,
        PoiVisitEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao

    abstract fun plannedRouteDao(): PlannedRouteDao

    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 =
            object : Migration(
                5,
                6
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `planned_routes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `distanceMeters` REAL NOT NULL,
                            `durationSeconds` INTEGER NOT NULL,
                            `ascentMeters` INTEGER,
                            `descentMeters` INTEGER,
                            `minElevationMeters` INTEGER,
                            `maxElevationMeters` INTEGER,
                            `startName` TEXT,
                            `endName` TEXT
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `planned_route_points` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `routeId` INTEGER NOT NULL,
                            `pointType` TEXT NOT NULL,
                            `orderIndex` INTEGER NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `altitudeMeters` REAL,
                            `distanceMeters` REAL,
                            `name` TEXT,
                            FOREIGN KEY(`routeId`)
                                REFERENCES `planned_routes`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_planned_route_points_routeId`
                        ON `planned_route_points` (`routeId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_planned_route_points_routeId_pointType_orderIndex`
                        ON `planned_route_points`
                        (`routeId`, `pointType`, `orderIndex`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_6_7 =
            object : Migration(
                6,
                7
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_places` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `note` TEXT,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_saved_places_category`
                        ON `saved_places` (`category`)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_7_8 =
            object : Migration(
                7,
                8
            ) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `poi_visits` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `placeId` INTEGER NOT NULL,
                            `activityId` INTEGER,
                            `visitedAt` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            FOREIGN KEY(`placeId`)
                                REFERENCES `saved_places`(`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_poi_visits_placeId`
                        ON `poi_visits` (`placeId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_poi_visits_activityId`
                        ON `poi_visits` (`activityId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        `index_poi_visits_placeId_activityId`
                        ON `poi_visits`
                        (`placeId`, `activityId`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        INSERT INTO `poi_visits`
                        (
                            `placeId`,
                            `activityId`,
                            `visitedAt`,
                            `source`
                        )
                        SELECT
                            `id`,
                            NULL,
                            `createdAt`,
                            'MANUAL'
                        FROM `saved_places`
                        """.trimIndent()
                    )
                }
            }

        /*
         * 8 → 9: osobny czas w ruchu.
         * Do tej pory durationMillis przechowywało czas w ruchu (FIT: total_timer_time,
         * TCX: suma TotalTimeSeconds), więc przenosimy go do movingDurationMillis.
         * Czas całkowity odtwarzamy z punktów (ostatni − pierwszy znacznik czasu),
         * tylko gdy wynik jest dłuższy od dotychczasowego i nie przekracza 24 h.
         * Zapisanych prędkości i tempa nie przeliczamy.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `activities`
                    ADD COLUMN `movingDurationMillis` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE `activities`
                    SET `movingDurationMillis` = `durationMillis`
                    """.trimIndent()
                )

                val elapsedFromPoints =
                    """
                    (
                        SELECT (MAX(`timestampSeconds`) - MIN(`timestampSeconds`)) * 1000
                        FROM `track_points`
                        WHERE `track_points`.`activityId` = `activities`.`id`
                            AND `timestampSeconds` IS NOT NULL
                    )
                    """.trimIndent()

                db.execSQL(
                    """
                    UPDATE `activities`
                    SET `durationMillis` = $elapsedFromPoints
                    WHERE $elapsedFromPoints IS NOT NULL
                        AND $elapsedFromPoints > `durationMillis`
                        AND $elapsedFromPoints <= $MAX_ELAPSED_FROM_POINTS_MILLIS
                    """.trimIndent()
                )
            }
        }

        // 9 → 10: notatka użytkownika przy aktywności (opcjonalna).
        private val MIGRATION_9_10 = object : Migration(9, 10) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `note` TEXT")
            }
        }

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room
                            .databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "tatra_tracker_database"
                            )
                            .addMigrations(
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                                MIGRATION_8_9,
                                MIGRATION_9_10
                            )
                            .build()

                    INSTANCE =
                        instance

                    instance
                }
        }
    }
}
