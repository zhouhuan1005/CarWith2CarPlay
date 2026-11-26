# CarWith 模式音频降级补丁设计

> 本文说明 CarWith2CarPlay 相对上游 HyperCar 的核心差异：CarWith 模式下"车机无声"的根因，
> 以及音频降级补丁的触发判据、数据链路与实测证据。

## 一、现象

CarWith 模式连车后：画面正常、触摸正常、车机**无声**；声音从手机扬声器/蓝牙出。
多轮实测（2026-11-23 ~ 11-25，小米 17 Pro Max / HyperOS 4.0.6.0 / 奥迪 Q3 MIB3 4318）中
MEDIA/TTS/VR 三个通道全程零内容：

```
CARWITH-CHANNEL: MEDIA reason=periodic rxBytes=0 frames=0 pcmBytes=0 nonzeroSamples=0 … 无内容
startup_silence_recoveries=…（混音器只发启动静音）
firstMediaMonoNs=NA（136 次采样全为 NA）
```

关键判据：日志中**从未出现** `CARLIFE: MEDIA 音频初始化`——CarWith 连 `MEDIA_INIT`
都没发过，不是"推了静音"，而是"根本不使用 CarLife 引擎的音频通道"。

## 二、根因

小米 CarWith 的媒体音频按设计留在手机本地（蓝牙 A2DP 或外放），互联通道只承载
视频/触摸/TTS/VR。旁证：

