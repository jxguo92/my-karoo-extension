# 主流码表组合 Data Field UI 调研

检索日期：2026-09-22。本文把“组合字段”严格定义为**一个可配置的 data field/frame 内同时呈现两个以上语义指标**；“一页放多个普通字段”单独记录，不混为一谈。资料优先品牌官方手册、支持页和官方商店展示；其中 Garmin Connect IQ 示例是第三方作者制作、Garmin 官方商店分发，不代表 Edge 内建 UI。

## 直接证实的组合字段

### COROS DURA：原生 Chart 字段

COROS 官方把 Chart 列为一种字段类别，且逐项说明其一个字段内的三项数据：[DURA 字段清单](https://support.coros.com/hc/en-us/articles/26920689006228-Customizing-Activity-Data-Pages-on-DURA)。

- **Power**：3 秒平均功率、最大功率、全程平均功率。
- **Power and NP**：3 秒平均功率、最大功率、标准化功率 NP。注意这**不是**“3s + NP + AP”；NP 版本以最大功率为第三项，没有 AP。
- **Heart Rate**：当前、平均、最大心率。
- 同类还包括速度与踏频的当前/平均/最大三值组合。

同一官方页面说明，可编辑一页的 frame 数并为每格选择字段，格数越多文字越小；部分图表受格位样式限制。这意味着此处的“Chart”是**占一个 frame 的复合内容**，而不是三个独立 frame。官方 2025 年功能更新明确称其为新的可视化图表接口：[说明及设备图](https://coros.com/stories/latest-news/c/january-2025-dura)。

截图：[COROS 官方 DURA Chart 展示图（直链）](https://static.coros.com/coros-web-faq/upload/images/f3cd9a8786a0e84a8d25285cddcc918f.png)；[官方 App 页面编辑截图](https://support.coros.com/hc/article_attachments/39621537089044)。官网文案证明字段内的三项数据；上述图片用于观察视觉语言，不应从图片反推全部图表数值的精确位置。

![COROS DURA Chart：大号当前值、左右平均/最大值及下方微型图形](https://static.coros.com/coros-web-faq/upload/images/f3cd9a8786a0e84a8d25285cddcc918f.png)

### Garmin Edge：Connect IQ 扩展字段，而非原生三值模板

Garmin 官方 Edge 字段清单把 3s Power、Avg Power、Normalized Power、心率相关值分别列为独立字段；这只说明它们**可分别用于页面布局**，不证明内建字段会自动把它们组合：[Edge 550 字段清单](https://www8.garmin.com/manuals-apac/webhelp/edge550/EN-SG/GUID-F6C66BD3-9A5A-4C27-BD2D-665EC7F06F12-3650.html)。

Garmin Connect IQ 官方商店提供真正占一个 Data Field 的第三方组合设计实例：

- [PowerField](https://apps.garmin.com/en-US/apps/ae433c7a-d706-4f80-8e29-f64e3fc4add4)：作者说明在字段**上方**显示心率、踏频的当前/平均/最大值，下面是多个时间窗的功率当前/平均/峰值/目标值；用颜色表示区间状态。商店页面有 3 张预览图。它是信息密集型、多行表格式，而非三值极简卡片。
- [HEART+](https://apps.garmin.com/en-US/apps/ff4ca365-dfc2-45e0-a732-6cac6386945b)：作者说明一个字段同时显示当前、最大、平均心率，心率区间用图标颜色表示，另有分区时间水平条和近期心率曲线；商店页面有预览图。
- [Heart Rate Zones](https://apps.garmin.com/en-US/apps/9db4c19b-5b66-4211-9b2e-d498fb0201ce)：面向 Edge 1050 的三值加区间条案例：当前值/区间为主，活动最大/平均为辅；商店页面有预览图。

这些是**第三方 Connect IQ 字段在 Garmin 设备上的 UI 先例**，不能归纳成“Garmin 原生组合字段”。

## 未证实为单字段三值组合的品牌

### Wahoo ELEMNT

[官方页面自定义说明](https://support.wahoofitness.com/hc/en-us/articles/21043803163154-Customize-Pages-on-an-ELEMNT-GPS-Computer)强调在一页添加、更换、排序、删除多个字段，心率和功率是可选的字段类型，但没有确认“3s/NP/AP”或“当前/平均/最大心率”为一个内建三值字段。[新一代 ACE/BOLT 3/ROAM 3 自定义说明](https://support.wahoofitness.com/hc/en-us/articles/22709892604306-Customize-workout-profiles-and-pages-for-ELEMNT-ACE-BOLT-3-or-ROAM-3)同样以页面和字段配置为主。

近似的原生**图形组件**是 ACE 的 Wind Dynamics：Wahoo 官方说明可用颜色背景表示风况，并有风况分布图；专用 Wind 页面有速度仪表、6 个可定制字段和阻力比例图。[官方介绍及设备截图](https://support.wahoofitness.com/hc/en-us/articles/22735007422610-ELEMNT-ACE-Wind-sensor-and-Wahoo-Wind-Dynamics)，[4 字段截图直链](https://support.wahoofitness.com/hc/article_attachments/34805380010514)，[6 字段截图直链](https://support.wahoofitness.com/hc/article_attachments/34805380011922)。这是“一页的多个字段 + 专用图形”，**不是**用户举例的单字段三值功率/心率组合；只可借鉴其背景状态色和图形层次。

![Wahoo ACE Wind 页面：大仪表、独立的 3s 功率与心率格，以及底部图形](https://support.wahoofitness.com/hc/article_attachments/34805380010514)

### iGPSPORT

[iGS618 官方详细手册，第 22–25 页](https://global.igpsport.com/File/pdf/igs618-%E8%AF%B4%E6%98%8E%E4%B9%A6%E4%B8%AD%E6%96%87%E7%89%88.pdf)明确区分“每页显示格数”和“每一格的显示内容”：一页可设 1–10 格，逐格选择数据。其字段表列出当前/平均/最大心率及 3 秒平均、平均、NP 功率，但没有把三项列为一个字段。因此已证实的是**同页多格拼装**，尚未证实内建单格三值组合。手册第 22–24 页含配置过程的官方屏幕图，可直接打开 PDF 对照。

### Magene

[C506 官方产品页](https://www.magene.com/en/bike-computers/171-c506-smart-gps-bike-computer.html)写明数据页支持圆形仪表、折线图、柱状图，支持 14 类、108 个数据项、每骑行模式至多 10 页；页面配有产品图。它证实了**图形化页面布局能力**，但未给出任何明确的“一个字段同时含 3s/NP/AP 或当前/平均/最大心率”的规格或字段定义。不能仅凭营销页面图片将圆形仪表断定为三值组合字段。

另见 [C606 Pro 官方布局展示](https://www.magene.com/en/bike-computers/248-c606-pro-smart-gps-bike-computer.html)：圆盘、折线和数字格混排，页面说明 110+ 数据项和可自定义布局，但同样不能证明是一个扩展字段。

![Magene C606 Pro 官方数据页布局集合](https://static.magene.com/img/cms/C606P/11-1.png)

## 对 Karoo 组合字段的可借鉴点（推论，不是产品事实）

1. 最直接、可复核的 MVP 对标是 COROS 的“三个同一维度指标放一格”，而且应把组合**命名为固定语义**：功率 `3s / Max / Avg`、`3s / Max / NP`，心率 `Current / Avg / Max`。若项目希望 `3s / NP / AP`，它是自定义变体，不要称为 COROS 原样。
2. 主数值优先呈现当前或短窗值，平均/最大/NP 作次级读数；三项都保留清楚的短标签与单位。字段缩小时优先压缩次级标签或隐藏装饰图形，不能让 `NP`、`AP` 等语义混淆。此条是结合 COROS 格数变多导致字体变小及 Garmin IQ 字段层次的设计推论。
3. 同时考虑纯数字版与图形版：图形版可以借鉴 COROS 曲线、Garmin IQ 区间色和 Wahoo 状态底色，但颜色不应是区分 3s/平均/最大/NP 的唯一方式。

## 证据边界与后续待核

- COROS 资料证明字段指标构成，但现有官方截图不一定覆盖 Power、Power and NP、Heart Rate 三种选择各自的完整显示状态；实施前最好在 DURA 真机或官方 App 中逐一检查主次字级和小格位裁切。
- Garmin 的三值示例来自第三方开发者提交到 Connect IQ；不同 Edge 机型/字段尺寸的预览可能不同。
- Wahoo、iGPSPORT、Magene 目前只找到可信的页面级布局资料或图形组件，**未找到可核实的对应单字段三值功率/心率内建实现**。这是“未证实”，不是证明不存在。
