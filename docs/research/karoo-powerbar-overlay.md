# Karoo Powerbar 如何在 Data Field 外绘制

> 调研日期：2026-09-22。依据 [Powerbar `master` 源码](https://github.com/timklge/karoo-powerbar) 与 Android 官方文档。以下链接指向源码行；`master` 会变化，结论需随上游更新复核。

## 结论与调用链

Powerbar 的横条不是 Karoo Data Field。它利用 Android `SYSTEM_ALERT_WINDOW` 权限创建 `TYPE_APPLICATION_OVERLAY` 窗口，由 Android `WindowManager` 把自定义 `View` 放在其他应用画面上方。Karoo SDK 仅提供骑行状态、功率等数据流和扩展发现入口，不负责横条的绘制位置或窗口管理。[`extension_info.xml`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/res/xml/extension_info.xml#L1-L7) 只有扩展 ID 和 `scansDevices="false"`，没有实际 `<DataType>` 声明；[`KarooPowerbarExtension`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/KarooPowerbarExtension.kt#L12-L41) 也未提供字段实现。

```text
Karoo 扩展服务 / 设置 Activity
  → 检查悬浮窗授权并启动 ForegroundService
  → 订阅骑行状态与设置，创建顶部/底部 Window
  → WindowManager.addView(TYPE_APPLICATION_OVERLAY)
  → KarooSystemService 数据流更新进度、颜色、标签
  → CustomView.onDrawForeground(Canvas) 绘制
```

1. [Manifest](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/AndroidManifest.xml#L4-L35) 声明 `SYSTEM_ALERT_WINDOW`、`FOREGROUND_SERVICE`、普通 `ForegroundService` 和 Karoo 扩展服务。用户打开 [Activity](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/MainActivity.kt#L19-L40) 时，若未授权则启动 `ACTION_MANAGE_OVERLAY_PERMISSION`；获得授权后启动前台服务。扩展服务的 `onCreate` 也在 `Settings.canDrawOverlays` 为真时启动该前台服务。[扩展服务源码](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/KarooPowerbarExtension.kt#L21-L33)
2. 前台服务立即发出常驻通知并调用 `startForeground`。它用 `KarooSystemService.streamRideState()` 和设置流决定是否显示：启用“仅骑行时显示”时只在 `RideState.Recording` 创建窗口；分别按配置创建顶部和底部窗口。[`ForegroundService.kt`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/ForegroundService.kt#L28-L68)、[前台通知](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/ForegroundService.kt#L74-L94)
3. 每个 [`Window`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L84-L127) 创建 `WRAP_CONTENT × WRAP_CONTENT`、`TYPE_APPLICATION_OVERLAY`、`FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`、透明像素格式的 `LayoutParams`，按 `Gravity.TOP` 或 `BOTTOM` 靠边并将宽度设为可用显示宽度。它 inflate 一个高度 `80dp` 的 [`popup_window.xml`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/res/layout/popup_window.xml#L1-L12)，随后在主线程调用 [`WindowManager.addView`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L202-L210)。这一步才使内容出现在 Data Field 区域之外。
4. 窗口直接连接 `KarooSystemService`，为所选数据源启动流订阅。以功率为例，读取 SDK 功率流与用户档案，计算进度、功率区间颜色和标签，调用 `invalidate()` 重绘；非 `Streaming` 状态映射成缺失值的 `?`。[`Window.open`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L129-L205)、[`streamPower`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L710-L752)。[`CustomView.onDrawForeground`](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/CustomProgressBar.kt#L15-L27) 遍历进度条并在 Android `Canvas` 上绘制，而非通过 SDK 的 `RemoteViews`/Glance 字段视图。

## 与本项目 SDK 能力梳理的关系

[现有能力文档](karoo-sdk-extension-capabilities.md)描述的 `DataTypeImpl.startView`/`RemoteViews` 只更新 Karoo 分配给该 Data Field 的视图；地图层接口只在地图页显示图标或折线。Powerbar 用的是运行扩展的 Android 应用自身的系统悬浮窗能力，因而能覆盖骑行界面的其他区域。SDK 在此承担数据源和骑行状态来源，悬浮窗的授权、绘制、可见性及回收由 Android 应用承担。这也意味着窗口并非 Karoo 页面布局的一部分；Android 只保证应用悬浮窗位于普通 Activity 之上、关键系统窗口之下，系统仍可调整其位置、大小或可见性。实际遮挡与定位需在 Karoo 2/3 真机验证。[Powerbar README 的使用及权限说明](https://github.com/timklge/karoo-powerbar#usage)、[Android `TYPE_APPLICATION_OVERLAY` 文档](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)

Powerbar 在重建窗口时调用 `close()`，取消各数据流任务、注销临时隐藏广播接收器并 `removeView`；还允许其他扩展发 `de.timklge.HIDE_POWERBAR` 广播暂时隐藏某个位置。源码中的前台服务没有覆写 `onDestroy` 进行对称收尾，`Window` 本身也没有在 `close()` 调用 `KarooSystemService.disconnect()`；这些是复用思路时需补齐的生命周期细节，不应直接照搬。[窗口回收和广播](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L756-L798)、[前台服务全文件](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/kotlin/de/timklge/karoopowerbar/ForegroundService.kt)

## 移植到本项目的版本边界

Powerbar 使用 `compileSdk = 34`、`targetSdk = 33`；本项目当前 `targetSdk = 34`。Powerbar 的 Manifest 未给前台服务指定 `android:foregroundServiceType`。Android 官方要求面向 API 34 及以上的应用为**每个**前台服务声明与用途相符的类型及该类型权限，否则调用 `startForeground()` 时可能抛 `MissingForegroundServiceTypeException` 或权限异常。后台启动前台服务还受 Android 版本和豁免条件约束，不能假定扩展服务每次 `onCreate` 都能直接启动。因而不能直接复制其 Manifest/前台服务配置；应先确定适用类型与启动时机，并在目标 KOS/Android 版本实测。[Powerbar Gradle 配置](https://github.com/timklge/karoo-powerbar/blob/master/app/build.gradle.kts#L10-L18)、[Powerbar Manifest](https://github.com/timklge/karoo-powerbar/blob/master/app/src/main/AndroidManifest.xml#L4-L29)、[Android 14 前台服务要求](https://developer.android.com/about/versions/14/changes/fgs-types-required)、[Android 后台启动限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

若要做类似能力，本项目需增加独立 overlay 切片及其服务，保留现有 Data Field 切片职责；做纯计算与状态测试，并在真机检查权限授予、骑行开始/结束、页面切换、尺寸/遮挡、断线、服务退出和重新进入时的窗口及订阅清理。`TYPE_APPLICATION_OVERLAY` 的权限与窗口行为属于 Android 平台，不能由 `karoo-ext` 公开 API 保证。[Android 悬浮窗权限说明](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)、[Karoo SDK `KarooSystemService`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext/-karoo-system-service/index.html)
