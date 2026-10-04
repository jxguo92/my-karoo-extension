# Data Fields

## Climber Field 1

- `typeId`: `climber-field-1`；显示名称：`Climber Field 1`；图形字段，无 header。
- 全宽（`gridSize.first == 60`）：左侧 `CLIMB.DISTANCE_TO_TOP`、中心 `PERCENT_MAX_FTP`、右侧 `CLIMB.ELEVATION_TO_TOP`。
- 窄幅：只显示中心 `%FTP`。固定预览：`2.3 km | 87% | 186 m`，窄幅为 `87%`。
- 左右按用户资料的 distance/elevation 偏好分别转换公英制；资料未到达时仅左右为 `--`。距离小于 1 km/mi 用整数 m/ft，否则一位小数 km/mi；爬升为整数 m/ft。保留负值。

## Climber Field 2

- `typeId`: `climber-field-2`；显示名称：`Climber Field 2`；图形字段，无 header。
- 全宽：左侧 `HEART_RATE`（bpm）、中心 `CADENCE`（rpm）、右侧 `SHIFTING_REAR_GEAR` 的齿数（T）。
- 窄幅：只显示中心踏频。固定预览：`156 bpm | 92 rpm | ≈21T`，窄幅为 `92 rpm`。
- 正整数直接齿数优先；缺失时按 XG-1371 13 速表推断并加 `≈`，明确报告非 13 速时不推断。

两个组合字段均把中心指标映射为自身的 `SINGLE` stream。各来源独立失效：无活动爬坡只清 Field 1 左右，传感器断线只清对应位置；缺失、非有限值及不可用状态显示 `--`。零 `%FTP` 和零踏频有效，零或负心率不可用。暂停时继续显示有效实时值；明确收到骑行 `Idle` 后全清，直到重新进入 Recording/Paused 才接收传感器值。实时视图先立即绘制占位，后续合并状态每秒最多刷新一次；取消时移除订阅。

## Current Rear Cog Teeth

- `typeId`: `rear-cog-teeth`
- 显示名称：`Rear Cog Teeth`
- 数据来源：Karoo `SHIFTING_REAR_GEAR` stream
- 单位：齿数（整数）
- 图形字段：是
- 固定预览：`≈21T`

字段优先读取 `SHIFTING_REAR_GEAR_TEETH`。该值存在且为正整数时，直接显示齿数，
例如 `21T`。

实时齿数缺失时，字段按 SRAM CS-XG-1371-E1 的 13 片飞轮规格，从
`SHIFTING_REAR_GEAR` 序数推测齿数。当前映射假设序数从最大飞轮片开始；该方向与
SRAM 安装文档中“第 6 档为 21T”一致，但仍需在目标 Karoo 与后拨组合上实测确认：

```text
档位：  1  2  3  4  5  6  7  8  9  10 11 12 13
齿数： 46 38 32 28 24 21 19 17 15 13 12 11 10
```

推测值前加约等号，例如第 6 档显示 `≈21T`。若系统同时报告飞轮片数且不是 13，
字段不会套用该型号映射。序数缺失、越界或非整数，以及 `NotAvailable`、断线等情况
显示 `--`。`Idle` 和 `Searching` 状态由 stream 原样传递。字段不累计骑行状态，暂停
或重置后继续显示传感器当前值；字段或视图取消时立即移除各自的 Karoo consumer。

