# Karoo 扩展如何点击 Data Field 切换显示内容

调查日期：2026-09-23。选取 [awesome-karoo](https://github.com/timklge/awesome-karoo) 收录、明确支持点击切换内容且源码可查的三个扩展。源码链接固定到调查时的提交；“热门”仅指社区列表中可见的项目，不作为下载量排名。本文讨论骑行页中的自定义图形 Data Field，不讨论进入应用设置页后再修改显示。

## 结论

三个项目的共同链路是：**图形字段给 `RemoteViews` 绑定点击动作 → 应用接收点击并修改显示状态 → `startView` 正在收集的状态流发出新值 → 重新生成 `RemoteViews`，调用 `ViewEmitter.updateView`**。Karoo SDK 1.1.9 的 `DataTypeImpl.startView` 是输出视图的接口，并无通用的字段点击回调；`startStream` 的标准数值视图也没有可由扩展绑定的内部热区。[SDK `DataTypeImpl`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.extension/-data-type-impl/index.html)、[Android `RemoteViews.setOnClickPendingIntent`](https://developer.android.com/reference/android/widget/RemoteViews#setOnClickPendingIntent(int,%20android.app.PendingIntent))

## 三个项目的源码路径

### 1. sk0711-graph：`PendingIntent` → `BroadcastReceiver` → 内存 `StateFlow`

它的心率和功率曲线点击后按 **1 分钟 → 5 分钟 → 全程 → 1 分钟**切换。`BaseGraphDataType` 在非预览的图形视图上调用 `setOnClickPendingIntent`，心率/功率分别使用不同的 action 和 request code；Manifest 注册仅本应用可接收的 `TapReceiver`。Receiver 调用 Extension 服务的 `toggleHrWindow` 或 `togglePowerWindow`，将对应的 `MutableStateFlow` 更新为下一个 `TimeWindow`。[视图和 PendingIntent](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/BaseGraphDataType.kt#L198-L227)、[Receiver](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/TapReceiver.kt#L7-L13)、[Manifest](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/AndroidManifest.xml#L38-L45)、[状态与切换](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/HrPowerExtension.kt#L22-L44)、[窗口顺序](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/graph/TimeWindow.kt#L3-L12)

`startView` 把传感器采样、窗口状态、平均值和最大值合并。窗口变化本身就能触发新的图像绘制及 `updateView`，无需等待下一次传感器数据。显示状态按**心率/功率类别**共享；普通功率与 NP 功率共用功率窗口，并非同一类型的每个画面实例各自保存窗口。README 所说“各字段各自保留窗口”应按源码的实际状态作用域理解。启动时从设置读取默认窗口，点击后的切换留在内存中。[合并与重绘](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/BaseGraphDataType.kt#L124-L147)、[状态初始化](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/HrPowerExtension.kt#L22-L68)、[README](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/README.md?plain=1#L20-L21)

### 2. karoo-routegraph：Glance 点击回调 → 共享显示状态 → 重绘

项目说明点击路线高度图可循环全程、未来 2/20/50/100 km 等范围，实际可选级别来自用户设置，且只采用小于路线总长度的级别。`RouteGraphDataType` 用 Glance 的 `clickable(actionRunCallback<ChangeZoomLevelAction>())` 给主图绑定点击，传入一个 `view_id`；回调从 Koin 取得共享的 `RouteGraphDisplayViewModelProvider`，把 `zoomLevel` 更新为 `next()`。传入的 `view_id` 当前只用于日志，没有用来隔离每一个画面实例。[项目说明](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/README.md)、[图形点击绑定](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/datatypes/RouteGraphDataType.kt#L650-L668)、[点击回调](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/datatypes/minimap/ChangeZoomLevelAction.kt#L31-L59)、[级别与共享状态](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/RouteGraphDisplayViewModel.kt#L25-L99)

`startView` 合并路线、显示状态、设置、可见性等 Flow，按 1 秒节流，只在可见时重画。项目把横向图、纵向图和小地图的缩放分别放在共享状态的不同属性中。图上的 POI 子按钮另有点击动作，表明一个图形字段可以分配多个点击热区。项目的混淆规则显式保留三个 Glance 缩放回调类；其提交历史有一次“点击不触发”的修复，因此 release 构建必须实际验证回调。[Flow 与刷新](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/datatypes/RouteGraphDataType.kt#L124-L153)、[显示状态](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/RouteGraphDisplayViewModel.kt#L88-L99)、[混淆规则](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/proguard-rules.pro)、[修复提交](https://github.com/timklge/karoo-routegraph/pull/233)

### 3. karoo-headwind：Glance 点击回调 → DataStore → 设置 Flow 重绘

天气预报字段在非预览模式给整个预报 `Row` 绑定 `clickable(actionRunCallback<CycleHoursAction>())`。回调读取当前 widget 设置和预报，小时偏移量每次加 3；超过可用预报范围时归零，然后写回 DataStore。`ForecastDataType.startView` 合并预报与 `streamWidgetSettings()` 等 Flow，按偏移量挑选要显示的预报，最终通过 `updateView` 更新视图。默认偏移量为 0，点击状态会跨进程/服务重启保留；同一 `weatherForecast` 字段的多个放置位置读取同一个设置键，因而共享偏移值。[字段实现](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/datatypes/WeatherForecastDataType.kt#L26)、[点击绑定与显示选择](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/datatypes/ForecastDataType.kt#L222-L280)、[回调及越界处理](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/datatypes/CycleHoursAction.kt#L31-L51)、[持久化和读取 Flow](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/DataStore.kt#L63-L120)、[默认值](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/HeadwindSettings.kt#L52-L57)

## 对本项目的选择

本项目现有图形字段使用 XML `RemoteViews`，尚未引入 Glance。因此若只做“点一下在几组指标/页面间循环”，最短路径是沿用 **sk0711-graph 的 Android 点击入口**：在字段专用视图的根容器上设置 `PendingIntent.getBroadcast`，用本应用的非导出 Receiver 接收；在对应 `feature/datafield/<field>/` 内用纯 Kotlin 状态转换决定下一页，并让现有显示 Flow 同时观察实时数据和页码。`startView` 继续只负责 SDK 适配与生命周期，收到新状态后重新渲染 `RemoteViews`。[本项目图形字段示例](../../app/src/main/kotlin/com/jxguo92/mykarooextension/feature/datafield/climberfield1/ClimberField1DataField.kt)、[本项目刷新 helper](../../app/src/main/kotlin/com/jxguo92/mykarooextension/core/karoo/CompositeFieldRuntime.kt)、[sk0711 点击链路](https://github.com/svenk0711/sk0711-graph/blob/d0daa2af011b8f2865252f7eff98ab4607eb940d/app/src/main/kotlin/com/sk0711/graph/extension/BaseGraphDataType.kt#L198-L227)

先明确切换状态的**作用范围**：这三个项目都按字段类别或应用级状态共享，不证明 Karoo 自动提供“同一 typeId 的每个格子独立状态”。公开 `ViewConfig` 只有尺寸、对齐、边框和预览等属性，没有稳定的字段实例 ID。如果两个相同字段同时放在页面上，要求它们各自独立切换，就需要单独设计实例标识及重建策略，不能直接把一次 `startView` 产生的临时 UUID 当作可长期恢复的设备实例 ID。[SDK `ViewConfig`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-view-config/index.html)、[RouteGraph 回调仅记录 view_id](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/datatypes/minimap/ChangeZoomLevelAction.kt#L36-L59)

更新频率也要限制：官方 `ViewEmitter.updateView` 超过 **1 Hz** 的调用会被丢弃。点击可以立刻更新状态，但显示反馈可能到下一次允许的画面提交；快速连点时按业务决定保留每次状态转换还是只显示最终页。预览仍使用固定内容，不响应点击；字段取消时移除 Flow 收集，Receiver 需避免操作已经失效的临时视图。[SDK `ViewEmitter`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.internal/-view-emitter/index.html)、[本项目预览与取消](../../app/src/main/kotlin/com/jxguo92/mykarooextension/core/karoo/CompositeFieldRuntime.kt)

## 实现前的真机检查

- 新 Karoo 骑行页：单击根热区是否到达，快速连点是否漏掉事件；各格子尺寸下热区是否准确。
- 字段预览、切页、退出/重进、扩展重启后：显示页是否符合选定的内存或持久化策略；取消后无残留订阅。
- 同一 `typeId` 放两个格子：确认是否有意同步切换；如果产品要求独立，先解决实例身份问题。
- 若使用 Glance：debug 与 release 包都检查点击，尤其检查混淆规则；长按仍由 Karoo 用于更换字段，不能当作扩展切换动作。[Hammerhead 骑行中编辑说明](https://support.hammerhead.io/hc/en-us/articles/25709425315227-Karoo-OS-Editing-an-ongoing-ride)

本次只做源码与 API 调研，未在 Karoo 真机上复现交互。
