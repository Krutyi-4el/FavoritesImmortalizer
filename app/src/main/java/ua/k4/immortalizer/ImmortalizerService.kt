package ua.k4.immortalizer

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.isExecutable
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class ImmortalizerService : Service() {
    private val favorites = Uri.parse("content://settings/system/recent_panel_favorites")
    private lateinit var pipeFile: Path
    private var favoritesString: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        pipeFile = Path(applicationContext.filesDir.path, "pipe")
        thread { setResult(checkAndRun()) }
    }

    private fun checkAndRun(): String? {
        val systemPath = System.getenv("PATH")?.split(":") ?: return getString(R.string.no_path)
        arrayOf("su", "sh", "mkfifo").filter { program ->
            !systemPath.any { directory ->
                Path(
                    directory, program
                ).isExecutable()
            }
        }.let {
            if (it.isNotEmpty()) return getString(R.string.no_programs) + it.joinToString(prefix = " ")
        }

        if (pipeFile.notExists()) ProcessBuilder("mkfifo", pipeFile.toString()).start().waitFor()
            .let {
                if (it != 0) return getString(R.string.mkfifo_failed)
            }

        val pipeCheck = ProcessBuilder("sh", "-c", "echo true > '$pipeFile'").start()
        if (!pipeCheck.waitFor(1, TimeUnit.SECONDS)) {
            pipeCheck.destroy()
            val su = ProcessBuilder(
                "su", "-c", "id -u; while true; do eval \$(cat '$pipeFile' || echo break); done"
            ).start()
            if (su.inputStream.bufferedReader().readLine() != "0") {
                su.destroy()
                return getString(R.string.no_root)
            }
        }

        thread {
            Looper.prepare()
            contentResolver.registerContentObserver(
                favorites, true, FavoritesObserver(
                    this, Handler(Looper.myLooper()!!)
                )
            )
            Looper.loop()
        }
        updateFavorites()

        isRunning = true
        thread { mainLoop() }
        return null
    }

    private fun mainLoop() {
        while (isRunning) {
            Thread.sleep(1000)
            favoritesString?.let {
                pipeFile.writeText("for name in $it; do for pid in \$(pidof \$name); do echo '-1000' > /proc/\$pid/oom_score_adj; done; done")
            }
        }
    }

    private fun readFavorites(): String? {
        val cursor = contentResolver.query(
            favorites,
            arrayOf("value"),
            null,
            null,
            null,
        ) ?: return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        return cursor.getString(0)
    }

    fun updateFavorites() {
        val favorites = readFavorites() ?: return
        favoritesString = favorites.split("|").map { it.substringAfter(":").substringBefore("/") }
            .plus(packageName).joinToString(" ")
    }

    companion object {
        var isRunning = false
        private var result = CompletableFuture<String?>()

        fun getResult(): String? {
            val res = result.get()
            result = CompletableFuture()
            return res
        }

        private fun setResult(value: String?) {
            result.complete(value)
        }
    }
}

class FavoritesObserver(private val service: ImmortalizerService, h: Handler) : ContentObserver(h) {
    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        service.updateFavorites()
    }
}
