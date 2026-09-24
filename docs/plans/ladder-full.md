# Ladder Full 整页仪表盘字段实施计划

## Summary

新增图形 Data Field `ladder-full` / `Ladder Full`：独占整页（5 行 2 列）的汽车仪表盘风格组合字段，视觉以
[`demo/dashboard/ladder/ladder.prototype.html`](../../demo/dashboard/ladder/ladder.prototype.html) 的方案 B、C 为准，
规则以 [`demo/dashboard/context.md`](../../demo/dashboard/context.md) 为准；`prototype-description.md` 不作为依据。
术语见 [`CONTEXT.md`](../../CONTEXT.md)。

- 点击字段在**速度核心布局**（原 B，默认）与**功率核心布局**（原 C）之间切换，结果持久化。
- 相比原型只改两处：左上角的风向箭头改为按**风力级别**着色的**相对风向**箭头；右上角改为指向真北的**指北针**。
- 不在整页位置时什么都不显示。
- 保持单 `app` 模块；业务代码全部位于 `feature/datafield/ladderfull/`。

## 已确认决策

| 主题 | 决策 |
| --- | --- |
| 整页判定 | 仅 `ViewConfig.gridSize == (60, 60)`；`viewSize` 只用于缩放。否则正式运行与预览都空白，不订阅任何数据、不请求天气。 |
| 布局切换 | 整个字段为点击热区；DataStore 持久化；所有 Ladder Full 共享同一状态；预览不响应点击；默认速度核心布局。 |
| 分区 | 功率：Coggan 7 区，按 `UserProfile.ftp` 计算，指示条覆盖 41%–200% FTP。心率：HRR 6 区，按 `maxHr`/`restingHr` 计算，覆盖 41%–100% HRR。不读取 Karoo 的 `powerZones`/`heartRateZones`。FTP ≤ 0 或 `maxHr ≤ restingHr` 时该指示条只画空轨道。 |
| 单位 | 固定公制（km/h、km），忽略用户资料的单位偏好。这是本字段对 `AGENTS.md`“处理单位制”要求的有意例外，其他字段不变。 |
| 主题 | 固定原型深色配色，不随 Karoo 日夜模式变化。 |
| 车头朝向 | `LOCATION.LOC_BEARING`；仅当速度 ≥ 5 km/h 且 bearing 存在时更新，否则保持最后有效值；本次骑行尚无有效值时箭头与指北针都不显示。绘制角度四舍五入到 10°。 |
| 相对风向 | 箭头指向风吹去的方向：纯顶风朝下、纯顺风朝上。角度 = 气象风向 + 180° − 车头朝向。 |
| 风力级别 | 持续风速（不看阵风），边界值归较高一级：轻微 < 3.4 m/s 浅灰白；小 3.4–5.4 青绿；中 5.5–7.9 黄；大 ≥ 8.0 红。只用颜色，不显示风速数字。 |
| 指北针 | 菱形双色指针，北端红、南端灰，北端外侧小号 `N`；旋转角 = −车头朝向。 |
| 天气刷新 | 启动即请求；距上次成功请求位置 > 5 km 或已满 30 分钟时再请求；失败每 2 分钟重试；超过 60 分钟未成功即为过期天气，箭头隐藏；未配置和风凭据时永不请求；不受骑行状态影响。 |
| 和风归因 | 按用户决定不显示（仅个人使用；公开分发前需重新评估）。 |
| 骑行状态 | 沿用 Climber 字段规则：收到 `Idle` 后全部清空（含风向与指北针，并重置车头朝向），直到重新进入 Recording/Paused；暂停时照常显示；RideState 未到达时不阻塞。 |
| 缺失值 | 各数据源独立：数字 `--`；指示条/圆弧保留空轨道和刻度、不画填充；踏频灯珠全暗；坡度、档位 `--`。零功率/踏频/速度有效，心率 ≤ 0 无效。超出显示范围时填充钳制到端点，数字显示原始值。 |
| 预览 | 固定速度核心布局与原型示例数据（见下文）。 |

## Implementation Changes

### 数据源

