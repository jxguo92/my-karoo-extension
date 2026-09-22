# Karoo Extension Data Field 能否控制手机媒体

调查日期：2026-09-22。范围：Hammerhead 公开 `karoo-ext` 1.1.9 API、Hammerhead 官方帮助页、Android 官方 API。本文讨论手机正在播放的媒体；Karoo 本机播放的媒体是另一种情况。

## 结论

**可以做可点击的自定义 Data Field，但目前不能仅凭公开 `karoo-ext` API，复用 Karoo 内置字段与官方 Companion App 的链路，直接控制手机的播放、上一首、下一首或手机音量。** Hammerhead 明确说明内置媒体功能经 Companion App 工作，且提供三个 Music Data Fields；这些产品功能说明不了第三方扩展获得了同一命令接口。[Hammerhead 媒体播放器帮助页](https://support.hammerhead.io/hc/en-us/articles/41659377165979-Companion-App-Karoo-Media-Player)、[Hammerhead Data Fields Legend](https://support.hammerhead.io/hc/en-us/articles/35533240795419-Data-Fields-Legend)

截至公开 API 文档 1.1.9，`KarooEffect` 的完整继承类列表没有媒体播放、切歌或调节手机音量的 effect；`KarooEvent` 列表也没有手机媒体状态、曲目信息或音量事件。`PerformHardwareAction` 能模拟 Karoo 的四个机身按键及两个组合键，不能指定某个内置媒体控件执行播放或音量动作。故此结论限定为**已发布的公开 API**；不能据此断言 Karoo OS 内部没有私有实现。[`KarooEffect`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-karoo-effect/index.html)、[`KarooEvent`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-karoo-event/index.html)、[`PerformHardwareAction`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-perform-hardware-action/index.html)

SDK 生成的既有数据类型与字段常量清单中也没有公开的 `MEDIA`、`MUSIC` 或 `VOLUME` 项。不能因为系统 UI 里出现三个内置字段，就推断扩展可通过 `OnStreamState` 订阅它们的媒体状态。[`DataType.Type`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-data-type/-type/index.html)、[`DataType.Field`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-data-type/-field/index.html)

Hammerhead 社区有一条要求增加 `MediaControl.Play/Pause/Next/Previous/VolumeUp/VolumeDown` 等 effect 的提案；SDK 仓库还有一条开放的需求单，要求公开原生音频/媒体状态与事件。这两条都是用户需求，并非官方发布承诺；上述公开 API 清单才是当前可调用能力的依据。[Hammerhead 社区提案](https://support.hammerhead.io/hc/en-us/community/posts/41728258928795-Add-Karoo-Effects-for-media-player-control)、[`karoo-ext` issue #74](https://github.com/hammerheadnav/karoo-ext/issues/74)

## 点击能力与手机命令之间的缺口

公开 SDK 的 `DataTypeImpl.startView` 可向 Karoo 提供 `RemoteViews`。Android 的 `RemoteViews.setOnClickPendingIntent` 可将视图点击转回扩展应用的组件。因此“字段中放播放/音量按钮，接收点击”在 UI 层有实现路径；但点击之后仍需独立的**手机端命令通道**。Android 对 Karoo 本机 `MediaSessionManager` 或 `AudioManager` 的调用不等于调用手机上的相应服务。[`DataTypeImpl`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.extension/-data-type-impl/index.html)、[`RemoteViews.setOnClickPendingIntent`](https://developer.android.com/reference/android/widget/RemoteViews#setOnClickPendingIntent(int,%20android.app.PendingIntent))、[`MediaSessionManager.getActiveSessions`](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName))

区分两种音量：内置 `Media Volume` 字段在官方媒体功能语境中调节**连接手机的媒体音量**；在 Karoo 扩展进程调用 Android `AudioManager.adjustStreamVolume` 则作用于执行该调用的 **Karoo 本机音频流**，不能据此改变手机音量。若媒体应用运行于 Karoo 本机，这一边界才改变。[Hammerhead 媒体播放器帮助页](https://support.hammerhead.io/hc/en-us/articles/41659377165979-Companion-App-Karoo-Media-Player)、[Android `AudioManager.adjustStreamVolume`](https://developer.android.com/reference/android/media/AudioManager#adjustStreamVolume(int,%20int,%20int))

## 可行路径

1. **沿用内置字段**：若目标只是骑行中控制普通手机媒体，直接添加官方 `Media Control`、`Media Volume`、`Now Playing` 字段，使用官方 Companion App。官方要求最新一代 Karoo、Karoo SW 1.585.2249 或以上、手机有互联网连接；Android 手机还需允许通知访问。此路径无需开发扩展，但外观和交互由 Karoo 决定。[Hammerhead 媒体播放器帮助页](https://support.hammerhead.io/hc/en-us/articles/41659377165979-Companion-App-Karoo-Media-Player)、[Data Fields Legend](https://support.hammerhead.io/hc/en-us/articles/35533240795419-Data-Fields-Legend)
2. **自建 Android 手机伴侣应用**：Karoo 字段用 `RemoteViews`/`PendingIntent` 接收点击，再通过自己设计并验证的网络或蓝牙通道向手机应用发送播放、切歌、音量命令。手机应用可在用户启用通知监听后，用 `MediaSessionManager.getActiveSessions` 获取手机**本机**活动媒体会话，用相应 `MediaController.TransportControls` 发送 `play`、`pause`、`skipToNext`、`skipToPrevious`；手机媒体流音量可由手机应用调用 `AudioManager.adjustStreamVolume(STREAM_MUSIC, …)`。`getActiveSessions` 需要 `MEDIA_CONTENT_CONTROL` 权限或已启用的通知监听服务，普通第三方应用通常采用后者。此路径需自行解决手机端安装授权、配对、连接、状态回传与异常处理；官方 Companion App 的私有媒体通道未在公开 SDK 中暴露。[Android `MediaSessionManager`](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName))、[`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService)、[`TransportControls`](https://developer.android.com/reference/android/media/session/MediaController.TransportControls)、[`AudioManager`](https://developer.android.com/reference/android/media/AudioManager#adjustStreamVolume(int,%20int,%20int))、[`KarooEffect`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-karoo-effect/index.html)
3. **特定媒体服务的官方远程 API**：若只需控制某一服务，可评估其公开设备控制 API，从 Karoo 直接向该服务发命令。这无法等同于通用手机媒体控制，尤其不能假定能调手机的系统音量；具体功能须按该服务官方 API 核对。此项是架构选项，尚未针对任何服务作可行性验证。
4. **Karoo 本机播放**：若播放器也装在 Karoo，扩展可探索 Karoo 本机 Android 媒体会话与音量 API；这控制的是 Karoo 上的播放，不是连接手机上的媒体。媒体会话访问仍受 Android 权限约束。[Android `MediaSessionManager`](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName))、[`AudioManager`](https://developer.android.com/reference/android/media/AudioManager#adjustStreamVolume(int,%20int,%20int))

## 真机待验证

- 在目标 Karoo 机型和系统版本上，确认自定义图形字段的各尺寸中 `RemoteViews` 点击可达、多个按钮能区分，页面切换及字段取消后不再处理过期点击。[`DataTypeImpl.startView`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.extension/-data-type-impl/index.html)、[`RemoteViews.setOnClickPendingIntent`](https://developer.android.com/reference/android/widget/RemoteViews#setOnClickPendingIntent(int,%20android.app.PendingIntent))
- 检查官方 Companion App 连接、播放手机媒体时，Karoo 本机是否产生可供普通扩展应用读取的代理 `MediaSession`；公开文档没有保证存在，且即便存在也需要验证第三方可访问及命令是否确实回传手机。不能据此预先设计为可靠方案。[Android `MediaSessionManager.getActiveSessions`](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName))
- 如走自建 Android 手机伴侣应用，验证通知监听授权、不同播放器的可用动作、锁屏与后台运行、耳机输出下的手机音量变化、断连重连和延迟；`TransportControls` 表示向播放器提出动作请求，具体响应取决于播放器提供的会话能力。[`MediaController.TransportControls`](https://developer.android.com/reference/android/media/session/MediaController.TransportControls)、[`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- 设备行为无法由文档检查替代；本次未进行真机测试。
