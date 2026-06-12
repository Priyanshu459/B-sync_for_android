package com.example.bsyncbrowser.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Priyanshu459/B-sync_for_android/releases/latest"

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val body = json.optString("body", "No release notes provided.")
                
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val urlStr = asset.getString("browser_download_url")
                    if (urlStr.endsWith(".apk")) {
                        downloadUrl = urlStr
                        break
                    }
                }

                if (downloadUrl.isNotEmpty()) {
                    // Convert version strings to integers for comparison (e.g., "1.0.1" -> 101, "v2.0" -> 20)
                    val currentClean = currentVersionName.replace(Regex("[^0-9]"), "").padEnd(3, '0').toIntOrNull() ?: 0
                    val latestClean = tagName.replace(Regex("[^0-9]"), "").padEnd(3, '0').toIntOrNull() ?: 0
                    
                    val isUpdateAvailable = latestClean > currentClean

                    return@withContext UpdateInfo(
                        isUpdateAvailable = isUpdateAvailable,
                        latestVersion = tagName,
                        releaseNotes = body,
                        downloadUrl = downloadUrl
                    )
                }
            } else {
                Log.e(TAG, "Failed to check for updates. HTTP Code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}")
        }
        return@withContext null
    }
}