| 元素 | SDK Type | Field | 换算与格式 |
| --- | --- | --- | --- |
| 踏频灯珠 | `CADENCE` | `CADENCE` | 51–110 rpm，每 5 rpm 一颗，共 12 颗；51–80 白、81–100 绿、101–110 红；不显示数字 |
| 3 秒功率 | `SMOOTHED_3S_AVERAGE_POWER` | `SMOOTHED_3S_AVERAGE_POWER` | 整数 W |
| 心率 | `HEART_RATE` | `HEART_RATE` | 整数 bpm |
| 速度 | `SPEED` | `SPEED` | m/s × 3.6，一位小数 km/h |
| 坡度 | `ELEVATION_GRADE` | `ELEVATION_GRADE` | 一位小数 `%`，保留负号 |
| 档位 | `SHIFTING_REAR_GEAR` | `SHIFTING_REAR_GEAR`、`SHIFTING_REAR_GEAR_MAX` | `6/13`；最大档数缺失时只显示 `6`；不显示齿数 |
| AVG PWR / NP | `AVERAGE_POWER` / `NORMALIZED_POWER` | 同名 | 整数 W |
| AVG SPD | `AVERAGE_SPEED` | `AVERAGE_SPEED` | 一位小数 km/h |
| AVG HR | `AVERAGE_HR` | `AVG_HR` | 整数 bpm |
| DIST | `DISTANCE` | `DISTANCE` | m ÷ 1000，一位小数 km |
| 位置与朝向 | `LOCATION` | `LOC_LATITUDE`、`LOC_LONGITUDE`、`LOC_BEARING`、`LOC_SPEED` | 朝向更新门限优先用同一样本的 `LOC_SPEED`，缺失时用最近的 `SPEED` |
| 分区参数 | `UserProfile` | `ftp`、`maxHr`、`restingHr` | — |
| 风 | `QWeatherProvider` | `windDirectionDegrees`、`windSpeedMetersPerSecond` | 任一缺失即不显示箭头 |

全部订阅复用 `core/karoo/KarooFlows.streamStates`、`userProfile`、`rideState`。天气请求经 `NetworkAwareHttpGet`
（`ConnectivityWifiStatus` + `UrlConnectionHttpGet` + `KarooHttpGet(KarooSystemHttpChannel)`），凭据来自
`QWeatherConfig.fromBuildConfig()`。

### 切片文件：`feature/datafield/ladderfull/`

- `LadderFullDataField.kt`：唯一的 SDK 适配层。
  - 不覆写 `startStream`（SDK 默认实现）。
  - `startView`：发送 `UpdateGraphicConfig(showHeader = false)`；非整页时提交一次空白视图并返回。
  - 整页且非预览时：在字段自己的 `SupervisorJob` scope 中收集各数据流、布局 Flow 和天气更新，归约为状态，
    经 `distinctUntilChanged + sample(1 s)` 渲染；首帧立即以空状态绘制。
  - 每帧新建 `RemoteViews`，根视图绑定 `PendingIntent.getBroadcast`（`FLAG_IMMUTABLE`，显式指向本应用 receiver）；
    `updateView` 用 try/catch 包住，失败只记录日志。
  - `emitter.setCancellable` 取消 scope，移除全部 consumer 与天气循环。
- `LadderFullState.kt`：不可变状态与纯 reducer（传感器值、分区参数、骑行状态、车头朝向、天气、当前布局），
  `frame()` 输出与绘制无关的显示模型（文字、填充比例、分区色序号、灯珠数、箭头角与级别、可见性）。
- `LadderFullCalculator.kt`：纯计算。
  - 功率/心率分区边界、区内线性插值的填充比例（沿用 HTML 的 `zoneFraction`）与分区色映射（6 区跳过第 5 色）。
  - 速度填充比例（0–60 km/h）、踏频点亮颗数（`floor((rpm − 51) / 5) + 1`，钳制到 1–12）。
  - 风力级别、相对风向角、指北针角、10° 取整、车头朝向保持规则。
- `LadderFullWeatherPolicy.kt`：纯规则，输入当前时间、位置、上次成功时间与位置、上次失败时间，输出“是否请求”
  和“是否过期”；距离用大圆公式。
- `LadderFullWeatherUpdates.kt`：用注入的 `WeatherProvider`、单调时钟和位置 Flow 按策略循环请求，输出最新天气快照；
  同一时刻最多一个请求在途。
- `LadderFullRenderer.kt`：Android Canvas 绘制。
  - 设计坐标系为 480 × 638（HTML 中 y ∈ [81, 719] 的字段区平移到原点），按 `viewSize` 等比缩放并居中，
    其余区域填背景色；`viewSize` 非正时按 480 × 638 绘制。
  - 每个视图复用一张 ARGB_8888 位图（`onNext` 为同步 binder 调用，提交返回后可重绘）。
  - 所有几何常量、颜色、字号、刻度照搬 HTML；速度核心布局与功率核心布局各一个绘制函数。
- `LadderFullPreview.kt`：固定预览状态。
- `LadderFullLayoutStore.kt`：Preferences DataStore，键 `ladder_full_layout`，值 `SPEED_CORE` / `POWER_CORE`；
  缺失或未知值按 `SPEED_CORE` 处理（默认值与向前兼容策略）；提供 `layout: Flow` 与 `toggle()`。
- `LadderFullTapReceiver.kt`：非导出 `BroadcastReceiver`，`goAsync()` 后调用 `toggle()`。

DataStore 与天气刷新目前只有 Ladder Full 一个调用者，按 `AGENTS.md` 暂不移入 `core/`。

### 资源、注册与文档

