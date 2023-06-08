package ua.k4.immortalizer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.isExecutable
import kotlin.io.path.notExists

class ImmortalizerService : Service() {
    private lateinit var pipeFile: Path

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

        isRunning = true
        thread { mainLoop() }
        return null
    }

    private fun mainLoop() {
        while (isRunning) {
            Thread.sleep(1000)
            Log.d("ImmortalizerService", "loop")
        }
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
