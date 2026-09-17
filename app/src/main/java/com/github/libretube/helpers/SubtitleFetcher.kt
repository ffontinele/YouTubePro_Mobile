package com.github.libretube.helpers

import java.net.HttpURLConnection
import java.net.URL

object SubtitleFetcher {

    data class Track(val code: String, val name: String, val url: String, val auto: Boolean)

    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun fetchTracks(videoId: String): List<Track> {
        val conn = URL("https://www.youtube.com/watch?v=$videoId&hl=en").openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", UA)
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        conn.setRequestProperty("Cookie", "CONSENT=YES+cb; SOCS=CAI")
        val html = conn.inputStream.bufferedReader().readText()
        val marker = "\"captionTracks\":"
        val idx = html.indexOf(marker)
        if (idx < 0) return emptyList()
        val start = html.indexOf('[', idx)
        if (start < 0) return emptyList()
        var depth = 0
        var end = start
        while (end < html.length) {
            val c = html[end]
            if (c == '[') depth++
            else if (c == ']') {
                depth--
                if (depth == 0) break
            }
            end++
        }
        val arr = org.json.JSONArray(html.substring(start, end + 1))
        val tracks = mutableListOf<Track>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val base = o.optString("baseUrl", "")
            if (base.isEmpty()) continue
            val runs = o.optJSONObject("name")?.optJSONArray("runs")
            val name = runs?.optJSONObject(0)?.optString("text") ?: o.optString("languageCode")
            val auto = o.optString("kind") == "asr"
            tracks.add(Track(o.optString("languageCode"), name, base, auto))
        }
        return tracks
    }
}