- [CarWithEnhance](https://github.com/ayuanStear/CarWithEnhance) 的反编译审计显示 CarWith
  媒体链路全部在手机内部（`MediaController.Callback → na.c → pa.b → la.d → BaseMusicView`），
  不存在"把媒体音频发给车机"的路径；
- 关闭车机蓝牙「媒体音频」后 CarWith 直接走手机外放，互联通道依旧空；
- HyperCar（扮演车机）会主动断开车机 HFP/A2DP（`BT-GUARD`），进一步把声音压回手机扬声器。

结论：CarWith 引擎不发音频是**设计使然**，无法通过协议侧"骗"它发（`CMD_HU_INFO` 按百度
官方规范无音频能力位，`HU_FEATURE_CONFIG_RESPONSE` 亦无可翻转的音频开关）。

## 三、补丁方案：采集兜底

既然音源必然在手机本地，就把手机系统混音抓下来推给车机——与自绘 UI 模式同一套采集代码
（`CarAudioCapture`，Shizuku shell 身份的 AudioPolicy 回环采集）：

```
手机任意 App 播放 → 系统混音 → [Shizuku 采集] → nativePushAudioPcm
  → Rust 混音器 →（PCM 或 AAC 编码）→ AirPlay → 车机
```

### 触发与退出

| 事件 | 触发条件 | 动作 |
|---|---|---|
| `carwith_media_silent` | MEDIA 通道建立 15 秒且从未出现非静音 PCM（一次性） | Kotlin 启动 Shizuku 系统采集 |
| `carwith_media_live` | 通道出现首个非静音 PCM | 停采集，避免双播 |
| 会话收尾 | 任意断开路径 | `CarWithAudioFallback.reset()` 清理 |

判据细节：
- **不能以 `AudioInit` 为"活"判据**：引擎可能只发 INIT 不发 PCM；
- **以 `channel_stats.pcm()` 的 firstNonSilentPCM 标记为唯一"活"判据**：INIT 后仍可能全是静音；
- **看门狗不受 `diagnostic_phase.streaming()` 门控**：该相位若未置位会导致永不触发，
  而现场"连 MEDIA_INIT 都不发"恰是最需要降级的时机；
- 每轮会话只尝试一次降级（`AtomicBoolean` 去重），Shizuku 未就绪时只记一条日志不刷屏。

### 上下文传递的关键坑

`call_listener` 在拿不到 `callback_context` task-local 时对 `onState` 直接丢弃
（"无主状态不得终止当前会话"规则，`lib.rs`）。CarWith 七通道任务经
`tokio::spawn` / `JoinSet::spawn` 创建，**不继承** task-local；补丁的 `emit_state`
最初因此全部被静默吞掉。修复：拨号任务与七通道子任务统一套 `callback_context::inherit(...)`。
（`emit_log` 走 `onLog`，无上下文时回退全局 LISTENER，所以日志一直正常——这就是
"日志有、状态无"假象的来源，排查时注意。）

### 第二层坑：CarWith 路径根本没有作用域（2026-11-26 定谳）

inherit 修复后实车复测（v1.0.0.0，2026-11-25 晚）仍然一条状态事件都没到 Kotlin。
证据链：会话跑了数分钟、`CARLIFE: → VIDEO 心跳` 连续到 #210（说明七通道任务全部
活着、MEDIA 客户端任务在跑），`carwith_media_silent` 却从未出现。

真正的根因：**CarWith 路径的 JNI 入口就缺作用域**。调用链是
`nativePrepareCarWith → carwith_source::prepare → CarLifeHub::connect`，
而 `sync_scope` 只包在 `nativeStart`（AirPlay 接收端路径）里——CarWith 预热路径
的 task-local 是 `None`，拨号任务里 `inherit` 捕获到的就是 `None`，七通道子任务
随之全部无主。v1 的"上下文丢失"其实在这一层，v2/v3 修的是其下游。

修复（v1.0.0.1）：`lib.rs` 新增 `ensure_callback_context()`——有作用域用作用域，
没有则从全局 LISTENER 兜底构造 context；`CarLifeHub::connect_to_ports` 在 spawn
拨号任务前包一层 `sync_scope`。

### 第三层坑：冻结的 context listener 会被 owner 门拦截（2026-11-26 定谳）

v1.0.0.2 实车复测（2026-11-26 17:44 日志）：会话正常、MEDIA 任务活着
（`CARWITH-CHANNEL: MEDIA reason=periodic` 每 30 秒一条，证明 tick 分支在跑），
`carwith_media_silent` 仍未到达。同时 AirPlay 引擎自身的
`CORE: [negotiating]/[activation]` 状态全部正常——onState 通道本身没问题。

根因：全局监听器随引擎代际不断更换（每次 `nativeStart` 都 setListener 新的
`EngineEventListener(owner=N)`），而 CarWith 七通道源在 `nativePrepareCarWith`
时刻把 context 里的 listener **冻结**为当时的引擎监听器。看门狗 15 秒后触发时，
该 owner 往往已被新引擎顶替，`EngineEventListener.onState` 的 owner 门把状态
静默拦截（且拦截日志有 ≤8 条上限，超限后完全无痕）。时序证据：prepare 在
17:31:44（owner=3 引擎已启动），看门狗 17:32 左右触发，投递链早已换代。

修复（v1.0.0.3，双层）：
- Rust：`emit_state` 对 `carwith_media_silent` / `carwith_media_live` 不走冻结
  context，直接解析投递时刻的全局监听器（现行引擎的 listener）；
- Kotlin：`EngineEventListener.onState` 对这两个状态绕过 owner 门，无论本
  listener 是否仍对应现行引擎都送达 `rustListener`。

## 四、实测证据

- 0.1.0.619（仅看门狗）：MEDIA 零内容 6 分钟，`carwith_media_silent` 从未出现 → 定位到上下文丢失；
- 0.1.0.620（inherit 修复）：未上车复测；
- 0.1.0.621 / CarWith2CarPlay 1.0.0.0（inherit + 门控外置 + 事件去重）：实车复测
  （2026-11-25 晚）看门狗仍未触发 → 定位到 CarWith 路径顶层缺 sync_scope；
- CarWith2CarPlay 1.0.0.1（兜底 context + FGS 崩溃修复）：待实车验证（预期日志
  `CORE: [carwith_media_silent] elapsedSec=15` → `CARWITH-FALLBACK: 系统采集已启动` → 车机出声）。

## 五、已知边界

1. 手机外放与车机会同时出声（采集的是系统混音，不会自动静音手机）；
2. Shizuku 未授权时补丁静默失效（日志可辨）；
3. 若小米未来版本让 CarWith 走互联通道音频，`carwith_media_live` 会自动让位，无需干预；
4. 采集引入约几十毫秒系统延迟，与视频链路（~100ms 量级）基本同量级，实测声画同步可接受。
