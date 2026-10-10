package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogBrowseTest {
    @Test fun `artist results retain their browse identity without needing a song video id`() {
        val artists = parseCatalogSearch(catalogResponse, CatalogKind.Artist)

        assertEquals(listOf("UCweeknd"), artists.map { it.id })
        assertEquals("The Weeknd", artists.single().title)
        assertEquals("40.1M subscribers", artists.single().subtitle)
        assertEquals("https://images.example/artist.jpg", artists.single().artworkUri)
        assertEquals(CatalogKind.Artist, artists.single().kind)
    }

    @Test fun `album results retain the MPRE browse identity instead of becoming song groups`() {
        val albums = parseCatalogSearch(catalogResponse, CatalogKind.Album)

        assertEquals(listOf("MPREafterhours"), albums.map { it.id })
        assertEquals("After Hours", albums.single().title)
        assertEquals("The Weeknd • 2020", albums.single().subtitle)
        assertEquals("https://images.example/album.jpg", albums.single().artworkUri)
        assertEquals(CatalogKind.Album, albums.single().kind)
    }

    @Test fun `song metadata links do not masquerade as artist or album search results`() {
        val songOnly = """{"contents":[{"musicResponsiveListItemRenderer":{
            "playlistItemData":{"videoId":"song-video"},
            "navigationEndpoint":{"watchEndpoint":{"videoId":"song-video"}},
            "flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Blinding Lights"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                    {"text":"The Weeknd","navigationEndpoint":{"browseEndpoint":{"browseId":"UCweeknd"}}},
                    {"text":" • "},
                    {"text":"After Hours","navigationEndpoint":{"browseEndpoint":{"browseId":"MPREafterhours"}}}
                ]}}}
            ]
        }}]}"""

        assertEquals(emptyList<CatalogItem>(), parseCatalogSearch(songOnly, CatalogKind.Artist))
        assertEquals(emptyList<CatalogItem>(), parseCatalogSearch(songOnly, CatalogKind.Album))
    }

    private val catalogResponse = """
        {"contents":{"tabbedSearchResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                    "navigationEndpoint":{"browseEndpoint":{"browseId":"UCweeknd",
                        "browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{
                            "pageType":"MUSIC_PAGE_TYPE_ARTIST"
                        }}
                    }},
                    "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
                        {"url":"https://images.example/artist.jpg"}
                    ]}}},
                    "flexColumns":[
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{
                            "text":"The Weeknd","navigationEndpoint":{"browseEndpoint":{"browseId":"UCweeknd"}}
                        }]}}},
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"40.1M subscribers"}]}}}
                    ]
                }},
                {"musicResponsiveListItemRenderer":{
                    "navigationEndpoint":{"browseEndpoint":{"browseId":"MPREafterhours",
                        "browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{
                            "pageType":"MUSIC_PAGE_TYPE_ALBUM"
                        }}
                    }},
                    "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
                        {"url":"https://images.example/album.jpg"}
                    ]}}},
                    "flexColumns":[
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{
                            "text":"After Hours","navigationEndpoint":{"browseEndpoint":{"browseId":"MPREafterhours"}}
                        }]}}},
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                            {"text":"The Weeknd"},{"text":" • "},{"text":"2020"}
                        ]}}}
                    ]
                }},
                {"musicResponsiveListItemRenderer":{
                    "playlistItemData":{"videoId":"unrelated-song"},
                    "navigationEndpoint":{"watchEndpoint":{"videoId":"unrelated-song"}},
                    "flexColumns":[
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Weekend Song"}]}}},
                        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Other Artist"}]}}}
                    ]
                }}
            ]}}]}
        }}}]}}}
    """.trimIndent()
}
