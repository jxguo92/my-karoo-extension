# Data Fields

## Current Rear Cog Teeth

- `typeId`: `rear-cog-teeth`
- 显示名称：`Rear Cog Teeth`
- 数据来源：Karoo `SHIFTING_REAR_GEAR` stream
- 单位：齿数（整数）
- 图形字段：是
- 固定预览：`21'`

字段优先读取 `SHIFTING_REAR_GEAR_TEETH`。该值存在且为正整数时，直接显示齿数，
例如 `21`。

实时齿数缺失时，字段按 SRAM CS-XG-1371-E1 的 13 片飞轮规格，从
`SHIFTING_REAR_GEAR` 序数推测齿数。当前映射假设序数从最大飞轮片开始；该方向与
SRAM 安装文档中“第 6 档为 21T”一致，但仍需在目标 Karoo 与后拨组合上实测确认：

```text
档位：  1  2  3  4  5  6  7  8  9  10 11 12 13
齿数： 46 38 32 28 24 21 19 17 15 13 12 11 10
```

推测值末尾追加单引号，例如第 6 档显示 `21'`。若系统同时报告飞轮片数且不是 13，
字段不会套用该型号映射。序数缺失、越界或非整数，以及 `NotAvailable`、断线等情况
显示 `--`。`Idle` 和 `Searching` 状态由 stream 原样传递。字段不累计骑行状态，暂停
或重置后继续显示传感器当前值；字段或视图取消时立即移除各自的 Karoo consumer。

飞轮规格来源：[SRAM CS-XG-1371-E1 官方产品页](https://www.sram.com/en/service/models/cs-xg-1371-e1)。

真机仍需确认：扩展和字段可被发现、直接齿数是否不带标记、缺少实时齿数时 1/6/13
档分别显示 `46'`/`21'`/`10'`、断线显示 `--`、各字段尺寸和左右/居中对齐正常，
退出或重进数据页后没有残留订阅。
