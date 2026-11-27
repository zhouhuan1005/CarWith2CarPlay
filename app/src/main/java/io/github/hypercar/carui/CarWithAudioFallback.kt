package io.github.hypercar.carui

import android.content.Context
import io.github.hypercar.session.HyperCarRepo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 补丁（CarWith2CarPlay）：CarWith 模式音频降级。
 *
 * 背景：CarWith 模式的音源是本机 CarLife 引擎 MEDIA 通道（9240）。实测小米 CarWith
 * 的媒体音频按设计留在手机本地出声（蓝牙 A2DP 或外放），MEDIA 通道全程零内容，
 * 车机侧只收到按时间线补的静音包。Rust 侧（carlife.rs handle_media_client）的
 * 看门狗发现「MEDIA 通道建立 15 秒 + 从未出现非静音 PCM」时发
 * `carwith_media_silent` 状态；通道出现首个非静音 PCM 时发 `carwith_media_live`。
 *
 * 本对象收到 silent → 用 Shizuku 系统采集（CarAudioCapture.startLegacy，单混音）
 * 兜底出声；收到 live → 停掉采集，交还引擎通道，避免双播。
 *
 * 边界（与上游 docs/AUDIO.md 的设计一致）：
 *  - 不改变 CarWith 模式"无需 Shizuku"的默认形态：Shizuku 未就绪时只记一次日志，
 *    不重试刷屏；
 *  - 采集走的是 self-contained 的 CarAudioCapture，不动引擎与 AirPlay 输出链路；
 *  - 双播窗口：live 事件由首个非静音 PCM 触发，停采集前可能有亚秒级重叠，可接受。
 */
object CarWithAudioFallback {
    private val attempted = AtomicBoolean(false)
    private val ownedCapture = AtomicBoolean(false)

    /** 引擎 MEDIA 通道持续零内容：降级开系统采集。每轮会话只尝试一次。 */
    fun onSilent(context: Context, detail: String) {
        if (!attempted.compareAndSet(false, true)) return
        HyperCarRepo.log("CARWITH-FALLBACK: $detail → MEDIA 通道零内容，降级 Shizuku 系统采集（CarWith2CarPlay 补丁）")
        // 采集运行中手机媒体不能再走蓝牙 A2DP（否则车机双播）；HFP 通话不受影响。
        io.github.hypercar.bluetooth.CarProfileGuard.setA2dpOnly(true)
        runCatching { CarAudioCapture.startLegacy(context.applicationContext) }
            .onSuccess {
                ownedCapture.set(true)
                HyperCarRepo.log("CARWITH-FALLBACK: 系统采集已启动（legacy 单混音，PCM48k stereo）")
            }
            .onFailure {
                HyperCarRepo.log("CARWITH-FALLBACK: 采集启动失败 ${it.message}（Shizuku 未就绪则无兜底，后续日志会持续报 silent）")
            }
    }

    /** 引擎通道出现非静音 PCM：停掉降级采集，交还引擎。 */
    fun onLive(detail: String) {
        if (!attempted.getAndSet(false)) return
        HyperCarRepo.log("CARWITH-FALLBACK: $detail → 引擎通道恢复内容，停用降级采集")
        io.github.hypercar.bluetooth.CarProfileGuard.setA2dpOnly(false)
        if (ownedCapture.getAndSet(false)) CarAudioCapture.stop()
    }

    /** 会话收尾：无论当前归谁，清状态并停掉由本补丁启动的采集。 */
    fun reset() {
        attempted.set(false)
        io.github.hypercar.bluetooth.CarProfileGuard.setA2dpOnly(false)
        if (ownedCapture.getAndSet(false)) runCatching { CarAudioCapture.stop() }
    }
}
