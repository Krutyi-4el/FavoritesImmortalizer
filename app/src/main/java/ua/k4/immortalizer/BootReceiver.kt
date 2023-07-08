package ua.k4.immortalizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.io.path.Path

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED || !Settings(
                Path(
                    context.filesDir.path,
                    "settings.json"
                )
            ).startOnBoot
        ) return

        context.startService(Intent(context, ImmortalizerService::class.java))
    }
}
