# 骑行功率如何映射到一条刻度线：四种已实现方案

> 2026-09-22 核查。只采用项目自己的源码及 TrainerRoad 官方说明；GitHub 链接固定到当日提交。这里研究的是“功率值 → 一维坐标”，不限定 Karoo Data Field，也不把颜色分区误认为坐标映射。参照原型：[power-gauge-scale.prototype.html](../../demo/power-gauge-scale.prototype.html)。

原型约束：0% FTP 必须在刻度 0，400% FTP 必须在刻度 100；希望 50%–120% FTP 至少占中间 50 格。它实验了全程线性、手工锚点分段线性、归一化 Hill/S 曲线，并将无数据与 0 W 区分。以下外部实现的目标不一定相同，**没有发现这些项目照搬原型的 0–400% FTP 双端锚点**。[原型的公式与空值处理](../../demo/power-gauge-scale.prototype.html)

## 1. Karoo Powerbar：固定/自定义瓦数范围的线性归一化

实际公式是 `u=(round(W)-Wmin)/(Wmax-Wmin)`。默认 `Wmin = userProfile.powerZones.first().min`、`Wmax = userProfile.powerZones.last().min + 30 W`；用户开启自定义范围后可分别覆写上下限。训练目标值、目标区间上下界也走**同一** `remap`，因此目标与当前功率不会错用两把尺。[取值与目标变换](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L798-L841)、[`remap` 的空值/零跨度分支](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L50-L55)

