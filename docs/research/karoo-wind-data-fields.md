# Karoo 风向、风速 Data Field 的数据来源对比

> 调研日期：2026-09-22。样本为 Karoo 上常见的 Headwind、myWindsock、Epic Ride Weather、WindField；“热门”仅指扩展库和社区讨论中常见，并非按安装量排序。以各产品官方说明、Headwind 源码与 Karoo SDK 文档为准。

## 核心结论

这四款都把**外部天气数据**与 Karoo 的当前位置、骑行方向或路线结合，生成风向风速字段。Karoo SDK 的[内置 Data Type 清单](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-data-type/-type/index.html)没有天气/风的内置类型；“风速”通常是当地气象服务的风速或其推算值，不能直接理解为车把处传感器实测风速。后一判断是根据清单与各产品所述数据获取方式作出的推论。

| 扩展 | 可核实的天气来源与取数方式 | 风字段的重点 | 联网、路线及公开程度 |
| --- | --- | --- | --- |
| **Karoo Headwind** | 开源实现。默认请求 Open-Meteo；用户也可提供 One Call API key 改用 OpenWeatherMap。以 GPS 位置请求当前天气和逐小时预报。Open-Meteo 请求包含 `wind_speed_10m`、`wind_direction_10m`、`wind_gusts_10m`。见[项目 README](https://github.com/timklge/karoo-headwind)、[Open-Meteo 请求代码变更](https://git.timklge.de/tim/karoo-headwind/commit/21d06aab3a9d37884ce0efdf642b5b0e3210b52a)。 | 同时提供绝对风速/阵风/风向、相对骑行方向的迎风分量及图形箭头；有未来 12 小时的风预报，加载路线后按路线取预报。见[README](https://github.com/timklge/karoo-headwind)。 | 获得 GPS 定位后自动下载；位置约按 3 km 取整，骑行超过约 3 km 或至迟 1 小时重取，失败时每分钟重试。Karoo 3 可经手机 Companion 上网；路线可选。全部见[README](https://github.com/timklge/karoo-headwind)。 |
| **myWindsock** | 官网确认依靠 GPS 当前位置获取天气，骑前需要数据连接；所取天气可在断网后继续使用，途中保持连接可更新当前位置天气。未在所查 Karoo 文档中公开具体气象 API/供应商。见[Karoo Data Fields](https://mywindsock.com/page/hammerhead-extension/data-fields/)、[Karoo FAQ](https://mywindsock.com/page/hammerhead-extension/hammerhead-extension-faq/)。 | Wind Field 显示平均风速、最大阵风、相对骑行方向的箭头，并用红/蓝区分逆风/顺风。另有迎风时间累计、结合地速与盛行风的 Air Speed、风与坡度合成的 Feels Like Gradient，以及 Forces。见[字段说明](https://mywindsock.com/page/hammerhead-extension/data-fields/)。 | 无须上传路线。风速单位跟随 Karoo 的 mph/kph 设置。Forces 还使用 Karoo 气压/海拔数据；有功率计时可持续估计 CdA，否则用默认 CdA。见[字段说明](https://mywindsock.com/page/hammerhead-extension/data-fields/)与[FAQ](https://mywindsock.com/page/hammerhead-extension/hammerhead-extension-faq/)。 |
| **Epic Ride Weather** | 官方称为基于当前位置或路线的**预报**；路线模式按路线位置、方向、速度和未来天气变化划分区段。Karoo 端具体上游气象供应商与采样算法未公开。见[Karoo 产品页](https://www.epicrideweather.com/karoo/)。 | Current Wind 显示当前位置预报风力与相对车头方向；有路线时显示这段风况预计持续多久。Upcoming Wind 显示下一段风况及预计时长；无路线时显示一小时后的条件。见[Karoo 产品页](https://www.epicrideweather.com/karoo/)。 | 必须连接 Epic Ride Weather 账号，使用需 Forecasts 订阅或试用。骑行开始时联网下载初始预报，此后可离线使用；途中持续联网才能刷新。无路线也能用，路线提供区段级预测。见[Karoo 产品页](https://www.epicrideweather.com/karoo/)与[设置说明](https://www.epicrideweather.com/karoo-setup/)。 |
| **WindField** | 官方说明使用第三方气象源/气象站；免费层为一个来源，PRO 宣传四个来源与自动选择，Ultimate 宣传来源平均。公开页面没有列出这四个风数据供应商，也没有公开 Karoo 端请求或融合算法。见[官网及 FAQ](https://windfield.app/)、[功能页](https://windfield.app/Features)。 | Karoo 原生扩展提供独立的风、天气和预报数据类型；Karoo 版首发说明列出风速、风向、迎风箭头等字段。见[功能页](https://windfield.app/Features)、[用户手册 Karoo 更新记录](https://windfield.app/manual)。 | Karoo 版在扩展库安装，设置页扫码关联账号；存在免费层及不同订阅层。官网列出按层级的更新频率，但功能表主要按 Garmin 设备分类，不能仅据此断言每项频率与算法在 Karoo 完全相同。见[官网](https://windfield.app/)、[功能页](https://windfield.app/Features)。 |

## “风速”为什么会不同

1. **绝对风速与迎风分量不同。** Headwind 的绝对风速取自天气响应；它的迎风读数把风速按骑行方向投影。正面逆风约等于原风速，正面顺风为负，纯侧风趋近零。[README](https://github.com/timklge/karoo-headwind)给出 ±20 km/h 示例，[源码变更](https://git.timklge.de/tim/karoo-headwind/commit/a98fcb875aafde831964908a4139e08b48006faf)展示余弦投影。因此“20 km/h 风”与“0 km/h 迎风”可同时成立。
2. **迎风分量与体感空气速度不同。** myWindsock 的 Air Speed 将地速与盛行风合成，并考虑地表粗糙度/风切变；其 Wind Field 则显示平均天气风速、阵风与方位。[Karoo 字段说明](https://mywindsock.com/page/hammerhead-extension/data-fields/)。Headwind 的 Relative Grade/Resistance Forces 也把风用于阻力估计，但这不是气象站风速本身。[README](https://github.com/timklge/karoo-headwind)。
3. **当前地点与前方路段不同。** Headwind 可切换当前地点的未来 12 小时预报和路线沿途预报；Epic 的 Current/Upcoming Wind 专门强调有路线时下一风况何时发生、预计持续多久；myWindsock 的 Karoo 字段无需路线。[Headwind README](https://github.com/timklge/karoo-headwind)、[Epic Karoo 页](https://www.epicrideweather.com/karoo/)、[myWindsock 字段说明](https://mywindsock.com/page/hammerhead-extension/data-fields/)。
4. **来源、刷新和空间代表性不同。** Headwind 明确暴露 Open-Meteo/OpenWeatherMap 的选择与重取触发条件。WindField 宣传多个气象源和自动选择，但没有公开源名称与 Karoo 实现。myWindsock、Epic 在所查 Karoo 文档中也未披露可复现的供应商、请求参数或缓存算法。天气模型/气象站提供的区域值与车把处瞬时风可能不同；WindField 自己也提醒气象站并非骑手所在位置。[Headwind README](https://github.com/timklge/karoo-headwind)、[WindField 官网 FAQ](https://windfield.app/)。

另有**外置硬件测量**路线：GiBLI Tech 的 Karoo 扩展读取并记录其 Aero Sensor 数据，属于与上述天气服务不同的数据来源。[Hammerhead 扩展 FAQ](https://support.hammerhead.io/hc/en-us/articles/31150180125083-FAQ-Hammerhead-Extensions)没有明确该扩展在 Karoo 上提供自然风向、风速字段，因此不将它计入上表的四款天气风字段。

## 对本项目的启示

- 若想做**可独立使用、来源透明**的风字段，Headwind 是最可追溯的参考：天气 API → 位置/时间缓存 → 与车头朝向组合 → 分别输出绝对风、迎风分量及预报。它还公开了供别的扩展订阅的 `TYPE_EXT::karoo-headwind::<typeId>` 数据类型，包括 `windDirection`、`headwindSpeed`、`windSpeed`；速度流声明为 m/s，图形显示再按 Karoo 单位转换。见[README 开发者段落](https://github.com/timklge/karoo-headwind)。
- 若只想在自己的组合字段中**引用现有风数据**，可以研究订阅 Headwind 的公开 Data Type，用户需先安装该扩展。应明确依赖、缺失时状态及单位契约；不要把“相对迎风分量”误当绝对风速。见[README 开发者段落](https://github.com/timklge/karoo-headwind)。
- 想做**前方路线风况**，需要路线几何、当前位置与预计到达时间的联合预测。Epic 证明这是对用户有区别的产品能力，但其内部实现不可作为可复现的源码参考。见[Epic Karoo 页](https://www.epicrideweather.com/karoo/)。

## 证据边界

本次未安装 APK 或用 Karoo 真机抓取网络流量。尤其是三款闭源产品的实际供应商、采样高度、Karoo 端刷新周期、断网缓存有效期和矢量计算细节，不能仅凭宣传页确认。WindField 的[用户手册](https://windfield.app/manual)有大量 Garmin Connect IQ 专用说明；本文只把明确标注 Karoo 的内容归给 Karoo 版。
