package ua.k4.immortalizer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // notify that it should continue running
        if (ImmortalizerService.isRunning) startImmortalizer()

        val button = findViewById<Button>(R.id.main_button)
        button.text =
            getString(if (ImmortalizerService.isRunning) R.string.main_button_stop else R.string.main_button_start)
        button.setOnClickListener {
            if (!ImmortalizerService.isRunning) {
                startImmortalizer()
                thread {
                    val result = ImmortalizerService.getResult()
                    runOnUiThread {
                        val textView = findViewById<TextView>(R.id.main_text)
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
            }
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
