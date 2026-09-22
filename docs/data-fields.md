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
