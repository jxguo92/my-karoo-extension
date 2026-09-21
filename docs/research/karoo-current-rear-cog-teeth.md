# Karoo SDK 当前飞轮齿数能力

## 结论

官方 `karoo-ext` 可以获取当前飞轮齿数，但该值不是 `SHIFTING_REAR_GEAR` 本身。

- `Field.SHIFTING_REAR_GEAR`：当前后拨位置/档位序数，必需字段。
- `Field.SHIFTING_REAR_GEAR_TEETH`：当前飞轮片的实际齿数，可选字段。
- `Field.SHIFTING_REAR_GEAR_MAX`：飞轮总片数，可选字段。

官方源码对后齿数据类型的说明是：`SHIFTING_REAR_GEAR` 包含必需的档位字段，以及可选的 `SHIFTING_REAR_GEAR_TEETH` 和 `SHIFTING_REAR_GEAR_MAX`。因此，扩展应优先读取 `SHIFTING_REAR_GEAR_TEETH`；不能把档位序数直接当作齿数。

## 订阅方式

通过 `KarooSystemService.addConsumer` 订阅 `OnStreamState`，参数使用：

```kotlin
OnStreamState.StartStreaming(DataType.Type.SHIFTING_REAR_GEAR)
```

收到 `StreamState.Streaming` 后，从 `DataPoint.values` 读取：

```kotlin
val rearCogTeeth = dataPoint.values[
    DataType.Field.SHIFTING_REAR_GEAR_TEETH
]
```

`DataPoint.values` 的类型是 `Map<String, Double>`，所以读取结果是 `Double?`。字段为可选字段，缺失时必须按 `null` 处理，并覆盖 `Idle`、`Searching`、`NotAvailable` 和断开场景。

示意代码：

```kotlin
val consumerId = karooSystem.addConsumer<OnStreamState>(
    params = OnStreamState.StartStreaming(DataType.Type.SHIFTING_REAR_GEAR),
    onEvent = { event ->
        val teeth = (event.state as? StreamState.Streaming)
            ?.dataPoint
            ?.values
            ?.get(DataType.Field.SHIFTING_REAR_GEAR_TEETH)
    },
)
```

实际项目中应把 `consumerId` 在字段或 Extension 生命周期结束时传给 `removeConsumer`。

## 版本注意

官方 `karoo-ext` 1.1.8 的 release note 写明该版本“导出最新 data types 和 fields”，并要求 Karoo/K2 使用 KOS 1.613.2351 或更新版本；官方包页面显示目前仓库发布过 1.1.9。项目应以实际目标设备的 KOS 版本和 Gradle 依赖为准。

如果目标设备或 SDK 版本低于字段导出的版本，编译期可能没有该常量，或者运行时没有该可选字段。即使常量存在，也不能假设所有后拨/变速来源都会提供齿数。

## 相关但不同的能力

`SavedDevices.SavedDevice.gearInfo.rearTeeth` 是用户为整套飞轮配置的齿数列表，适合做配置或缺少实时齿数字段时的备用映射；它不是实时当前飞轮片值。实时数据仍应优先使用 `SHIFTING_REAR_GEAR_TEETH`。若自行用档位序数映射 `rearTeeth`，需要在真机和具体传感器上确认序数的起始约定。

## 官方来源

- [官方 `DataType.kt` 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/DataType.kt)：`Source` 和 `Type` 对前后档字段的定义。
- [官方 `DataPoint.kt` 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/DataPoint.kt)：流数据使用 `values: Map<String, Double>`。
- [官方 `KarooEvent.kt` 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt)：`OnStreamState.StartStreaming`、`SavedDevice.GearInfo`。
- [官方 `StreamState.kt` 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/StreamState.kt)：`Streaming`、`Searching`、`NotAvailable` 等状态。
- [官方 1.1.8 release note](https://github.com/hammerheadnav/karoo-ext/releases)：最新字段导出及 KOS 版本要求。