- `gradle/libs.versions.toml` 与 `app/build.gradle.kts`：加入 `androidx.datastore:datastore-preferences`（实现时取最新稳定版并确认 minSdk 23 兼容）。
- `AndroidManifest.xml`：注册 `LadderFullTapReceiver`，`android:exported="false"`。
- `res/layout/ladder_full_view.xml`：根 `FrameLayout` + 全尺寸 `ImageView`；空白状态使用同一布局但不设位图。
- `res/xml/extension_info.xml`：新增 `ladder-full`，`graphical="true"`，复用 `ic_extension`；`strings.xml` 增加名称与描述。
- `DataFieldRegistry`：追加 `LadderFullDataField`，顺序为 `rear-cog-teeth`、`climber-field-1`、`climber-field-2`、`ladder-full`；
  `create` 增加 `Context` 参数以构造 DataStore 与网络状态，`MyKarooExtension` 传入自身。
- `docs/data-fields.md`：新增 Ladder Full 小节——数据源、整页条件、两种布局、点击与持久化、固定公制例外、风与指北针规则、
  天气刷新/过期、缺失与骑行状态、预览、未显示和风归因、真机待验证项。

### 预览数据

速度核心布局；3 秒功率 245 W、心率 156 bpm、踏频 88 rpm（8 颗）、速度 28.6 km/h、坡度 3.6%、档位 6/13；
AVG PWR 198 W、AVG SPD 27.4 km/h、AVG HR 148 bpm、NP 214 W、DIST 42.7 km；FTP 200、最大心率 190、静息心率 50；
风从左前方吹来（相对风向箭头指向右下）、风力级别“小”；车头朝向 315°（指北针指向右上）。

## Tests

- Calculator：
  - 功率分区边界在 FTP 200 时为 82/110/150/180/210/240/300/400 W；心率分区在 190/50 时与 `context.md` 一致。
  - 填充比例在下界、分区边界、区内、超上下界时的值；FTP ≤ 0、`maxHr ≤ restingHr` 返回无分区。
  - 速度 0/28.6/60/75 km/h 的比例；踏频 <51、51、55、56、88、110、>110 的点亮颗数。
  - 风力级别四档与 3.4、5.5、8.0 边界；NaN/负值。
  - 相对风向：纯顶风 180°（朝下）、纯顺风 0°、左前来风、跨 0°/360°；指北针角；10° 取整（含 355° → 0°）。
- 车头朝向：速度 ≥ 5 km/h 且有 bearing 时更新；低速或 bearing 缺失时保持；Idle 后清空；`LOC_SPEED` 缺失时回落到 `SPEED`。
- WeatherPolicy：首次请求；5 km 与 30 分钟阈值（含边界）；失败后 2 分钟内不重试；60 分钟过期；无位置不请求。
- WeatherUpdates（coroutines-test 虚拟时间）：成功、失败重试、在途去重、取消后不再请求。
- State/frame：
  - 全部正常；任一数据源 `Searching`/`NotAvailable`/缺字段/非有限值只影响对应元素。
  - 零功率、零踏频、零速度有效；心率 0 与负值无效；档位缺最大档数。
  - Paused 保持显示；Idle 全清且忽略后续传感器值直到重新进入骑行；天气过期或无朝向时箭头隐藏。
  - 两种布局下左右指示条与中央圆弧绑定的指标正确。
- 整页判定：`(60, 60)` 为真，`(60, 48)`、`(30, 60)`、`(60, 15)` 为假。
- LayoutStore：默认、切换两次回到原值、未知存储值回落 `SPEED_CORE`。
- Registry：XML 与 Kotlin typeId 集合相等、唯一，顺序包含 `ladder-full`；Extension ID 检查不变。
- 验证命令（macOS）：`./gradlew testDebugUnitTest`，提交前 `./gradlew lint test assembleDebug`。

## New Karoo 真机验收

- 字段选择器能发现 `Ladder Full`；放在单字段整页时显示完整仪表盘，放在其他尺寸时完全空白。
- Debug 日志记录首次 `gridSize`/`viewSize`/`preview`，确认整页确为 `(60, 60)`、隐藏 header 后的实际像素，以及编辑器预览的尺寸。
- 点击能切换布局且不干扰长按换字段；快速连点行为可接受；重启扩展或设备后布局保持。
- 静止、低速、转弯时 `LOC_BEARING` 的表现，确认 5 km/h 门限与 10° 取整下箭头和指北针不乱跳；实测箭头方向与真实风向一致。
- Wi-Fi 与仅手机蓝牙两种网络下都能取到天气；断网超过 60 分钟后箭头隐藏，恢复后重新出现。
- 约 1.2 MB 位图每秒更新时无 `TransactionTooLargeException`、无明显卡顿或内存增长。
- 传感器断线、零踏频、暂停、结束、重新开始的显示符合规则；退出/重进数据页后无残留订阅或重复天气请求。
- 确认 `ELEVATION_GRADE` 的原始值即百分数（例如 3.6 而非 0.036）、`LOC_SPEED` 的单位为 m/s。

## Assumptions

- `LOC_BEARING` 是以真北为基准的 GPS 行进方向（度）；和风的风向为气象来向（度，真北）。
- 实现基于工作区中尚未提交的 `NetworkAwareHttpGet`/`KarooHttpGet` 改动。
- 不增加设置页、英制支持、日间配色、风速数字、和风归因或每个字段实例独立的布局状态。
