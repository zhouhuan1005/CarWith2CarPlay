package io.github.hypercar.carui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 音乐桥（iAP2 NowPlaying）使用的偏好（CarWith2CarPlay 精简版）。
 *
 * 自绘车机 UI 移除后，这里只保留媒体桥需要的字段：默认音乐 App 与连车自动播放。
 * 原地图/壁纸/主题/投屏应用等字段随自绘 UI 一并移除。
 */
object CarUiSettings {
    private const val PREFS = "car_ui_settings"

    private var prefs: android.content.SharedPreferences? = null

    var defaultMusicPackage: String by mutableStateOf("")
        private set
    var autoPlayOnConnect: Boolean by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        defaultMusicPackage = p.getString("default_music", "").orEmpty()
        autoPlayOnConnect = p.getBoolean("auto_play", false)
    }

    fun updateDefaultMusicPackage(packageName: String) {
        defaultMusicPackage = packageName
        prefs?.edit()?.putString("default_music", packageName)?.apply()
    }

    fun updateAutoPlayOnConnect(value: Boolean) {
        autoPlayOnConnect = value
        prefs?.edit()?.putBoolean("auto_play", value)?.apply()
    }

    /** 车机日夜信号：自绘 UI 已移除，仅保留状态位供日志/主题查询。 */
    var carNight: Boolean? by mutableStateOf(null)
        private set

    fun onCarNight(night: Boolean) {
        carNight = night
    }
}