飞轮规格来源：[SRAM CS-XG-1371-E1 官方产品页](https://www.sram.com/en/service/models/cs-xg-1371-e1)。

真机仍需确认：扩展和字段可被发现、直接齿数是否不带标记、缺少实时齿数时 1/6/13
档分别显示 `≈46T`/`≈21T`/`≈10T`、断线显示 `--`、各字段尺寸和左右/居中对齐正常，
退出或重进数据页后没有残留订阅。

## Concentric Dashboard

- `typeId`: `concentric-dashboard`；显示名称：`Concentric Dashboard`。追加在已有字段之后。
- 仅整页 `gridSize == (60, 60)` 显示，无 header、点击、设置或数值中心 stream；其他尺寸透明空白，不订阅数据或请求天气。
- 固定公制。实时来源：`SMOOTHED_3S_AVERAGE_POWER`、`CADENCE`、`HEART_RATE`、`SPEED`、`ELEVATION_GRADE`；辅助来源：`DISTANCE`、`AVERAGE_SPEED`、`AVERAGE_HR.AVG_HR`、`AVERAGE_POWER`、`NORMALIZED_POWER`。速度 m/s 转 km/h、距离 m 转 km。
- 后拨档位序号/总档数来自 `SHIFTING_REAR_GEAR`，例如 `6/13`，总数缺失显示 `6`，不推算齿数。
- FTP、最大/静息心率来自 `UserProfile`，不用自定义 zones。功率采用 Coggan 七区，41%–200% FTP 等弧长、区内线性插值，边界归较低区；HRR 六区给心率着色，41% 以下普通文字色。资料无效时保留读数、隐藏相应分区提示。
- 踏频 50–110 连续填充，≤80 白/日间深灰、81–100 绿、>100 红。零功率/踏频/速度有效，心率必须 >0；坡度可负。缺失、非有限值、断线、`NotAvailable` 独立显示 `--`，静态轨道与刻度保留。
- 暂停继续显示实时值；Idle 清空骑行读数和朝向，直到 Recording/Paused 才恢复接收。天气缓存与刷新独立于骑行状态，Idle 的风向位置显示 `--`。
- `LOCATION.LOC_BEARING` 在速度 ≥5 km/h 时更新，否则保持本次骑行最近有效朝向。优先同一样本 `LOC_SPEED`（SDK 1.1.9 没有对应常量，兼容 `FIELD_LOC_SPEED_ID` 与 `LOC_SPEED` 两种键），否则最近有效 `SPEED`。`FIELD_LOC_SPEED_ID` 按 SDK 其他 LOCATION 键命名推断，实际键及 m/s 单位待真机确认。最终角度四舍五入到10°。本次骑行无朝向时隐藏指北针，风向位置显示 `--`。
- 天气使用已有和风配置与 Wi-Fi/手机 HTTP 路由；无配置或有效位置不请求。首次位置、距成功位置 >5 km 或成功后满30分钟刷新；失败后2分钟重试，同一视图最多一请求，失败保留成功缓存，超过60分钟风向位置显示 `--`。使用单调时钟，取消视图/Extension 时取消任务和请求。
- 相对风向为气象风向 +180° −朝向，朝向、气象风向和持续风速均有效时，静风 <0.3 m/s 留空；缺失/无效数据、天气过期或 Idle 时，在原箭头中心用普通文字色显示 `--`；<3.4、<5.5、<8、≥8 m/s 四档仅以颜色表达。**Q7 明确例外：本字段不显示天气来源标注**；通用 `CurrentWeather.attributions` 契约保持不变。
- Canvas 位图通过 RemoteViews 展示，设计尺寸480×638，等比缩小并居中，不超过设计分辨率；无效 viewSize 使用设计尺寸。日夜布局一致，`UI_MODE_NIGHT_YES` 夜间，其余日间。立即首帧，此后每秒检查显示状态和主题，仅变化时提交，每张位图独立。
- 固定预览：245 W、88 rpm、156 bpm、28.6 km/h、6/13、42.7 km、+4.8%；平均速度27.4、平均心率148、平均功率198、标准化功率214；FTP200、最大/静息心率190/50，朝向315°、气象风向270°、持续风速4 m/s。预览不订阅传感器或天气，仍响应主题。
- 真机待验：整页布局和位图传输、主题是否传播至扩展进程、断线/暂停/结束、Wi-Fi及手机天气、重复进出页面后无残留订阅。模拟器与 JVM 检查不代替这些验收。

## Ladder Dashboard

- `typeId`: `ladder-full`；显示名称：`Ladder Dashboard`。位于 Concentric Dashboard 之后，图形字段，无 header，沿用 SDK 默认 numeric stream。
- 只接受整页 `gridSize == (60, 60)`；其他尺寸完全空白，不订阅传感器或天气。`viewSize` 只决定绘制缩放，不决定整页资格。固定方案 D（功率核心 · 大字），无点击、布局选择或持久化。
- 固定深色、公制 km/h 与 km；这是对单位偏好要求的明确例外。中央为 `SMOOTHED_3S_AVERAGE_POWER`（W），左侧 `SPEED`（m/s ×3.6，0–60 km/h、六等段、固定青蓝），右侧 `HEART_RATE`（bpm，HRR 六区）。底部 `AVERAGE_POWER`、`AVERAGE_SPEED`、`AVERAGE_HR.AVG_HR`、`NORMALIZED_POWER`、`DISTANCE`（m ÷1000）。坡度 `ELEVATION_GRADE` 一位小数%，后拨 `SHIFTING_REAR_GEAR` 显示序号/总数 `6/13`，总数缺失只显示序号。
- 功率用 `UserProfile.ftp` 的 Coggan 七区，覆盖 41%–200% FTP；心率用 `maxHr`/`restingHr` 的 HRR 六区，覆盖 41%–100%。不用 Karoo 自定义 zones。每区等宽，区内线性插值、边界归较低区；心率六区颜色跳过七色中的第5色。无效资料只保留对应空轨道和真实读数。
- 踏频 `CADENCE` 只显示12颗灯珠：51–110 rpm 每5 rpm一颗，51–80 白、81–100 绿、101–110 红；<51 或缺失全部暗，88 rpm 点亮8颗。零功率/速度/踏频有效，心率≤0无效。数字超范围保留原值，仅钳制填充。各源 Searching、NotAvailable、缺字段、非有限值独立清除对应数字/填充，数字显示 `--`，静态轨道及刻度保留。
- 暂停保持实时显示；Idle 清空所有骑行读数和朝向，隐藏指北针、风向位置显示 `--`，忽略传感器读数直到 Recording/Paused；RideState 未到达时允许显示。
- 复用 Concentric 的 `DashboardState`、`DashboardCalculator`、Karoo 订阅和 LOCATION 兼容转换。朝向更新门限5 km/h；同一样本 LOCATION 速度优先，缺失回退最近有效 SPEED，低速或朝向缺失保持。`FIELD_LOC_SPEED_ID`/`LOC_SPEED` 两种键的真实值和 m/s 单位仍待真机确认。未有有效朝向时隐藏指北针，风向位置显示 `--`；角度按10°四舍五入。
- 相对风向角为气象风向+180°−车头朝向，顶风朝下、顺风朝上；双色菱形指北针角为−车头朝向，北红南灰，北端标N。风使用持续风速，朝向、气象风向和持续风速均有效时，<0.3 m/s静风留空；<3.4、<5.5、<8、≥8四档颜色为浅灰白/青绿/黄/红。缺失/无效朝向、风速或风向、天气过期或 Idle 时，在原箭头中心用普通文字色显示 `--`；保留有效指北针。
- 复用 `DashboardWeather`/`DashboardWeatherPolicy`，每视图独立缓存：有位置即首次请求，距成功位置>5 km或成功后满30分钟刷新，失败后2分钟重试，超过60分钟过期。无和风配置不请求；天气与骑行状态独立。请求经既有 Wi-Fi/手机 HTTP 路由，取消视图或 Extension 时取消全部订阅与天气任务。不显示和风归因（个人使用的明确例外，公开分发前重新评估）。
- Canvas 设计坐标480×638，按有效 viewSize 等比缩放并居中，余区填深色背景，无效尺寸回退480×638。每视图复用一张 ARGB_8888 位图，提交返回后才重绘，每次新建 RemoteViews。首帧立即占位，之后每秒检查、变化时提交，提交失败记录无敏感信息日志并重试。
- 固定预览：功率245 W、心率156、踏频88、速度28.6、坡度3.6%、档位6/13；AVG PWR198、AVG SPD27.4、AVG HR148、NP214、DIST42.7；FTP200、最大/静息心率190/50、车头315°、气象风向270°、持续风速4 m/s。风箭头140°青绿，指北针50°，预览不联网。
- 真机待验：字段可发现/添加、整页与其他尺寸、方案 D 的字体/单位间距及裁切、约1.2 MB位图每秒Binder传输和内存、静止/低速/转弯、坡度与LOCATION速度单位、断线/暂停/结束/重新开始、Wi-Fi/手机天气及过期恢复、反复进出无残留订阅。当前无连接模拟器/设备，Canvas仪器检查尚未运行。
