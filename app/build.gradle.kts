import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)

    id("com.google.devtools.ksp") version "2.3.6"
}


class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {

        return listOf(
            "room.schemaLocation=${schemaDir.path}"
        )
    }
}

/*
 * =========================================================
 * MAPTILER API KEY
 * =========================================================
 */

val localProperties =
    Properties().apply {

        val localPropertiesFile =
            rootProject.file(
                "local.properties"
            )

        if (localPropertiesFile.exists()) {

            localPropertiesFile
                .inputStream()
                .use { stream ->

                    load(stream)
                }
        }
    }

val mapTilerApiKey =
    localProperties.getProperty(
        "MAPTILER_API_KEY",
        ""
    )

/*
 * Escapowanie znaków dla wygenerowanego
 * pola String w BuildConfig.
 */
val escapedMapTilerApiKey =
    mapTilerApiKey
        .replace(
            "\\",
            "\\\\"
        )
        .replace(
            "\"",
            "\\\""
        )

android {

    namespace =
        "com.example.tatratracker"

    compileSdk =
        36

    defaultConfig {

        applicationId =
            "com.example.tatratracker"

        minSdk =
            30

        targetSdk =
            36

        versionCode =
            1

        versionName =
            "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        /*
         * =================================================
         * MAPTILER BUILDCONFIG
         * =================================================
         */
        buildConfigField(
            "String",
            "MAPTILER_API_KEY",
            "\"$escapedMapTilerApiKey\""
        )
    }

    buildTypes {

        release {

            isMinifyEnabled =
                false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        // Samsung Health Data SDK wymaga Javy 17+.
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    buildFeatures {

        compose =
            true

        buildConfig =
            true
    }
}

/*
 * =========================================================
 * ROOM SCHEMA EXPORT
 * =========================================================
 *
 * Gradle 9 waliduje @InputDirectory jeszcze przed uruchomieniem KSP,
 * dlatego katalog musi istnieć już podczas konfiguracji projektu.
 */
val roomSchemaDirectory =
    File(
        projectDir,
        "schemas"
    ).apply {
        mkdirs()
    }

ksp {
    arg(
        RoomSchemaArgProvider(
            roomSchemaDirectory
        )
    )
}

dependencies {

    // =====================================================
    // ANDROID
    // =====================================================

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )

    // =====================================================
    // COMPOSE
    // =====================================================

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    // =====================================================
    // NAVIGATION
    // =====================================================

    implementation(
        "androidx.navigation:navigation-compose:2.7.7"
    )

    // =====================================================
    // COROUTINES
    // =====================================================

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0"
    )

    // =====================================================
    // VIEWMODEL
    // =====================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
    )

    // =====================================================
    // MAPLIBRE
    // =====================================================

    implementation(
        "org.maplibre.gl:android-sdk:13.4.0"
    )

    // =====================================================
    // ROOM
    // =====================================================

    val roomVersion =
        "2.8.4"

    implementation(
        "androidx.room:room-runtime:$roomVersion"
    )

    implementation(
        "androidx.room:room-ktx:$roomVersion"
    )

    ksp(
        "androidx.room:room-compiler:$roomVersion"
    )

    // =====================================================
    // GARMIN FIT
    // =====================================================

    implementation(
        "com.garmin:fit:21.205.0"
    )

    // =====================================================
    // SAMSUNG HEALTH DATA SDK (test)
    // =====================================================

    // Plik .aar pobierany ręcznie do app/libs/ (nie jest w repozytorium).
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("com.google.code.gson:gson:2.13.2")
    // SDK używa kotlinx.parcelize.Parceler w runtime. Przy wbudowanym Kotlinie AGP 9
    // wtyczka parcelize nie dodaje tej biblioteki sama. Wersja = Kotlin z AGP (2.2.10).
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:2.2.10")

    // =====================================================
    // TESTY
    // =====================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}