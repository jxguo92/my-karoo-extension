# New Karoo Climber 组合字段实施计划

## Summary

新增两个图形 Data Field，同时保留现有 `Rear Cog Teeth`：

- `climber-field-1` / `Climber Field 1`
  - 全宽：到坡顶距离｜当前 `%FTP`｜到坡顶剩余爬升
  - 窄幅：仅当前 `%FTP`
- `climber-field-2` / `Climber Field 2`
  - 全宽：心率｜踏频｜后飞轮齿数
  - 窄幅：仅踏频
- 目标设备仅为 New Karoo 2024。
- 不增加设置页、DataStore、3 秒平滑、点击行为或语义颜色。
- 保持单 `app` 模块，以两个字段切片和共享 Karoo/传动边界组织代码。

## Implementation Changes

### 数据源与共享接口

- 在 version catalog 中加入 `kotlinx-coroutines-core` 和 `kotlinx-coroutines-test` 1.11.0。
- 在 `core/karoo` 增加可取消的 Flow adapter：
  - `streamStates(typeId)`：包装 `OnStreamState.StartStreaming`。
  - `userProfile()`：提供 distance/elevation 单位偏好。
  - `rideState()`：提供 `Idle / Recording / Paused`。
  - 所有 `callbackFlow` 在 `awaitClose` 中移除 consumer。
- 使用以下 SDK 类型：
  - Field 1：`PERCENT_MAX_FTP` 和 `CLIMB`。
  - Field 2：`HEART_RATE`、`CADENCE`、`SHIFTING_REAR_GEAR`。
  - `%FTP` 直接使用 SDK 当前百分比，不用 3 秒功率自行计算。[SDK 类型表](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-data-type/-type/index.html)
- `startStream` 只暴露中心指标：
  - Field 1 将 `PERCENT_MAX_FTP` 映射为自身 `Field.SINGLE`。
  - Field 2 将 `CADENCE` 映射为自身 `Field.SINGLE`。
  - 中心源的非 Streaming 状态原样传播；缺少所需 field 时返回 `NotAvailable`。

### 状态、格式化与生命周期

- 为两个字段分别定义不可变状态和纯 reducer；每个子值独立可用，单个 stream 失效只清空对应位置。
- `Searching`、`Idle`、`NotAvailable`、缺失字段和非有限数值在图形视图中统一格式化为 `--`。
- RideState 规则：
  - `Recording`、`Paused`：显示 SDK 仍提供的有效值。
  - 明确收到 `RideState.Idle`：两个字段全部清为 `--`，并在再次进入有效骑行状态前忽略后续传感器值。
  - RideState 尚未首次返回或订阅失败时不阻塞其他数据，避免整个字段永久空白。
- 单位转换：
  - SDK climb 值按米解释；`UserProfile.preferredUnit.distance/elevation` 决定公英制。
  - 用户资料尚不可用时，Field 1 左右显示 `--`，中心 `%FTP` 正常工作。
  - 距离绝对值小于 `1 km/mi` 时显示整数 `m/ft`，否则显示一位小数 `km/mi`。
  - 剩余爬升始终显示整数 `m/ft`。
  - 负的距离和爬升不钳制；完成单位转换后保留负号。
- 其他格式：
  - `%FTP`、心率、踏频使用四舍五入整数。
  - `%FTP=0`、踏频 `0 rpm` 有效；心率 `<=0` 显示 `--`。
  - 负的 `%FTP`、心率或踏频视为非法并显示 `--`。
- 将齿数计算与 SDK values 映射移到共享边界，供旧字段和 Field 2 使用：
  - 优先采用正整数 `SHIFTING_REAR_GEAR_TEETH`。
  - 缺失时按 XG-1371 13 速表推断；系统明确报告非 13 速则不推断，总档数缺失时仍按 13 速推断。
  - 直接值显示 `21T`，推断值显示 `≈21T`；旧独立字段同步采用该格式。
- 每个 live view 先立即绘制全 `--`，之后把合并状态 `distinctUntilChanged + sample(1s)`，每秒至多提交一次最新 RemoteViews，满足 SDK 约 1 Hz 的更新限制。[ViewEmitter 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/internal/Emitter.kt)
- Field stream、view 和所有子订阅均由 emitter cancellable 取消；退出页面后不得残留 consumer。

### 图形布局

