package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser

internal enum class CatalogKind { Artist, Album, Playlist }
internal data class CatalogItem(val id: String, val title: String, val subtitle: String,
    val artworkUri: String, val kind: CatalogKind) {
    fun artworkTrack() = Track(title = title, artist = subtitle, album = "", uri = "catalog:$id", artworkUri = artworkUri)
}
internal data class CatalogPage(val item: CatalogItem, val songs: List<Track>,
    val albums: List<CatalogItem>, val description: String)

private fun catalogObjects(value: Any?): Sequence<JsonObject> = sequence {
    when (value) {
        is JsonObject -> { yield(value); value.values.forEach { yieldAll(catalogObjects(it)) } }
        is List<*> -> value.forEach { yieldAll(catalogObjects(it)) }
    }
}
private fun textRuns(value: JsonObject?): String = value?.getArray("runs")
    ?.joinToString("") { (it as? JsonObject)?.getString("text", "").orEmpty() }
    ?.ifBlank { value.getString("simpleText", "") }.orEmpty()
private fun columnText(renderer: JsonObject, index: Int): JsonObject? =
    (renderer.getArray("flexColumns").getOrNull(index) as? JsonObject)
        ?.getObject("musicResponsiveListItemFlexColumnRenderer")?.getObject("text")

internal fun parseCatalogSearch(json: String, kind: CatalogKind): List<CatalogItem> =
    catalogObjects(JsonParser.`object`().from(json)).mapNotNull { wrapper ->
        val renderer = wrapper.getObject("musicResponsiveListItemRenderer", null)
            ?: wrapper.getObject("musicTwoRowItemRenderer", null) ?: return@mapNotNull null
        // Only the title's endpoint identifies the result; artist links on songs are metadata.
        if (renderer.getObject("playlistItemData").getString("videoId", "").isNotBlank()) return@mapNotNull null
        val titleObject = renderer.getObject("title", null) ?: columnText(renderer, 0)
        val endpoint = renderer.getObject("navigationEndpoint", null)
            ?: (titleObject?.getArray("runs")?.firstOrNull() as? JsonObject)?.getObject("navigationEndpoint")
        val id = endpoint?.getObject("browseEndpoint")?.getString("browseId", "").orEmpty()
        val matches = when(kind) {
            CatalogKind.Artist -> id.startsWith("UC")
            CatalogKind.Album -> id.startsWith("MPRE")
            CatalogKind.Playlist -> id.startsWith("VL")
        }
        val title = textRuns(titleObject)
        if (!matches || title.isBlank()) return@mapNotNull null
        val subtitle = textRuns(renderer.getObject("subtitle", null) ?: columnText(renderer, 1))
        val image = catalogObjects(renderer.getObject("thumbnail", null) ?: renderer.getObject("thumbnailRenderer", null))
            .map { it.getString("url", "") }.lastOrNull { it.startsWith("https:") }.orEmpty()
        CatalogItem(id, title, subtitle, highResolutionArtworkUrl(image), kind)
    }.distinctBy(CatalogItem::id).take(60).toList()

internal fun parseCatalogPage(json: String, item: CatalogItem): CatalogPage {
    val root = JsonParser.`object`().from(json)
    val description = catalogObjects(root).mapNotNull { it.getObject("musicDescriptionShelfRenderer", null) }
        .map { textRuns(it.getObject("description", null)) }.firstOrNull().orEmpty()
    val header = catalogObjects(root).mapNotNull {
        it.getObject("musicImmersiveHeaderRenderer", null) ?: it.getObject("musicResponsiveHeaderRenderer", null)
            ?: it.getObject("musicDetailHeaderRenderer", null)
    }.firstOrNull()
    val image = catalogObjects(header?.getObject("thumbnail", null))
        .map { it.getString("url", "") }.lastOrNull { it.startsWith("https:") }.orEmpty()
    val songs = parseInnertubeSearch(json).map { track ->
        track.copy(artist = track.artist.takeUnless { it == "YouTube Music" }
            ?: if (item.kind == CatalogKind.Artist) item.title else item.subtitle.substringBefore(" • "),
            album = if(item.kind == CatalogKind.Album) item.title else track.album,
            artworkUri = track.artworkUri.ifBlank { image.ifBlank { item.artworkUri } })
    }
    return CatalogPage(item.copy(artworkUri = image.ifBlank { item.artworkUri }), songs,
        parseCatalogSearch(json, CatalogKind.Album), description)
}
