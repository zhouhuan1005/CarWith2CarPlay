# CarWith2CarPlay

**CarWith2CarPlay** 是一个面向 **小米手机 × 奥迪（MIB3）等 CarPlay 车机** 的开源互联方案：把小米 CarWith 的画面（高德地图车机版等）通过无线 CarPlay 投到车机屏幕，触摸可反控，并通过 **音频降级补丁** 解决 CarWith 模式"车机无声"的问题。无需转接盒、无需 root。

> 本项目是 [fw867/hypercar](https://github.com/fw867/hypercar) 的衍生发行版（GPLv3），在其基础上加入了
> **CarWith 音频降级补丁**、** Audi MIB3 实测调优说明**与中文文档，详见下文[与上游的差异](#与上游-hypercar-的差异)。
> 上游的多品牌车机档案已继承。**自 v1.1.0.0 起，本发行版移除了自绘车机 UI，恒定走 CarWith 画面来源**，
> 代码与功能说明以此为准；需要自绘 UI 请使用上游 fw867/hypercar。

[下载 APK](https://github.com/zhouhuan1005/CarWith2CarPlay/releases/latest) · [使用与开发文档](docs/README.md) · [音频补丁设计](docs/CARWITH-AUDIO-FALLBACK.md) · [反馈问题](https://github.com/zhouhuan1005/CarWith2CarPlay/issues)

已有部分实车连接成功、音画正常的反馈。下表区分已留档结果和单车反馈；结果不代表同一档案的所有车型、固件或功能都已验证。

## 注意事项

1. **反馈问题请附上故障现场日志。** 在 [GitHub Issues](https://github.com/zhouhuan1005/CarWith2CarPlay/issues) 提交反馈时，请完整描述故障现象，包括 App 版本、车机型号与档案、画面来源、复现步骤和具体报错。故障发生后，请立即从 HyperCar 的「诊断」页导出txt日志，并附在 Issue 中。无完整日志的Issues无法定位故障会直接关闭。
2. **宝马二次上车提示连接失败时，可保留配对重连。** 在车机的「移动设备」设置中点击「连接新设备」，等列表出现手机名称后，点选该手机。这一步可触发车机重新发送 CarPlay 邀请，车机和手机都无需删除原有配对记录。

## 主要功能

- **画面来源**：恒定转发小米 CarWith 的界面，按车机屏幕自动适配，无需手动设置分辨率。
- **音乐控制**：手机端检测默认音乐 App 的播放状态，支持连接自动播放；封面与切歌控制经由 CarWith 链路。
- **声音转发**：提供「自动」「PCM」「AAC 优先」。AAC和缓冲音频需接收端声明支持。
- **蓝牙会话放行**：CarPlay 会话建立后不再反复断开车机蓝牙的通话/媒体配置文件，蓝牙音乐与通话保持稳定，无需每次删配对重连。
- **自动连接与唤醒**：支持目标车机、增强发现、待机通知、首页自动连接快捷开关及有界断线恢复。
- **外观与诊断**：手机 App 支持深色、HyperOS 风格浅色和跟随系统；连接阶段、推流参数与日志导出用于排查问题。

方向盘媒体键与语音转发已实现，具体按键取决于车机报告及当前来源；车机麦克风与 HUD 属于实验功能，见下方说明。

## 车机档案与兼容状态

在「设置 → 我的车机」选择档案及目标设备。内置档案离线可用，也可在 App 中更新远程档案；远程更新可调整兼容策略，新增原生功能仍需更新 App。

| 设置中的档案 | 适配对象 | 当前状态 |
|---|---|---|
| **BMW** | ID7-ID8.5（MGU-86 / MGU-55），以 iD8.5 为实车基准 | 已有 iD8.5 实车音画成功记录 |
| **BMW ID9** | IDC23 / ID9 | 有单车音画成功反馈，待留档复核 |
| **BMW ID6** | NBT Evo | 有单车音画成功反馈，待留档复核 |
| **大众 Volkswagen** | VW4131 / VW4211，参考 ID.7、帕萨特 2025 | 有单车音画成功反馈，待留档复核 |
| **Audi MIB3** | MMI 6318 / 9310 / 3315 参考配置 | 有单车音画成功反馈，待留档复核 |
| **奔驰 Mercedes-Benz** | MBUX（MB_D-PP / MB_E-P） | 有单车音画成功反馈，待留档复核 |
| **通用配置（0.27 基线）** | 未单独适配的接收端 | 供未知车型尝试，不代表全部车机可用 |
| **WinPlay** | PC 接收端 | 用于媒体、触摸和会话联调；DiPlay 也已有联调记录，但无独立内置档案 |

以上档案大致覆盖市面上的Carplay连接路径，未明确适配的车机可依次尝试上述档案找到可正常连接的档案使用，BMW 的不同硬件平台不能只按 iDrive 名称判断；IDC23 使用 BMW ID9，NBT Evo 使用 BMW ID6。PC 接收端测试结果不能代替实车兼容性验证。

## 手机要求与画面来源

- **Android 12 及以上、arm64-v8a** 手机。
- 车机支持无线 Apple CarPlay，手机蓝牙与 Wi-Fi 保持开启。
- 按首页引导完成必需权限；后台运行、静默入网及系统音频采集能力受 ROM 和应用限制。

| 画面来源 | 必需条件 | 画面与设置 |
|---|---|---|
| **CarWith（恒定）** | 系统已安装并启用小米 CarWith（`com.miui.carlink`） | 使用 CarWith 自身界面，自动适配车机屏幕，无需手动设置分辨率。基础投屏无需 Shizuku；**车机出声的音频降级补丁需要 Shizuku**（推荐授权），未授权时首次加入车机 Wi-Fi 或手动连接请求开启蓝牙可发现，可能需要按系统提示确认 |

CarWith 是否可用取决于系统是否提供该组件，不仅取决于手机品牌。部分静默入网、蓝牙回连辅助仍依赖 Shizuku 或系统授权。

## 首次使用

1. 从 [Releases](https://github.com/zhouhuan1005/CarWith2CarPlay/releases/latest) 下载 APK 安装，打开 HyperCar，按首页引导授予附近设备、通知等必需权限。Android 12 / 12L 还需定位权限与定位服务。
2. 确认系统 CarWith 可用（App 自动检测）。音频降级补丁需先完成 Shizuku 授权。
3. 在「我的车机」选择合适档案和目标车机，完成系统蓝牙配对，并在车机侧选择该手机、确认 Apple CarPlay 授权。部分车机首次需要手动加入车机 Wi-Fi。
4. 音乐自动播放需在手机端设置默认音乐 App；封面与控制需授予系统「通知使用权」。
5. 「上车自动连接」新安装默认开启，授予悬浮窗权限并完成启动条件后开始监听；按系统要求设置自启动和电池优化。Android 13+ 可为目标车机授权增强发现。
6. 连接后分别确认车机桌面、声音和触摸。诊断中的「控制已连接」或视频写帧增长，不等于车机已进入 CarPlay 桌面。

无互联网的车机热点按局域网方式连接时，HyperCar 绑定车机网络，其他 App 使用系统默认网络；实际入网授权与移动数据可用性仍取决于系统设置。

## 设置入口

| 分类 | 主要内容 |
|---|---|
| **我的车机** | 上车自动连接、车机档案、远程档案更新、目标设备与增强发现 |
| **画面与声音** | CarWith 设置快捷入口、分辨率与帧率、音频格式、画面资源联动 |
| **主题与外观** | 手机 App 深色、浅色、跟随系统；仅改变手机 App，车机界面主题在车机内设置 |
| **权限与后台** | 系统权限、通知使用权、后台运行与 Shizuku 授权 |
| **高级设置** | 手动蓝牙地址及配对身份清理 |

诊断页提供会话信息与日志导出。

### 显示与声音

- **分辨率**：CarWith 模式按车机屏幕分辨率自动换算源画面尺寸（上限 **2560×2560**），再按需拉伸适配车机屏幕，无需在系统 CarWith 中手动设置分辨率。
- **帧率**：可在「画面与声音」选择；实际输出取决于画面更新、编码器和接收端能力，静止地图不一定持续满帧率。
- **音频**：默认转发声音。音频格式可选自动、PCM 或 AAC 优先。缓冲音频在车机声音支持且使用AAC时尝试协商开启，CarWith 默认启用车机麦克风，在请求语音输入且车机支持时收音。

### 自动连接与车机联动

- **首次连接**：BMW 等要求本轮激活的档案等待车机邀请或本轮 iAP2 端点；已配对、连上热点或端口可达，均不能代替授权。
- **主动停止**：开启自动连接时，主动停止会暂停自动连接 10 分钟，手动启动可解除暂停。
- **画面资源联动**：按车机屏幕资源变化建立或释放视频通道，保留音频和控制；部分档案会强制启用。
- **蓝牙会话放行（v1.1.0.0）**：CarPlay 会话建立后，App 不再反复断开车机蓝牙的 HFP/A2DP 配置文件（此前该行为会导致部分车机蓝牙断断续续、需删配对重连）。音频降级采集期间只约束 A2DP，不影响通话。
- **HUD / 仪表导航**：导航数据订阅转发保留框架；自绘仪表地图已随自绘 UI 一并移除，HUD 显示依赖车机实际订阅。详见[导航说明](docs/NAVIGATION.md)。

## 构建与开发

Kotlin / Compose 管理界面、蓝牙、网络与会话；Shizuku 用于音频降级补丁的系统播放采集与静默入网辅助；Rust 负责 AirPlay 控制、H.264、PCM / AAC 和实验麦克风链路。

需要 JDK 17、Android SDK 35、NDK r27c、Rust 和 cargo-ndk。Windows 可使用仓库脚本构建调试包：

```powershell
pwsh -File tools\build-local.ps1

# 仅修改 Kotlin / 资源，且已有对应原生库时
pwsh -File tools\build-local.ps1 -SkipRust
```

正式签名、版本号、测试和安装见[构建说明](docs/BUILD.md)。CarWith 的 CarLife 通道、认证及加密流程见[画面来源说明](docs/CARWITH-SOURCE.md)。

| 目录 | 内容 |
|---|---|
| `app/` | Android 界面、会话、蓝牙、网络与 CarWith 音频降级 |
| `rust-core/` | AirPlay 控制与媒体核心 |
| `profiles/` | 远程车机档案 |
| `tools/` | 构建和诊断工具 |
| `docs/` | 使用、协议、适配与验证说明 |

## 与上游 HyperCar 的差异

上游 HyperCar 的 CarWith 模式把画面转发给车机，但**媒体音频按小米 CarWith 的设计留在手机本地**
（蓝牙 A2DP 或外放），CarLife 引擎的 MEDIA 通道全程零内容——实测表现为"车机没声音、声音从手机出"。
本项目在不动上游架构的前提下加入 **音频降级补丁**：

1. **Rust 层看门狗**（`rust-core/src/carlife.rs`）：CarWith 模式连车后，MEDIA 通道持续 15 秒零内容
   （以首个非静音 PCM 为唯一"活"判据）时发出 `carwith_media_silent` 状态事件；通道恢复内容时发
   `carwith_media_live`。事件经 `callback_context::inherit` 通道任务继承上下文后可靠送达 Kotlin。
2. **Kotlin 层自动降级**（`CarWithAudioFallback`）：收到 silent 后用 Shizuku 系统混音采集
   （`CarAudioCapture`），抓手机上任何 App 播放的声音，走同一条 AirPlay 通道推给车机；
   收到 live 自动停采集，避免双播；会话收尾自动清理。

设计依据与实测证据见 [docs/CARWITH-AUDIO-FALLBACK.md](docs/CARWITH-AUDIO-FALLBACK.md)。

**奥迪 MIB3 实测记录**（奥迪 Q3 quattro 2026款，MMI 3B.24-241202 / 软件 4318，`audi_mib3` 档案）：
- CarWith 模式 + 音频降级补丁：画面/触摸正常，连车后约 15 秒车机开始出声（前提：Shizuku 已授权）；
- 车机侧需先进配对窗口（车机上点一次 CarPlay/手机图标），否则会反复出现「车机未就绪或不在无线配对窗口」。
- 蓝牙媒体音频（A2DP）与补丁出声二选一：补丁采集期间 App 会主动断开 A2DP 防止双播，通话（HFP）不受影响。

## 致谢与许可证

- [fw867/hypercar](https://github.com/fw867/hypercar)（GPL-3.0）——本项目的基础，音频降级补丁的
  采集与投送链路完全复用其 `CarAudioCapture` / AirPlay 输出栈；
- [shaonialife/cardeck](https://github.com/shaonialife/cardeck)（MIT）——HyperCar 与 CarPlay 发送端
  的上游原型；
- [wangxso/mcarplay](https://github.com/wangxso/mcarplay)（MIT）——同方向的独立实现，其公开的
  CarWith 协议逆向笔记（IDR 帧需缓存 SPS/PPS 等）对本项目的诊断有参考价值；
- [ayuanStear/CarWithEnhance](https://github.com/ayuanStear/CarWithEnhance)——公开的 CarWith
  反编译审计文档，佐证了"CarWith 媒体音频不走 CarLife 引擎通道"的设计结论。

本项目按 [GPL-3.0](LICENSE) 开源分发，继承上游许可证并保留全部版权声明；所有改动均在本仓库
Git 历史中可见。与 Apple 及各车厂、地图和音乐服务商无隶属关系。CarPlay、AirPlay 等商标归各自权利人所有。

## 文档与反馈

从[文档索引](docs/README.md)进入完整说明；排查连接看[连接诊断](docs/HANDSHAKE-DIAGNOSTICS.md)，实际通过范围看[验证记录](docs/VALIDATION.md)，参考项目见[参考资料](docs/REFERENCE-REVIEW.md)。

反馈问题时请提供 App 版本、车机型号与档案、画面来源、是否首次/重连，以及操作现象和对应日志。分享前请移除日志中的设备标识、Wi-Fi 信息、路线和认证内容。

项目用于协议研究与互操作实验，按 [GPL-3.0](LICENSE) 开源，与 Apple 及各车厂、地图和音乐服务商无隶属关系。CarPlay、AirPlay 等商标归各自权利人所有。
