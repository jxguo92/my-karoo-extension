# Garmin Connect IQ 与 Karoo Extension 坡度字段调研

> 调研日期：2026-09-22。范围是骑行时显示的实时坡度（grade/gradient）；另区分路线前方的预测坡度。资料以 Garmin、Hammerhead 官方文档和第三方字段作者的源码、说明及亲历反馈为主。内置算法未公开的部分只作推断。

## 结论

坡度的基本量是某段路的爬升除以水平前进距离：`grade% ≈ 100 × Δ海拔/Δ距离`。Garmin 的 [Edge 手册](https://static.garmin.com/pumac/edge1040_OM_EN-MY.pdf)也只公开到“rise over run”这一层；**没有找到 Garmin 或 Hammerhead 公开实时内置坡度的采样距离、滤波器、异常值规则和版本差异**。因此不能断言其内部使用固定几秒、几十米或某一种滤波算法。

第三方字段通常并非获得了更精确的专用坡度传感器，而是拿到设备提供的海拔、距离、速度或气压，再自行选择**采样窗口、滤波、异常值处理和刷新时机**。这足以让同一时刻的两个数字不同。也有字段直接复用系统坡度；例如 [Karoo PowerBar 作者](https://support.hammerhead.io/hc/en-us/community/posts/31698680768539-Graphical-power-bar-extension)说明其柱状条可选择系统 current grade 作为数据源。仅凭“第三方”身份不能推定其重新计算。[Garmin `Activity.Info`](https://developer.garmin.com/connect-iq/api-docs/Toybox/Activity/Info.html)公开 `altitude`、`elapsedDistance`、`currentSpeed`、`rawAmbientPressure` 等，并提示字段可能为 `null`；[Karoo Extension SDK 的 `DataType.Type`](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-data-type/-type/index.html)同时公开系统 `ELEVATION_GRADE`、`DISTANCE`、`PRESSURE_ELEVATION_CORRECTION` 与 `LOCATION`。

“更准”多指**更符合骑手眼下所在路段的感觉、更快跟上坡度变化、慢速爬坡不容易归零，或数字更稳定**。这些是不同的优化目标；没有独立测量的道路高程基准、相同长度的对照路段和误差统计，仅看两块屏幕无法证明哪一个更接近真实坡度。

## 已公开的第三方算法

| 平台和实例 | 作者公开的方法 | 对读数的影响 |
| --- | --- | --- |
| Garmin：[GarminSlopeDatafield](https://github.com/mizamae/GarminSlopeDatafield) | 对海拔随距离做线性回归；采样数和海拔过滤系数可调。至少相隔 20 m 才取新样本，并限制突变对应的坡度为 50%。作者说明 GPS 海拔在设备开始活动后可能需要稳定，气压计设备可用较轻的过滤。 | 多点拟合抑制单次高度跳动；窗口和过滤越重越滞后。作者的 97 过滤系数例子约产生 80 m 延迟，这不是所有设备的通用延迟。 |
| Garmin：[Sensible Grade 源码](https://github.com/flyingflo/garmin_data_field_grade/blob/master/source/garmin_data_field_gradeView.mc) | `compute(info)` 读取 `info.altitude` 和 `info.currentSpeed`，累积 3 次速度估计行进距离，求相邻两次海拔差/距离；接近停车时重置，再以可配置长度的平均滤波器平滑。 | 约 3 次 `compute` 的高度差可降低过短分母带来的跳动，但最终刷新周期取决于设备调用节奏。其 [README](https://github.com/flyingflo/garmin_data_field_grade)把它描述为使用气压海拔和轮速、GPS 备用；源码实际读取的是系统提供的 `Activity.Info` 值，并未直接连接传感器。 |
| Garmin：[Advanced Grade 作者说明](https://forums.garmin.com/developer/connect-iq/f/showcase/411900/datafield-advanced-grade-6-clones) | 从可选的压力或海拔源，在默认 40 m 距离上算原始坡度；拒绝超过阈值的结果，然后以移动平均、二阶 Bessel 过滤和变化率规则处理，参数开放。 | 可压制突跳，代价是受窗口与 8 s 默认稳定时间影响。作者对“准确与快速”的平衡是设计选择，非独立精度证明。 |
| Garmin：[Grade Calc 作者讨论](https://forums.garmin.com/developer/connect-iq/f/discussion/384917/grade-calc-code-details) | 最近 11 个点（可设置）分别求平均、中位数和最小二乘坡度，取三个候选值的中间值；检测减速，并在低速/停止时清空队列。 | 异常值不易单独主导结果；低速、起步时可能需要重新积累样本。作者称自己测试时“often better”，属于其观察。 |
| Karoo：[Barberfish 当前说明](https://github.com/jpweytjens/barberfish#algorithms) | 取最近 30 m 的海拔样本，以普通最小二乘直线斜率求 grade，按距离而非固定时间平滑。 | 同一段路在不同速度下仍对应相近的空间窗口；停止时不继续向前推进。30 m 的平滑仍会抹掉更短的陡坡，并在坡度突变处滞后。 |
| Karoo：[Climber+ 作者说明](https://github.com/hazzus/karoo-climber-plus#whats-different-from-the-native-climber) | 使用 Karoo `OnNavigationState` 给的路线、爬坡和海拔曲线；前方 5 个 100 m 坡度块依骑手当前位置持续重算。作者称内置 Climber 的块固定在 100 m 网格。 | 这是**路线前方的预测坡度**，不是以实时气压海拔算的脚下坡度。它可比固定网格的显示更连续，但仍受路线高程、定位和偏航影响。 |

Garmin 开发者[论坛中的另一位字段作者](https://forums.garmin.com/developer/connect-iq/f/discussion/209421/grade-calc---filtered-and-fit)解释，他比较过 `ambientPressure`、`rawAmbientPressure`、`Activity.Info.altitude`，最后对海拔和距离分别过滤，并尝试 Kalman 滤波。这是某设备及作者的试验，不应泛化为 Garmin 所有机型的信号处理。早期 [Barberfish 作者在 Hammerhead 社区的介绍](https://support.hammerhead.io/hc/en-us/community/posts/48060183086491-Barberfish-extension)曾写约 6 s EWMA；当前仓库说明已经写成 30 m 最小二乘，因此本文描述当前公开版本，不把旧说法混用。

## 为什么与内置字段不同

1. **计算的路段不一样。** 以 10 m 爬升跨 100 m 路段为例，整段平均为 10%；但路中若先有 20 m 的 20% 陡坡，再有较缓部分，30 m 窗口与 100 m 窗口在陡坡处必然给出不同读数。固定时间窗口还会随速度变化：10 s 在 3 m/s 时覆盖 30 m，在 10 m/s 时覆盖 100 m。这里是公式推论，不是对某个内置实现的逆向断言。
2. **原始海拔和距离有噪声、量化与延迟。** 例如 20 m 距离上的 1 m 海拔误差就对应 5 个百分点的坡度误差；窗口过短会跳，过长会拖后。GarminSlopeDatafield 作者直接记录了平滑与约 80 m 延迟的取舍；[Hammerhead 高度校准说明](https://support.hammerhead.io/hc/en-us/articles/25322052189595-Karoo-OS-Elevation-Calibration)说明设备以气压计算海拔，需考虑校准。海拔绝对值校准误差若是恒定偏移会在差分中抵消，随时间变化的漂移不会；后一句为数学推论。
3. **输入和异常规则可不同。** 有些字段取系统处理后的海拔/累计距离，有些利用原始气压；可选速度传感器、GPS 或设备里程间接影响距离源。开发者可以规定最小位移、速度阈值、离群点上限、暂停和断线行为。[Garmin `Activity.Info` 文档](https://developer.garmin.com/connect-iq/api-docs/Toybox/Activity/Info.html)显示这些输入可由字段读取且可能缺失；上述三个公开算法展示了不同规则。具体系统距离怎样在传感器间切换，须按设备型号与设置核实。
4. **脚下与前方本来不是同一个量。** 实时坡度用已骑过的一段数据，是带有窗口的回顾估计；路线坡度用预载高程和地图匹配，是对将到路段的预测。[Hammerhead 的 Climber 说明](https://support.hammerhead.io/hc/en-us/articles/25602163627163-Karoo-OS-Climber)明确区分 `Current Grade`、整个爬坡的 `Avg Grade` 和路线各段的平均坡度。[Climber+](https://github.com/hazzus/karoo-climber-plus#how-it-works)称其数据来自 Karoo 导航状态；若与系统实时 Grade 对照，即使两者工作正常也可能不相等。
5. **系统算法和固件会变化。** 当前公开 SDK 并不暴露内置 grade 算法参数；Garmin 开发者[社区讨论](https://forums.garmin.com/developer/connect-iq/f/discussion/376820/grade-calc---community-project/1811335)里也有人认为 Edge 1040/1050 的新版原生显示更灵敏。故“内置总是更慢”不能跨型号、固件成立。

## 为什么许多用户认为第三方更准

- **低速陡坡能继续显示非零值。** [Gradient 字段作者在 Connect IQ 商品页的描述](https://apps.garmin.com/en-US/apps/83c5a6eb-7d2d-483e-bd3a-be7c79477762)说其更新间隔随速度调整，低速时拉长以保持一致性；作者的动机是其使用的 Edge 内置字段在低速时常显示 0。这里是作者对其机型的报告。低速时单位时间内距离小，计算分母更容易接近噪声量级，拉长采样距离的好处有数学依据。
- **过渡时更贴近体感。** 用户更容易记住陡坡开始时系统仍显示平地、坡顶后仍显示爬坡这类滞后。[Edge 820 用户在 Garmin 论坛的亲历对比](https://forums.garmin.com/sports-fitness/sports-fitness/f/edge-820/158433/edge-820-not-showing-grade-above-16/878552)称某 Connect IQ 字段更灵敏但也更跳；这恰好说明响应速度和稳定性在交换，不能把“更快”直接等同于“数值更准”。
- **设置可贴合设备和骑行场景。** 有气压计、速度传感器、慢速山地骑行或路线预览时，适合的窗口不同。公开字段允许调采样长度和过滤强度，用户可选更符合自己期望的显示。作者的参数说明支持这一点，但没有统一的“最优”设置。
- **可解释性增加信任。** 第三方开发者常公开公式、参数和失败条件，使用者知道数字为何变化。相较之下，内置字段公开资料通常只给坡度定义。这可解释部分偏好，但属于心理/使用体验推断，尚无针对这些字段的受控用户研究证明。

同样存在反例：上述 [Gradient 商店评论](https://apps.garmin.com/en-US/apps/83c5a6eb-7d2d-483e-bd3a-be7c79477762)中有用户抱怨第三方字段延迟；[Garmin 坡度算法社区讨论](https://forums.garmin.com/developer/connect-iq/f/discussion/376820/grade-calc---community-project/1811335)也有用户认为新版 Edge 原生坡度已很灵敏。用户评价可以说明感知，不是测量真值。

## 对本项目的直接启示

若后续做 Karoo 坡度字段，应先定义用户要**脚下最近一段的坡度**还是**路线前方一段的坡度**。实时字段可同时显示系统 `ELEVATION_GRADE` 与自行由 `PRESSURE_ELEVATION_CORRECTION`、`DISTANCE` 计算的版本，以便在同一次骑行比较；把窗口距离、最小有效位移、最大合理坡度、停止和缺失值处理写成明确契约。路线预测字段单独命名并标明“前方 100 m”，避免与实时坡度混淆。真机比较时记录相同时间戳的输入和输出，在已知高程的固定路段以相同距离窗口计算参考值，并比较平均误差、峰值误差和路段转换延迟；单凭体感或坡道标牌的整段平均值无法判定实时读数优劣。此段为项目建议，尚未在本仓库实现或验证。
