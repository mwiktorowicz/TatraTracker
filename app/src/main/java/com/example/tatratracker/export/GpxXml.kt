package com.example.tatratracker.export

import java.util.Locale

/** Dodatkowa przestrzeń nazw w nagłówku GPX (np. rozszerzenie z tętnem). */
internal data class GpxNamespace(
    val prefix: String,
    val uri: String,
    val schemaLocation: String
)

/** Elementy wspólne dla wszystkich plików GPX 1.1 zapisywanych przez aplikację. */
internal object GpxXml {

    const val XML_DECLARATION = """<?xml version="1.0" encoding="UTF-8"?>"""

    private const val GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1"
    private const val GPX_SCHEMA_LOCATION = "http://www.topografix.com/GPX/1/1/gpx.xsd"

    /** Rozszerzenie Garmina: tętno w punktach śladu (`gpxtpx:hr`). */
    val TRACK_POINT_EXTENSION = GpxNamespace(
        prefix = "gpxtpx",
        uri = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1",
        schemaLocation = "http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd"
    )

    /** Otwierający znacznik `<gpx …>` bez znaku nowej linii na końcu. */
    fun openGpxTag(extraNamespaces: List<GpxNamespace> = emptyList()): String {
        val schemaLocations =
            (listOf(GPX_NAMESPACE to GPX_SCHEMA_LOCATION) +
                    extraNamespaces.map { it.uri to it.schemaLocation })
                .joinToString(separator = " ") { (uri, location) -> "$uri $location" }

        return buildString {
            appendLine("<gpx")
            appendLine("    version=\"1.1\"")
            appendLine("    creator=\"TatraTracker\"")
            appendLine("    xmlns=\"$GPX_NAMESPACE\"")
            appendLine("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
            extraNamespaces.forEach { namespace ->
                appendLine("    xmlns:${namespace.prefix}=\"${namespace.uri}\"")
            }
            append("    xsi:schemaLocation=\"$schemaLocations\">")
        }
    }

    fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.7f", value)
    }

    fun formatElevation(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    /**
     * Bezpieczna nazwa pliku (bez rozszerzenia): małe litery, bez polskich znaków,
     * pozostałe znaki zamienione na „_”, najwyżej 80 znaków.
     */
    fun fileNameBase(text: String, fallback: String): String {
        val cleanName =
            text
                .trim()
                .lowercase(Locale.getDefault())
                .replace("ą", "a")
                .replace("ć", "c")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ń", "n")
                .replace("ó", "o")
                .replace("ś", "s")
                .replace("ź", "z")
                .replace("ż", "z")
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(80)

        return cleanName.ifBlank { fallback }
    }
}
