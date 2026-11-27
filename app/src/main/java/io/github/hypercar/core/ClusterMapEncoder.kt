package io.github.hypercar.core

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * 仪表盘地图投射（cluster map）——CarWith2CarPlay 精简版已停用。
 *
 * 该功能依赖自绘 UI 的 MapPreview 渲染链与高德导航联动，随自绘车机 UI 一并移除。
 * 保留类与 JNI 方法签名（RustCoreBridge 会被 Rust 调用），但永远返回不可用，
 * Rust 拿不到编码器后自然走无仪表投射路径。
 */
class ClusterMapEncoder private constructor() : AutoCloseable {
    companion object {
        internal fun capable(): Boolean = false

        internal fun available(width: Int, height: Int): Boolean {
            if (!availableInternal()) return false
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            return runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format) != null }
                .getOrDefault(false)
        }

        private fun availableInternal(): Boolean = false

        internal fun open(w: Int, h: Int, x: Int, y: Int, sw: Int, sh: Int, fps: Int): ClusterMapEncoder? = null
    }

    fun firstSample(): ByteArray? = null
    fun clearSample(): ByteArray? = null
    fun nextSample(sync: Boolean): ByteArray? = null

    override fun close() {}
}
