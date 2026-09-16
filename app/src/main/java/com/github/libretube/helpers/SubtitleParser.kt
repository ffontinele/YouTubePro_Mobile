package com.github.libretube.helpers

data class SubtitleCue(val start: Long, val end: Long, val text: String)

object SubtitleParser {

    fun parse(content: String): List<SubtitleCue> {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("{") -> parseJson3(trimmed)
            trimmed.startsWith("<") -> parseXml(trimmed)
            else -> parseVtt(trimmed)
        }
    }

    private fun clean(text: String): String =
        text.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&#39;", "'").replace("&quot;", "\"")
            .replace("\u00a0", " ")
            .trim()

    private fun parseJson3(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val obj = org.json.JSONObject(content)
        val events = obj.optJSONArray("events") ?: return cues
        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val start = ev.optLong("tStartMs", -1)
            val dur = ev.optLong("dDurationMs", 0)
            val segs = ev.optJSONArray("segs") ?: continue
            val sb = StringBuilder()
            for (j in 0 until segs.length()) {
                sb.append(segs.optJSONObject(j)?.optString("utf8", "") ?: "")
            }
            val text = clean(sb.toString().replace("\n", " "))
            if (start >= 0 && text.isNotEmpty()) cues.add(SubtitleCue(start, start + dur, text))
        }
        return cues
    }

    private fun parseVtt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val timeRegex = Regex("""(\d{2}:)?(\d{2}):(\d{2})[.,](\d{3})\s*-->\s*(\d{2}:)?(\d{2}):(\d{2})[.,](\d{3})""")
        val blocks = content.replace("\r\n", "\n").split("\n\n")
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            val timeLine = lines.firstOrNull { it.contains("-->") } ?: continue
            val m = timeRegex.find(timeLine) ?: continue
            fun ms(base: Int): Long {
                val h = m.groupValues[base].ifEmpty { "0" }.toLongOrNull() ?: 0
                val mi = m.groupValues[base + 1].toLongOrNull() ?: 0
                val s = m.groupValues[base + 2].toLongOrNull() ?: 0
                val ms = m.groupValues[base + 3].toLongOrNull() ?: 0
                return h * 3600000 + mi * 60000 + s * 1000 + ms
            }
            val text = clean(lines.dropWhile { !it.contains("-->") }.drop(1).joinToString(" "))
            if (text.isNotEmpty()) cues.add(SubtitleCue(ms(1), ms(5), text))
        }
        return cues
    }

    private fun parseXml(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val regex = Regex("""<p[^>]*\bt="(\d+)"[^>]*\bd="(\d+)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        for (m in regex.findAll(content)) {
            val start = m.groupValues[1].toLongOrNull() ?: continue
            val dur = m.groupValues[2].toLongOrNull() ?: 0
            val text = clean(m.groupValues[3].replace("\n", " "))
            if (text.isNotEmpty()) cues.add(SubtitleCue(start, start + dur, text))
        }
        return cues
    }
}
