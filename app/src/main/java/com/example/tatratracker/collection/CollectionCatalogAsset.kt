package com.example.tatratracker.collection

import android.content.Context

/** Katalog kolekcji z pliku aplikacji – wspólny dla ekranu „Kolekcja” i podsumowania wędrówki. */
object CollectionCatalogAsset {

    private const val ASSET_NAME = "kolekcja.json"

    /** Czyta z dysku – wołać poza wątkiem głównym. Rzuca przy braku lub uszkodzeniu pliku. */
    fun load(context: Context): CollectionCatalog {
        val json =
            context.assets
                .open(ASSET_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

        return CollectionCatalogParser.parse(json)
    }
}
