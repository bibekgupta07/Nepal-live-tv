package com.app.nepallivetv.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.net.toUri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.app.nepallivetv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UpdateManager(private val context: Context) {

    private val githubOwner = "bibekgupta07" 
    private val githubRepo = "Nepal-live-tv"

    private val apiService: GitHubApiService by lazy {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApiService::class.java)
    }

    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val latestRelease = apiService.getLatestRelease(githubOwner, githubRepo)
            val latestVersion = latestRelease.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v")

            if (isVersionGreater(latestVersion, currentVersion)) {
                val apkAsset = latestRelease.assets.firstOrNull { it.downloadUrl.endsWith(".apk") }
                if (apkAsset != null) {
                    return@withContext UpdateResult.UpdateAvailable(
                        version = latestRelease.tagName,
                        releaseNotes = latestRelease.body ?: "No release notes",
                        downloadUrl = apkAsset.downloadUrl
                    )
                }
            }
            UpdateResult.NoUpdate
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    fun downloadAndInstallUpdate(url: String) {
        val request = DownloadManager.Request(url.toUri())
            .setTitle("NepalLiveTv Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NepalLiveTv-update.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Register receiver for when download is complete
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val uri = downloadManager.getUriForDownloadedFile(id)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    sealed class UpdateResult {
        data class UpdateAvailable(
            val version: String,
            val releaseNotes: String,
            val downloadUrl: String
        ) : UpdateResult()
        object NoUpdate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}
