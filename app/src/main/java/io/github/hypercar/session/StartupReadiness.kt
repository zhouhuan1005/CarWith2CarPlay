package io.github.hypercar.session

/** Shared by onboarding, buttons and every manual/background connection entry. */
data class StartupReadiness(
    val source: String?,
    val missingPermissions: List<String> = emptyList(),
    val bluetoothEnabled: Boolean = true,
    val wifiEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val shizukuReady: Boolean = false,
    val carWithInstalled: Boolean = false,
    val overlayAllowed: Boolean = false,
) {
    val manualIssues: List<String> get() = buildList {
        addAll(missingPermissions)
        if (!bluetoothEnabled) add("开启手机蓝牙")
        if (!wifiEnabled) add("开启手机 Wi-Fi")
        if (!locationEnabled) add("开启系统定位服务")
        // 恒定 CarWith：任何 source 值（含历史遗留 null/"car_ui"）都按 CarWith 要求判。
        if (!carWithInstalled) add("安装并启用系统 CarWith（com.miui.carlink）")
    }
    val automaticIssues: List<String> get() = manualIssues +
        if (overlayAllowed) emptyList() else listOf("允许悬浮窗权限，以便上车自动唤醒")
    val manualReady: Boolean get() = manualIssues.isEmpty()
    val automaticReady: Boolean get() = automaticIssues.isEmpty()
    /** The standby listener can wait for the Bluetooth adapter to turn on. */
    val automaticStandbyIssues: List<String> get() =
        if (bluetoothEnabled) automaticIssues else copy(bluetoothEnabled = true).automaticIssues
}
