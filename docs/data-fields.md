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
- 暂停继续显示实时值；Idle 清空骑行读数和朝向，直到 Recording/Paused 才恢复接收。天气缓存与刷新独立于骑行状态，Idle 不显示风向。
- `LOCATION.LOC_BEARING` 在速度 ≥5 km/h 时更新，否则保持本次骑行最近有效朝向。优先同一样本 `LOC_SPEED`（SDK 1.1.9 没有对应常量，使用 wire key），否则最近有效 `SPEED`。最终角度四舍五入到10°。本次骑行无朝向时隐藏指北针和风箭头。
- 天气使用已有和风配置与 Wi-Fi/手机 HTTP 路由；无配置或有效位置不请求。首次位置、距成功位置 >5 km 或成功后满30分钟刷新；失败后2分钟重试，同一视图最多一请求，失败保留成功缓存，超过60分钟隐藏风箭头。使用单调时钟，取消视图/Extension 时取消任务和请求。
- 相对风向为气象风向 +180° −朝向，静风 <0.3 m/s 隐藏；<3.4、<5.5、<8、≥8 m/s 四档仅以颜色表达。**Q7 明确例外：本字段不显示天气来源标注**；通用 `CurrentWeather.attributions` 契约保持不变。
- Canvas 位图通过 RemoteViews 展示，设计尺寸480×638，等比缩小并居中，不超过设计分辨率；无效 viewSize 使用设计尺寸。日夜布局一致，`UI_MODE_NIGHT_YES` 夜间，其余日间。立即首帧，此后每秒检查显示状态和主题，仅变化时提交，每张位图独立。
- 固定预览：245 W、88 rpm、156 bpm、28.6 km/h、6/13、42.7 km、+4.8%；平均速度27.4、平均心率148、平均功率198、标准化功率214；FTP200、最大/静息心率190/50，朝向315°、气象风向270°、持续风速4 m/s。预览不订阅传感器或天气，仍响应主题。
- 真机待验：整页布局和位图传输、主题是否传播至扩展进程、断线/暂停/结束、Wi-Fi及手机天气、重复进出页面后无残留订阅。模拟器与 JVM 检查不代替这些验收。
