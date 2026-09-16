package com.github.libretube.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Serviço de tradução via LibreTranslate (API pública gratuita)
 */
object TranslationService {
    // Instâncias públicas do LibreTranslate (fallback se uma falhar)
    private val endpoints = listOf(
        "https://libretranslate.de",
        "https://translate.argosopentech.com",
        "https://translate.terraprint.co"
    )
    
    private val cache = mutableMapOf<String, String>()
    
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String? {
        val cacheKey = "${sourceLang}_${targetLang}_${text.hashCode()}"
        cache[cacheKey]?.let { return it }
        
        return withContext(Dispatchers.IO) {
            for (endpoint in endpoints) {
                try {
                    val result = callApi(endpoint, text, sourceLang, targetLang)
                    if (result != null) {
                        cache[cacheKey] = result
                        return@withContext result
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            null
        }
    }
    
    private fun callApi(endpoint: String, text: String, source: String, target: String): String? {
        val url = URL("$endpoint/translate")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        
        val body = JSONObject().apply {
            put("q", text)
            put("source", source)
            put("target", target)
        }
        
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        
        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            return json.getString("translatedText")
        }
        return null
    }
}