绘制时才对当前进度执行 `coerceIn(0,1)`：全宽模式终点 `x=width*u`；分半模式改用半宽，右半条从右缘向左填充。因此它是**瓦数轴的仿射变换**，不是“每个功率区等宽”；功率区只单独决定颜色。瓦数标签保留原始读数，越界时条端饱和但标签仍显示真实 W。目标坐标在这段绘制代码中**没有同样的 clamp**，不能笼统地说所有标记都会贴边。[进度坐标和裁剪](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/CustomProgressBar.kt#L147-L169)、[目标坐标](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/CustomProgressBar.kt#L220-L234)、[功率标签和颜色](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L825-L839)

非 `Streaming` 或缺失的功率会变成 `null`：进度为 `null`、标签 `?`，条不绘制；零跨度 `remap` 也返回 `null`，虽然此时仍可能保留数值标签。源码中没有看到自定义 `min < max` 的约束；若上下限反转，公式会反向，不能作为本项目的输入校验范例。[流与缺失分支](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L794-L841)、[空进度不绘条](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/CustomProgressBar.kt#L256-L277)

**设计目标/限制：**让骑手自选关注的功率上下限，阈值和进度在一把固定 W 轴上可比较。它不保证 400% FTP 可见，也不保证 50%–120% FTP 占多大行程；占比完全由所选 `[Wmin,Wmax]` 决定。若希望原型的 0–400% 双锚点，不能直接套用它的默认范围。[默认范围源码](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L813-L823)

## 2. Garmin Dash：FTP 与本次最大功率取大者，然后离散成十格

Garmin Dash 的紧凑横条先算 `M=max(FTP, sessionMaxPower)`，`u=clamp(P3s/M,0,1)`，再将 `round(10u)` 个矩形点亮。各矩形从中央向屏幕右缘排列；完整布局的弧形表也使用同一个 `u`，但离散成自身的段数。FTP 优先取 Garmin 用户档案，否则取 app 设置；功率区阈值主要用于颜色，**不是**这条直线的位置分段。[FTP 来源](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L107-L135)、[横条归一化与十段离散](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L1448-L1505)、[弧形表复用同一比例](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L1063-L1085)

**推导而非源码注释：**在尚未创下高于 FTP 的记录时，100% FTP 已经填满；一旦本次最大值升高，轴上限随之升高，同样的 100% FTP 会向左退。例如 FTP=200 W、本次最大功率=800 W 后，200 W 只点亮约 3/10 格。它换得的是对本次峰值的相对比较，代价是同一个绝对功率在不同时间的位置不固定，不能满足原型的稳定刻度。[本次最大功率的更新](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L259-L263)、[比例公式](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L1448-L1465)

断线处理要谨慎借鉴：没有 `currentPower` 时先置 `mHasPowerData=false`，但若平均/最大功率仍有值又会设成 `true`；`mPower3s` 不一定清零。故它并非严格的“无当前读数就隐藏功率条”，这与本原型的 `null` 不画指针不同。源码对 `M=0` 也没有显式防护。[数据更新](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L241-L263)、[比例公式](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L1448-L1465)

## 3. FoxPower：每个功率区等宽，区内线性插值

这是最接近原型“放大常用区间”的公开实现，不过它采用**功率区等宽**，而非手选 50%/120% 两个锚点。其 7 区边界为 FTP 的 `55%、75%、90%、105%、120%、150%`；每个区沿横线获得相同宽度 `zoneWidth`，区内用 `(P-low)/(high-low)` 得到 `zoneDecimal`，箭头坐标为 `x=2+zoneWidth*(zone-1+zoneDecimal)`。末区用 150% FTP 到 225% FTP 的线性段，超过后把区内比例钳在 1。它另可读取 Garmin 配置的 5 区边界，同样按区等宽。[FTP 七区边界](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerZones.mc#L7-L7)、[七区/五区插值与末区钳位](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerZones.mc#L36-L105)、[每区等宽与箭头坐标](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerView.mc#L423-L457)

**按源码公式推导**，取 FTP=200 W（边界恰好都是整数）时：50% FTP 落在刻度约 13.0，120% FTP 在约 71.4，常用区间占 **58.4/100**；100% FTP 在约 52.4；200% FTP 在约 95.2；225% FTP 已到右端，400% FTP 与它重合。因此它确实能给 50%–120% 超过半条直线，但**不满足**原型 `400% → 100` 且高功率仍可分辨的要求。这里的百分比是把区宽归一成 `100/7` 后的分析值，源码实际计算像素坐标。[区内插值](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerZones.mc#L44-L71)、[像素坐标](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerView.mc#L437-L457)

缺失读数时 `compute` 提前返回，当前区及箭头留在上一次位置；显示的大功率数字改为 `--`。暂停只停止“各功率区驻留时间直方图”的累加，实时功率与箭头继续更新。这是“保持上次指针”的设计，与原型的“无数据不画指针”明确不同。[缺失/暂停逻辑](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerView.mc#L211-L247)、[数字缺失显示](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerView.mc#L488-L493)

## 4. WattRamp：实时曲线把最近数据范围动态映到纵轴

WattRamp 的实时功率曲线并非单点横向 gauge，但它展示了第三种轴范围策略：从当前 `powerHistory` 与正值训练目标取 `minPower=min(values)-10 W`、`maxPower=max(values)+10 W`；`range=max(maxPower-minPower,20 W)`，再映射 `y=height-padding-(P-minPower)/range*(height-2*padding)`。目标水平线与所有历史点共用同一公式；横轴是样本顺序。少于两个样本时只画背景，`powerHistory` 是整数列表，源码没有定义 `null` 功率样本的状态。[完整坐标与少样本分支](https://github.com/yrkan/wattramp/blob/1d16b8b101594c854dc32e7fcc04da2cb46d3e39/app/src/main/kotlin/io/github/wattramp/ui/components/PowerGraph.kt#L20-L101)

**设计目标/限制：**每个当前窗口都铺满视觉高度，适合看短时波动和目标相对位置；范围随数据变化，所以不适合给 50%/120%/400% FTP 一个跨时间不动的指针刻度。该结论是由范围公式推导，不是项目对“固定刻度”的承诺。[动态范围源码](https://github.com/yrkan/wattramp/blob/1d16b8b101594c854dc32e7fcc04da2cb46d3e39/app/src/main/kotlin/io/github/wattramp/ui/components/PowerGraph.kt#L44-L79)

## 与原型的取舍

- 如果 **0% 与 400% FTP 两端必须固定**，Powerbar/Garmin Dash/WattRamp 的动态或用户档案上限都不能直接作为规则；FoxPower 的等区宽正好扩展常用区，却在 225% FTP 就饱和。原型的分段聚焦或归一化 S 曲线解决的是**另一个约束组合**，并非行业既成标准。[原型公式](../../demo/power-gauge-scale.prototype.html)、[FoxPower 末区](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerZones.mc#L55-L59)
- 可借鉴的共同做法是明确区分**数值标签、坐标归一化、颜色语义、目标标记**：Powerbar 的标签仍为 W、目标共用同轴；FoxPower 将位置分段，颜色仍按区；Garmin Dash 把连续比例离散成灯格。若本项目采取非线性位置，必须明示刻度不是 `% FTP` 本身。[Powerbar](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Window.kt#L819-L831)、[FoxPower](https://github.com/SukkaW/FoxCIQ/blob/f6bbbc9211b54c878afb21b695c7603b3e248129/FoxPower/source/FoxPowerView.mc#L423-L457)、[Garmin Dash](https://github.com/gregor-srdic/garmin-dash/blob/d20572ecac771072abdfd257383282aef2f88cf8/source/DashView.mc#L1448-L1505)
- “沿目标居中”是另一类任务。TrainerRoad 官方将 Target Power Bar 描述为接近目标的提示，并让区间平均功率白点对准目标竖线；但其文档**没有公开坐标公式、范围、越界处理**，不能反推它使用了何种线性或非线性映射。[TrainerRoad Live Workout Display](https://support.trainerroad.com/hc/en-us/articles/201974500-Live-Workout-Display-Explained)
