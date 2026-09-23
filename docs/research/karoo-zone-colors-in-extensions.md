# Karoo 扩展如何给心率、功率与速度着色

> 2026-09-23 核查。样本选自社区的 [awesome-karoo 扩展列表](https://github.com/timklge/awesome-karoo)，并以 Hammerhead SDK 文档、各项目自己的源码或 README 为依据。“热门”指该列表收录且与题目直接相关，不代表安装量排名。GitHub 源码链接中 Powerbar 固定到已核查提交；其他项目链接指向核查时主分支，后续可能变化。

## 先区分“分区阈值”和“分区颜色”

Karoo SDK 的 `UserProfile` 暴露 `heartRateZones`、`powerZones`、`maxHr`、`restingHr`、`ftp` 与单位偏好，但**没有 `speedZones`**。`UserProfile.Zone` 只有 `min` 和 `max`，**没有颜色字段**。所以扩展可直接沿用用户在 Karoo 上设定的心率/功率边界，但若想着色，仍须自己提供调色板；速度颜色则需自定义比较基准和阈值。这里说的是已公开 SDK 的接口，不代表 Karoo 自身屏幕没有内部配色。[SDK `UserProfile`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/index.html)、[SDK `Zone`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-user-profile/-zone/index.html)

## 心率与功率：使用 Karoo 边界，扩展自行配色

### Barberfish：区间和调色板分离，并处理昼夜对比度

`powerZone`、`hrZone` 接收 Karoo `UserProfile.Zone` 列表，按顺序找首个 `value <= zone.max` 的区；恰好等于上界仍算该区。超过所有上界落入末区；空列表回退到 1 区。颜色在同文件的本地数组中定义，默认 `KAROO`，也可选 Wahoo、Intervals.icu、Zwift、HSLuv。它的 `KAROO` 表为 7 个功率色（深绿、薄荷绿、黄、鲑红、橙、红、紫）和 5 个心率色（深绿、薄荷绿、黄、鲑红、红）；这是**项目写在源码里的仿 Karoo 调色板**，不是 SDK 返回的颜色。[分区判定与越界处理](https://github.com/jpweytjens/Barberfish/blob/master/app/src/main/kotlin/com/jpweytjens/barberfish/datatype/shared/ZoneColoring.kt)、[颜色数组](https://github.com/jpweytjens/Barberfish/blob/master/app/src/main/kotlin/com/jpweytjens/barberfish/datatype/shared/ZoneColoring.kt)、[调色板选择](https://github.com/jpweytjens/Barberfish/blob/master/app/src/main/kotlin/com/jpweytjens/barberfish/datatype/shared/ZoneColoring.kt)

每字段可选 `None`、`Text`、`Fill`：分别不着区色、给数值着色、给整个字段填色。Text 模式为浅/深主题预计算不同的可读颜色；项目文档称使用 APCA 对比度阈值 `|Lc| 45`，用 HSLuv 调整亮度。Fill 模式保留原色，再从黑/白中选择对比度更高的文字色。项目 README 还明确速度采用单个固定目标或滚动平均、平均速度和踏频可采用目标或范围阈值，所以其“速度颜色”与心率/功率训练区不同。[README 着色和速度阈值说明](https://github.com/jpweytjens/Barberfish#zone--grade-coloring)、[调色板设计文档](https://github.com/jpweytjens/Barberfish/blob/master/docs/color-palettes.md)、[填色文字选择函数](https://github.com/jpweytjens/Barberfish/blob/master/app/src/main/kotlin/com/jpweytjens/barberfish/datatype/shared/ZoneColoring.kt)

### Karoo Powerbar：区色只管颜色，进度条另用数值范围

Powerbar 同时支持功率和心率。流值仅在 `Streaming` 且有 `singleValue` 时进入着色分支；开启 `useZoneColors` 后，把 `userProfile.powerZones` 或 `heartRateZones` 传给 `getZone`，再用该区对应的应用资源色。关闭开关用 `zone0`；无读数时进度为 `null`、标签 `?`、颜色也回到 `zone0`。源码在 `getZone(...)` 未取得区时回退到 `zone7`；这不等于 SDK 定义了第七心率区。[心率流与颜色](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt)、[功率流与颜色](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt)

它把进度位置单独归一化：心率默认从静息心率到最大心率，功率默认从首区 `min` 到末区 `min + 30 W`，两者都允许用户覆写显示范围。训练目标也用同一范围映射；**区间颜色和条的位置不是一回事**。项目对功率色的设计无法仅由这段代码反推出固定的百分比 FTP 阈值，因为实际阈值来自用户档案。[心率显示范围](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt)、[功率显示范围](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt)

### Vin-HkE：公开产品说明可确认边界来源，无法核对内部算法

Vin-HkE README 称彩色功率字段、心率字段及多值字段的区色为蓝到紫，并分别依据 Karoo 用户档案的功率区、心率区；连续功率曲线的片段也随功率区变色。该项目在 awesome-karoo 中标为闭源，因此这里**不能断言**它在等于边界、缺失值、颜色十六进制值或主题切换时的内部处理。[Vin-HkE 彩色字段与曲线说明](https://github.com/maduwatas/Vin-HkE#colored-fields)、[awesome-karoo 的许可与功能条目](https://github.com/timklge/awesome-karoo#vinhke)

### BigNum：零值和无分区回到中性状态

BigNum 的作者 README 说明心率/功率边界取 Karoo 用户档案，5 个心率区和 7 个功率区使用仿设备设置页的颜色；用户可关闭着色、只给数字着色，或填满字段并自动选择黑/白文字。**无分区的字段、数值为零、档案没有配置分区**都会保持普通文字色且不填背景；README 明确将速度列为没有训练区、始终保持普通色的字段。这里是另一个项目的视觉策略，不应推断为 SDK 对零值的规定。[BigNum 作者 README：分区着色](https://github.com/smartycoder/karoo-bignum#zone-coloring)、[同页截图及速度说明](https://github.com/smartycoder/karoo-bignum#screenshots)

## 速度：相对目标/平均值的状态色

### karoo-colorspeed：以比较速度的百分比做多档颜色

它提供当前速度对骑行平均、上一圈平均、目标速度，以及本圈平均对上一圈/目标的字段。共用视图先算 `Int(currentSpeed / referenceSpeed * 100)`；当前或参考值非正时比值设为 0。默认分界是 `50、65、95、105、110、120%`，可在设置中修改；校验要求这些分界严格递增，停止阈值及目标速度非负。默认停止阈值 `0.9 m/s`，绘制前乘以当前速度单位换算系数。[字段种类](https://github.com/currand/karoo-colorspeed#features)、[默认值与校验](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/ConfigData.kt)、[比值与单位](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/ColorSpeedView.kt)

打开背景色时，停止阈值及以下透明；`<50%` 深红，`50–<65%` 浅红，`65–<95%` 橙，`95–105%` 浅色居中，`>105–<110%` 浅绿，`110%` 及以上深绿。配置里虽有 `120%` 阈值，当前绘制代码在 110–<120 与 ≥120 两支都给深绿背景、同样的条形级别 5，因此它目前不产生额外视觉档位。居中判断用 Kotlin 闭区间，因此**恰为 95% 或 105% 都算居中**；各比较在转换为 `Int` 后执行，故例如 105.9% 被截断到 105 仍算居中。关闭背景色时透明并用常规文字色；在其当前实现里缺失的速度流被送入视图作为 `0.0`，会进入停止态。[背景与文字色分支](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/ColorSpeedView.kt)、[条形级别](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/ColorSpeedView.kt)、[缺失值输入](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/CurrentVsRideAverageSpeed.kt)

### Average Speed Colour：三态比较，目标附近留白

作者 README 定义绿为高于目标/平均，红为低于目标/平均，目标附近 `±1` 为无色；可把绿色换为青色。源码先按用户偏好把速度从 m/s 转为 mph 或 km/h，才相减；所以这里的 `±1` 是 **1 mph 或 1 km/h**。`差值 > 1` 着绿/青、`< -1` 着红，**恰好等于 ±1 保持中性**；当前速度不高于 2 km/h 也先走透明停止态。当前或参考流非 `Streaming` 时该字段将两者传为 `0.0`，同样显示停止态。五个字段都是与骑行平均、上一圈平均或自定义目标比较。[项目 README 的颜色键、字段及设置](https://github.com/j4m1eb/Karoo-Average-speed-colour#how-it-works)、[三态和停止阈值](https://github.com/j4m1eb/Karoo-Average-speed-colour/blob/main/app/src/main/kotlin/com/j4m1eb/averagespeedcolour/data/ColorSpeedView.kt)、[单位换算与无读数处理](https://github.com/j4m1eb/Karoo-Average-speed-colour/blob/main/app/src/main/kotlin/com/j4m1eb/averagespeedcolour/data/CurrentVsRideAverageSpeed.kt)

## 对本项目的可用结论

1. 心率/功率若要与用户的 Karoo 配置一致，读取 SDK 区间 `min/max`，再把区号映射到本地颜色表。颜色表应在文档中明确是本项目的选择，不能称作 SDK 颜色。[SDK 档案和区间](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/index.html)、[Barberfish 实现](https://github.com/jpweytjens/Barberfish/blob/master/app/src/main/kotlin/com/jpweytjens/barberfish/datatype/shared/ZoneColoring.kt)
2. 速度需要先定比较语义：固定目标、骑行平均或圈平均。若想表达“低于/达到/高于配速”，三态或目标附近中性色更直观；若想表达偏差幅度，可用多档百分比，但应避免在平均值为零时做除法，并明确取整及等于阈值时的归属。[Average Speed Colour](https://github.com/j4m1eb/Karoo-Average-speed-colour#how-it-works)、[karoo-colorspeed 的比值和分支](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/ColorSpeedView.kt)
3. 先定义正常值、缺失值、停止/暂停和断线各自的视觉状态。Powerbar 用 `?` 和不绘条；karoo-colorspeed 将缺失流送成 `0.0`，视觉上等同停止。若本项目需要区分断线与真正 `0`，应保留空状态直到绘制层，而不是复用停止态。[Powerbar 无读数](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt)、[karoo-colorspeed 无读数](https://github.com/currand/karoo-colorspeed/blob/main/app/src/main/kotlin/com/currand60/karoocolorspeed/data/CurrentVsRideAverageSpeed.kt)
