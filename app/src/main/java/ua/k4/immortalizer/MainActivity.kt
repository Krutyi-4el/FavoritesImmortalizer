package ua.k4.immortalizer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import kotlin.concurrent.thread
import kotlin.io.path.Path

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.main_button)
        val textView = findViewById<TextView>(R.id.main_text)
        val optimizationSwitch = findViewById<SwitchCompat>(R.id.optimization_switch)
        val bootSwitch = findViewById<SwitchCompat>(R.id.boot_switch)

        val settings = Settings(Path(applicationContext.filesDir.path, "settings.json"))

        if (ImmortalizerService.isRunning) {
            // notify that it should continue running
            startImmortalizer()
            button.text = getString(R.string.main_button_stop)
            textView.text = getString(R.string.service_running)
        } else {
            button.text = getString(R.string.main_button_start)
            textView.text = getString(R.string.service_stopped)
        }

        button.setOnClickListener {
            if (!ImmortalizerService.isRunning) {
                startImmortalizer()
                thread {
                    val result = ImmortalizerService.getResult()
                    runOnUiThread {
                        if (result != null) {
                            stopImmortalizer()
                            textView.text = result
                        } else {
                            button.text = getString(R.string.main_button_stop)
                            textView.text = getString(R.string.success)
                        }
                    }
                }
            } else {
                stopImmortalizer()
                button.text = getString(R.string.main_button_start)
                textView.text = getString(R.string.service_stopped)
            }
        }

        optimizationSwitch.isChecked = settings.disableBatteryOptimization
        optimizationSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.disableBatteryOptimization = isChecked
        }
        bootSwitch.isChecked = settings.startOnBoot
        bootSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.startOnBoot = isChecked
        }
    }

    private fun startImmortalizer() {
        startService(Intent(applicationContext, ImmortalizerService::class.java))
    }

    private fun stopImmortalizer() {
        ImmortalizerService.isRunning = false
        stopService(Intent(applicationContext, ImmortalizerService::class.java))
    }
}
