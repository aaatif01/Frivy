package com.frivy.assistant.device
import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.view.Window
import androidx.core.content.getSystemService
import com.frivy.assistant.MainActivity
import org.json.JSONObject

class DeviceActionExecutor(private val context: Context) {
    fun execute(name: String, args: String): String {
        val json = runCatching { JSONObject(args) }.getOrDefault(JSONObject())
        return runCatching {
            when (name) {
                "open_settings" -> launch(Intent(Settings.ACTION_SETTINGS))
                "open_wifi" -> launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                "open_bluetooth" -> launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                "open_mobile_data" -> launch(Intent(Settings.ACTION_DATA_USAGE_SETTINGS))
                "open_notifications" -> launch(Intent("android.settings.NOTIFICATION_LISTENER_SETTINGS"))
                "open_quick_settings" -> (context as? MainActivity)?.openQuickSettings()
                "close_app" -> launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                "open_camera" -> launch(Intent(MediaStoreIntents.CAMERA))
                "open_gallery" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media")))
                "open_contacts" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people")))
                "open_phone" -> launch(Intent(Intent.ACTION_DIAL))
                "open_messages" -> launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
                "open_app" -> {
                    val pkg = json.optString("packageName")
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: error("App is not installed")
                    launch(intent)
                }
                "copy_to_clipboard" -> { context.getSystemService<android.content.ClipboardManager>()?.setPrimaryClip(android.content.ClipData.newPlainText("FRIVY", json.optString("text"))); "Copied to clipboard." }
                "toggle_flashlight" -> { context.getSystemService<android.hardware.camera2.CameraManager>()?.setTorchMode(context.getSystemService<android.hardware.camera2.CameraManager>()!!.cameraIdList.first(), json.optBoolean("enabled")); "Flashlight updated." }
                "set_brightness" -> {
                    if (!Settings.System.canWrite(context)) { launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))); "Brightness permission is required. I opened its permission screen." }
                    else { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (json.optInt("percent", 50).coerceIn(1,100) * 255) / 100); "Brightness adjusted." }
                }
                "set_volume" -> { val audio = context.getSystemService<AudioManager>()!!; val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); audio.setStreamVolume(AudioManager.STREAM_MUSIC, max * json.optInt("percent", 50).coerceIn(0,100) / 100, 0); "Media volume adjusted." }
                "toggle_wifi", "toggle_bluetooth" -> { launch(Intent(if (name == "toggle_wifi") Settings.ACTION_WIFI_SETTINGS else Settings.ACTION_BLUETOOTH_SETTINGS)); "Android requires manual confirmation for this setting, so I opened the correct controls." }
                else -> error("Unsupported action")
            }
            "Action completed."
        }.getOrElse { "I couldn't complete that action: ${it.message ?: "permission was denied"}." }
    }
    private fun launch(intent: Intent) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
    private object MediaStoreIntents { const val CAMERA = "android.media.action.IMAGE_CAPTURE" }
}