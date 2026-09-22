# 中国大陆 Karoo 风向风速 Data Field：天气 API 选型

> 调研日期：2026-09-22。承接 [现有风字段调研](karoo-wind-data-fields.md)。本文件比较可由自有 Karoo 扩展直接调用的天气 API；价格、套餐和许可可能变化，上线前应复核官方页面。尚未在中国大陆网络或 Karoo 真机实测。

## 选型结论

**先试和风天气 QWeather 与彩云天气 Caiyun，再用 Open-Meteo 作无密钥基线。** QWeather 的经纬度实况字段、公开阶梯价最利于快速做出可计费的 Karoo 字段；彩云提供 10 m 风和更密的实况发布节奏，但需要在管理台确认具体价格。若要比较另一家国内网格数据，可加中科星图·星图云。OpenWeather One Call 4.0 是价格清楚的国际商业备选。[QWeather 实况](https://dev.qweather.com/docs/api/weather/weather-current/)、[价格](https://dev.qweather.com/docs/finance/pricing/)、[彩云实况](https://docs.caiyunapp.com/weather-api/v2/v2.6/1-realtime.html)、[计费](https://docs.caiyunapp.com/weather-api/billing.html)、[星图云网格实况](https://open.geovisearth.com/support/document?docId=739)、[OpenWeather 4.0](https://openweathermap.org/api/one-call-4)。

这只是**接口与成本的优先级**，不是风准确率排名。没有查到可复核的中国大陆路线级、同场风速/风向横评，也没有在大陆网络或 Karoo 真机实测。厂商公布的“1 km”“分钟级”是产品规格，不能单独证明骑行点的风更准。[QWeather 产品说明](https://dev.qweather.com/docs/api/weather/)、[彩云时空覆盖](https://docs.caiyunapp.com/weather-api/v2/v2.6/tables/coverage.html)。

## 对比口径

- **更新频率**分开记录接口允许的请求间隔、供应商对外发布新数据的频率，以及预报时间步长。较密的预报时间步长不一定代表新的风观测；骑行设备每分钟请求也不一定得到新风值。
- 这里的风通常是气象站附近或数值模式网格的**10 m 高度风**。骑手附近的遮挡、山谷、建筑、车速以及骑行航向会使实际迎风与它不同。现有 [风字段调研](karoo-wind-data-fields.md) 已区分绝对风与迎风分量。
- **准确性风评**优先找同一地区、同一时段、同一高度的风速与风向实测对照。应用商店对降雨和体感的好评不能充当风的准确性证据；厂商的“高精度”宣传也不能直接当作横向排名。

## 中国大陆服务商

| 服务 | Web API 风字段 | 官方更新/空间说明 | 可核实价格 | 适配判断与限制 |
| --- | --- | --- | --- | --- |
| **和风天气 QWeather** | 新版 `/weather/v1/current/{latitude}/{longitude}` 返回 `wind.direction.degree`（度）、`wind.speed.value`、`windGust.value`（响应带单位）；[逐小时 API](https://dev.qweather.com/docs/api/weather/weather-hourly-forecast/)也有风与阵风。 | 官方称实况分钟级更新、1 km 分辨率；逐小时预报是 1 小时时间步长。[实况 API](https://dev.qweather.com/docs/api/weather/weather-current/)、[服务总览](https://dev.qweather.com/docs/api/weather/)。 | [天气与基础服务阶梯价](https://dev.qweather.com/docs/finance/pricing/)每月前 50,000 次 ¥0；接着 950,000 次 ¥0.0007/次，再接着 400 万次 ¥0.0005/次。多个天气/基础服务请求合并计量。 | **首选试验**。支持经纬度点，风速与方向可用于迎风投影。需要帐号、JWT、专属 API Host 和[数据归因](https://dev.qweather.com/docs/terms/attribution/)；新 API 经纬度最多两位小数，使用时应核实回包时间/缓存。旧 [v7 城市接口](https://dev.qweather.com/docs/api/weather/weather-now-webapi-v7/)标为将弃用，不能混用其字段或更新说明。 |
| **彩云天气 Caiyun** | 稳定版 v2.6 `/realtime` 的 `result.realtime.wind.speed/direction` 是 10 m 风；[小时 API](https://docs.caiyunapp.com/weather-api/v2/v2.6/3-hourly.html)逐项给时间、风速、风向。[实况 API](https://docs.caiyunapp.com/weather-api/v2/v2.6/1-realtime.html)。 | 官方称实况约 1–5 分钟滚动发布、约 1 km；短时小时预报滚动发布约 5–15 分钟，风预报约 9–13 km，具体时段与范围见[覆盖说明](https://docs.caiyunapp.com/weather-api/v2/v2.6/tables/coverage.html)。 | [公开计费页](https://docs.caiyunapp.com/weather-api/billing.html)列按量、包月、企业方案，但未公开可引用的具体单价；需登录管理台确认。 | **首选对比**。支持点位风，但必须固定[单位制](https://docs.caiyunapp.com/weather-api/v2/v2.6/tables/unit.html)：默认 `metric` 风速为 km/h、风向为角度；`SI` 为 m/s、**弧度**。v2.6 token 在 URL 路径，可考虑[签名鉴权](https://docs.caiyunapp.com/weather-api/v2/v2.6/auth.html)。 |
| **中科星图·星图云** | [全国网格实况 V2](https://open.geovisearth.com/support/document?docId=739)按经纬度返回 `ws`（m/s）、`wd`（度），另有[逐 10 分钟历史实况](https://open.geovisearth.com/support/document?docId=723)。 | 文档称中国区 70–140°E、0–60°N，1 km 网格，10 分钟更新。[实况 V2](https://open.geovisearth.com/support/document?docId=739)。 | 官方接口文档引向购买/调用限制，未核实公开单价；需确认报价和商用条款。 | **备选对照**。字段适合风 Data Field；需把 `999999` 识别为缺测，检查边界区域覆盖。该实况接口本身不等于路线未来风预报。 |
| **高德天气查询** | [普通实况](https://lbs.amap.com/api/webservice/guide/api-advanced/weatherinfo)给 `winddirection` 文本、`windpower` 风力级别，没有连续数值风速和角度；按行政区 `adcode` 查询。 | 官方称实况每小时更新多次，以回包 `reporttime` 判断时间。 | [配额页](https://lbs.amap.com/upgrade)列个人非商用及符合条件企业方案每月 5,000 次天气查询；天气流量包 ¥30/万次，商用条件需另核。 | 可作粗略文字天气后备；风级区间不能冒充精确 m/s，行政区风不适合准确的实时迎风投影。 |
| **百度地图天气查询** | [普通天气 API](https://lbsyun.baidu.com/docs/webapi?title=weatherinquiry%2Fweather%2Fadvanced)以 `wind_class`、`wind_dir` 为主；`wind_angle` 标为 VIP，未见精确数值风速。 | 响应含 `uptime`，所查官方接口文档未明确实况发布周期。 | [配额页](https://lbsyun.baidu.com/cashier/quota)说明超默认配额后购买，未查到可引用的天气请求单价/VIP 风向价。 | 不宜作主数据源；旧[车联网天气接口](https://lbsyun.baidu.com/index.php?title=car%2Fapi%2Fweather)已停更。 |

彩云请求坐标是**经度、纬度**，QWeather 新接口路径是**纬度、经度**；实现时必须分别编码，不能共享一个未标注顺序的坐标字符串。[彩云实况](https://docs.caiyunapp.com/weather-api/v2/v2.6/1-realtime.html)、[QWeather 实况](https://dev.qweather.com/docs/api/weather/weather-current/)。

## 国际服务，作为基线或备选

| 服务 | 风 API 与数据来源 | 更新时间与步长 | 价格与使用条件 | 大陆适用性判断 |
| --- | --- | --- | --- | --- |
| **Open-Meteo** | `/v1/forecast` 通过经纬度请求 `current` 或 `hourly` 的 `wind_speed_10m`、`wind_direction_10m`、`wind_gusts_10m`；可指定 ECMWF IFS 模型。其 [ECMWF 页面](https://open-meteo.com/en/docs/ecmwf-api)称全球 IFS HRES 为 9 km 网格、每 6 小时运行一次。 | [通用 API 文档](https://open-meteo.com/en/docs)提供逐小时风；所谓 `current` 基于 15 分钟数据，但大陆的 15 分钟序列由逐小时数据插值，并非独立的 15 分钟风预报。模型更新是 6 小时量级。 | [定价页](https://open-meteo.com/en/pricing)明确免费层仅非商业使用，限 10,000 次/日、300,000 次/月；商业 Standard 为 100 万次/月固定月费，页面未给出可核实金额，需向订阅页面确认。要求归属标注。 | 数据覆盖大陆；服务器位于欧美，页面未保证大陆网络可达性或时延。适合无 key 原型与可复现实验，生产使用前要做跨运营商实测。单独的 [CMA GRAPES 入口](https://open-meteo.com/en/docs/cma-api)虽有中国模式，但官方当前提示上游开放数据过载、下载不可靠，不宜据此承诺稳定备源。 |
| **OpenWeather One Call 4.0** | 按经纬度取 current 与时间线，字段含 `wind_speed`、`wind_deg`，阵风可选；[官方 4.0 文档](https://openweathermap.org/api/one-call-4)称全球覆盖，使用自有 OWHL 模型。 | 官方称模型/服务每 10 分钟更新，建议同间隔请求；可取 15 分钟与逐小时预报。10 分钟发布频率不证明每个大陆坐标的风场有 10 分钟实测。 | [产品文档](https://openweathermap.org/api/one-call-4)写明每天前 1,000 次免费，超出按调用量收费，须开通 One Call by Call；[官方订阅页](https://home.openweathermap.org/subscriptions/unauth_subscribe/onecall_40/base)标价每 100 次 0.15 美元（未含 VAT），即超额 0.0015 美元/次。 | GPS 坐标直接查询，适合作为商业可用的全球备选；官方未给中国大陆专属的风速验证、网络可达性或服务节点承诺。原 [Headwind 调研](karoo-wind-data-fields.md)中的 One Call 3.0 适配不能直接视为 4.0 已兼容。 |

Open-Meteo 的 [ECMWF 专用 API](https://open-meteo.com/en/docs/ecmwf-api)还有一个重要区别：它公开的是具体模式预报；通用接口默认选择“最佳匹配”模式，不能假定一直选 ECMWF。同一供应商的模式切换也会影响风值。

## 准确性证据与局限

目前没有找到可复核、面向中国大陆骑行路线、同时比较各商业 API 风向和风速的公开盲测。一个可参考但不能直接迁移到当前 API 排名的[新疆 21 个机场实测对照研究](https://pdf.hanspub.org/ojns_2951494.pdf)使用 2015–2019 年资料评估当时的 ECMWF 细网格 10 m 风预报：12–72 小时风速预报的平均绝对误差为 1.35 m/s，整体偏低，大风漏报明显。研究所用的模式版本、预报时效与机场环境均不同于今天的 Open-Meteo `current`，所以只说明“全球模式在大陆局地风上有显著误差风险”，不能用 1.35 m/s 代表某 API 当前性能。[大连 9 站阵风研究](https://cnki.istiz.org.cn/kcms/detail/detailall.aspx?dbcode=CJFD&dbname=CJFD2024&filename=gsqx202401014)则观察到当时 ECMWF 细网格阵风整体偏大、误差随风级变化；两地结论方向不同，进一步说明不能给全国单一准确率。

关于**用户风评**，可见的[和风天气 App Store 评论](https://apps.apple.com/cn/app/%E5%92%8C%E9%A3%8E%E5%A4%A9%E6%B0%94/id1461458147?platform=iphone&see-all=reviews)、[彩云天气 App Store 评论](https://apps.apple.com/cn/app/%E5%BD%A9%E4%BA%91%E5%A4%A9%E6%B0%94-%E4%B8%BA%E6%82%A8%E9%A2%84%E6%8A%A5%E5%87%A0%E7%82%B9%E5%87%A0%E5%88%86%E4%B8%8B%E9%9B%A8/id847764912?platform=iphone&see-all=reviews)和[社区使用反馈](https://bbs.hassbian.com/thread-21777-1-1.html)主要评价整体天气或降雨，样本与地点不受控，几乎没有风速和风向的同场实测。因此目前给 QWeather、彩云、星图云、Open-Meteo 或 OpenWeather 标注“风最准确”都缺证据。

## 建议验证方式

1. 选平原、海岸、山地/城市各若干条骑行路线，记录 Karoo GPS、时间、各 API 原始风速风向、原始数据时间戳、网络成功率与响应时间。让同一时刻的不同服务对照同一公开站点，风向用圆周角差，风速用 MAE 与偏差；不要把 Karoo 相对迎风分量与气象站绝对风速直接相减。
2. 先按 10–30 分钟或移动一定距离刷新，缓存最近成功值并显示数据年龄；在设备上区分“风接近零”和“数据过期/不可用”。一台设备连续每 10 分钟请求为 144 次/日，4 小时骑行约 24 次。若每人每月骑 30 小时且每 10 分钟请求一次，则为 180 次/人/月；1,000 人约 18 万次/月，按现行 QWeather 天气/基础服务阶梯且没有其他请求，约 ¥91/月。这只是调用量推算，不含路线多点查询和其他服务，也不代表实测账单。[QWeather 定价](https://dev.qweather.com/docs/finance/pricing/)。
3. 在大陆不同运营商的手机 Companion 热点链路上实测请求成功率和时延。服务商的地理数据覆盖不等于跨境网络稳定可达；没有本地实测前不据此排序。

## 仍需确认

- 彩云和星图云的实际套餐金额、超量处理及商用授权；QWeather 新实况的资料时间字段与缓存行为；各家缺测、限流与断网时的真实响应。公开文档不足以替代试用 key 验证。[彩云计费](https://docs.caiyunapp.com/weather-api/billing.html)、[星图云接口](https://open.geovisearth.com/support/document?docId=739)、[QWeather 实况](https://dev.qweather.com/docs/api/weather/weather-current/)。
- QWeather 要求[归因](https://dev.qweather.com/docs/terms/attribution/)；Karoo 小屏如何展示应在定稿 UI 时确认。彩云的 URL token、QWeather 的 JWT 和 OpenWeather API key 都应考虑应用分发后的密钥暴露与额度滥用，正式发布前核对各家鉴权/授权条款。[彩云鉴权](https://docs.caiyunapp.com/weather-api/v2/v2.6/auth.html)、[OpenWeather 4.0 文档](https://openweathermap.org/api/one-call-4)。