- 继续使用原生 XML `RemoteViews`，不为纯文本三列额外引入 Glance。
- 新建共享三列布局：
  - 左/中/右权重为 `30 / 40 / 30`。
  - 每格水平、垂直居中，忽略 `ViewConfig.alignment`。
  - 中间字号为 `config.textSize × 1.15`，左右为 `× 0.80`。
  - 固定白色粗体，不做功率区间或心率区间变色。
  - 不显示标签、图标、分隔线，也不注册点击 action。
  - 允许空间不足时自然换行；最多两行，不省略、不自动缩小。
- 布局模式由 `ViewConfig.gridSize.first` 决定：
  - 等于 `60`：显示三列。
  - 小于 `60`：左右 `GONE`，中心占满宽度。
- 两个新字段发送 `UpdateGraphicConfig(showHeader=false)`；旧字段继续 `showHeader=true`。
- deterministic preview：
  - Field 1：`2.3 km | 87% | 186 m`
  - Field 2：`156 bpm | 92 rpm | ≈21T`
  - 窄幅预览同样只保留中心值。

### 注册、资源与文档契约

- 在 `extension_info.xml`、`DataFieldRegistry`、字符串资源和 `docs/data-fields.md` 同步加入两个字段；三个字段的注册顺序为：
  1. `rear-cog-teeth`
  2. `climber-field-1`
  3. `climber-field-2`
- 两个新 DataType 均为 `graphical="true"`，复用现有扩展图标。
- 文档记录每格数据源、单位、紧凑模式、预览、局部失效、暂停/重置和约 1 Hz 刷新规则。
- 更新旧齿数字段文档，将 `21′` 改为 `≈21T`，保留其 header 和独立字段用途。
- 不修改 extension ID、applicationId 或现有 typeId。

## Tests

- Formatter/Calculator：
  - 公制和英制的阈值、四舍五入、负值、零值、NaN/Infinity。
  - `%FTP=0`、cadence=0、HR=0/负值。
  - 直接齿数、13 速推断、缺少总档数、非 13 速和越界档位。
- Reducer/state：
  - 三项全部正常、任意一项缺失、CLIMB 只缺一个 field。
  - `Searching/NotAvailable` 只清对应格。
  - 无活动爬坡时 Field 1 左右为 `--`、中心继续更新。
  - Paused 保留实时数据；明确 Idle 后全部清空。
- View model：
  - `gridSize.width == 60` 为三列，其他宽度为中心单值。
  - 字段名称、预览文本、字号比例、固定位置和无 header 契约。
- Flow/lifecycle：
  - 1 秒内多次上游更新只绘制最新状态。
  - 首次占位立即绘制，后续更新不超过 1 Hz。
  - 取消 field/view 后所有 Karoo consumer 被移除且不再发射。
- Registry：
  - XML 与 Kotlin typeId 集合完全相等且唯一，最终集合包含三个 ID。
- 验证命令：
  - 快速：`.\gradlew.bat testDebugUnitTest`
  - 完整：`.\gradlew.bat lint test assembleDebug`
  - 当前基线 `testDebugUnitTest` 已通过。

## New Karoo 2024 验收

- 字段选择器能发现三个字段，名称和顺序正确。
- Climber 全宽时两个新字段均显示三项且没有系统 header；半宽时只显示中心数据和单位。
- 页面编辑预览固定可复现，文字不截断；记录极端长度下自然换行的实际效果。
- 实测公制/英制切换、无活动爬坡、传感器断线、零踏频、暂停、结束和重新开始。
- 实测直接齿数与 1/6/13 档推断值，确认档位方向和 `≈…T` 格式。
- 退出并重进 Climber，确认没有重复订阅、残留旧值或空白视图。
- Debug 日志记录首次 `ViewConfig.gridSize/viewSize` 和 climb 原始样本，用于确认：
  - New Karoo 全宽确为 60、半宽确为 30。
  - 隐藏 header 后的可用高度。
  - `DISTANCE_TO_TOP`、`ELEVATION_TO_TOP` 原始值确为米。
- 若上述三个设备事实不符合假设，只调整尺寸分类或单位 adapter，不改变字段状态、格式化和布局契约。

## Assumptions

- SDK `%FTP` 是百分数值本身，例如 `87` 显示为 `87%`，不乘以 100。
- New Karoo Climber 使用深色背景；由于 SDK 不提供宿主文字色，按已确认决策固定使用白色。
- 自然换行和原样显示负剩余量是明确接受的产品行为，即使它们可能降低布局稳定性。
- 本次不增加设置页、FTP 输入、通用飞轮配置、Compose/Glance、DataStore 或其他设备兼容承诺。
