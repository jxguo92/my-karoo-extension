# 用 SRAM AXS Bonus Button 通过 Karoo Extension 控制 DJI Osmo Nano：可行性调研

> 调研时间：2026-09-21（Asia/Shanghai）  
> 调研范围：结论以 DJI 官方手册/支持与开发者文档、Hammerhead 官方 `karoo-ext` 源码/API、Hammerhead/SRAM 官方 AXS 说明和 Bluetooth SIG 规范为依据；另将一个社区逆向工程项目作为“研究型 PoC 已有信号”单独标注，不与官方支持混同。

## 结论先行

**截至本次调研，不能把“安装一个 Karoo Extension 后，用 SRAM AXS Bonus Button 可靠控制 DJI Osmo Nano 开始/停止录像”视为现成或官方支持的功能。**

链路的前半段已经成立：SRAM AXS Bonus Button 可在 Karoo 上映射动作；官方 `karoo-ext` 允许扩展声明自定义 `BonusAction`，按键触发后 Karoo 会把对应 `actionId` 传给扩展的 `onBonusAction`。官方样例还演示了扩展申请 Karoo 蓝牙、扫描和连接 BLE 设备。因此，**“AXS 按键 → Karoo → Extension 回调”可直接实现**。[Hammerhead AXS 控制说明](https://support.hammerhead.io/hc/en-us/articles/25672636525979-Karoo-OS-Controlling-Karoo-with-SRAM-AXS-Controllers)、[`KarooExtension.onBonusAction` 官方源码](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L172-L185)、[官方样例的 BonusAction 与 BLE 实现](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt#L77-L140)

阻塞点在链路后半段：**DJI 已公开用于部分 Osmo Action/360 的 R SDK 蓝牙控制协议和示例，但官方协议文档在设备 ID 表中对 Osmo Nano 明确写着 “Not supported yet. Please wait for a future firmware update.”** DJI 官方配件兼容表也把 “Osmo Action GPS Bluetooth Remote Controller × Osmo Nano” 标为 `×`。所以现有 DJI 官方协议不能直接用于 Nano。[DJI 官方 `Osmo-GPS-Controller-Demo`](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo)、[官方协议的 Nano 支持状态](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo/blob/main/docs/protocol_data_segment.md#device-id-and-common-return-codes)、[DJI 官方配件兼容表](https://dl.djicdn.com/downloads/Osmo_Nano/20251124/DJI_Osmo_Series_Accessories_List_en.pdf)

因此当前判断是：

- **官方支持、可量产：否。** 缺少 DJI Nano 的第三方控制接口。
- **逆向研究型 PoC：已有第三方实现声称在 Nano 真机上成功。** `WingerATB/theslate` 声称 Nano 使用不同于 DJI R SDK 的协议，并称配对、实时状态、各录像模式和触发录制已稳定；这提高了技术可行性的置信度，但仍是单一第三方项目的自述，不等于 DJI 支持，也不解决许可、固件兼容和 Karoo 移植验证。[第三方 `WingerATB/theslate`](https://github.com/WingerATB/theslate#built-on-djis-own-protocols)
- **仅安装 Extension 就能完成：否。** Extension 只是承接 AXS 动作并运行 Android 代码，不能凭空补出 Nano 的控制协议。

## 已确认的现成功能

### 1. AXS Bonus Button 可以触发 Karoo 上的动作

Hammerhead 官方说明：Karoo 与 SRAM Force AXS 或 RED AXS 系统配对后，可分别给 Bonus Button 的短按、长按分配动作，也可为两个 Bonus Button 的组合分配动作；Wireless Blips 也可映射，但只有短按。配置入口是 AXS Groupset 的 **Configure Controls**。[Hammerhead 官方说明](https://support.hammerhead.io/hc/en-us/articles/25672636525979-Karoo-OS-Controlling-Karoo-with-SRAM-AXS-Controllers)

SRAM 官方进一步说明 AXS Bonus Buttons / Wireless Blips 是 ANT+ 控制器，动作可直接在 Karoo 上配置。[SRAM AXS Integration](https://www.sram.com/en/learn/hammerhead/technologies/axs-integration)

这里的限制是：需要受支持的 SRAM Force/RED AXS 系统，且拨把先与主 AXS 组件配对；不是任意 SRAM 按键都会自动出现在 Karoo 上。[Hammerhead 官方说明](https://support.hammerhead.io/hc/en-us/articles/25672636525979-Karoo-OS-Controlling-Karoo-with-SRAM-AXS-Controllers)

### 2. Karoo Extension 可以注册自定义 BonusAction

官方 SDK 的 `extension_info.xml` 支持：

```xml
<BonusAction
    actionId="camera-record-toggle"
    displayName="Toggle Osmo recording" />
```

`KarooExtension.onBonusAction(actionId)` 的官方注释明确写明：扩展声明的动作可以分配给控制器；控制器按键激活时，配置的 `actionId` 会传给声明它的扩展。官方样例也声明了两个 `BonusAction`，并在 `onBonusAction` 中分别启动 Activity 或发出骑行警报。[`extension_info.xml` 官方样例](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/res/xml/extension_info.xml#L32-L38)、[`KarooExtension.kt`](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L172-L185)、[`SampleExtension.kt`](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt#L181-L200)

这意味着 Extension 能把一个 AXS 短按定义成“开始/停止录像”，也可以将短按、长按映射为两个独立动作；但回调只提供 `actionId`，真正控制相机仍需扩展自己实现传输和 DJI 协议。

### 3. Extension 具备实现 BLE 客户端的官方路径

`karoo-ext` 提供 `RequestBluetooth(resourceId)` / `ReleaseBluetooth(resourceId)`，用途分别是请求 Karoo 主机打开蓝牙供该应用使用、释放之前的请求。官方样例在 Extension 建立时请求蓝牙，在销毁时释放，并使用 Android BLE manager 扫描指定服务 UUID、连接 BLE 设备。[`karoo-ext` 官方 API](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/index.html)、[官方样例 BLE 扫描与生命周期](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt#L77-L140)

所以 Karoo/Extension 平台本身不是主要阻碍。若目标设备有公开 BLE 协议，Extension 可以承担 BLE central/client 的工作。

### 4. Osmo Nano 的官方远控方式是 DJI Mimo，且支持语音控制

Nano 用户手册说明：移动设备同时启用 Wi-Fi 和蓝牙后，DJI Mimo 可监看画面、设置参数和控制相机。DJI FAQ 还确认 Nano 支持英文或中文语音命令，包括开始录像、停止录像、拍照和关机，建议在相机一米内、较安静的环境使用。[Osmo Nano 用户手册](https://dl.djicdn.com/downloads/Osmo_Nano/Osmo_Nano_User_Manual-en.pdf)、[DJI Osmo Nano FAQ](https://www.dji.com/nano/faq)

这些是用户可用功能，但官方资料没有把 **Nano 所使用的协议**作为第三方 SDK 开放，也没有说明 Nano 接受标准蓝牙相机遥控 profile。DJI 已开放的 R SDK 当前明确排除 Nano。

## 缺少的 API / 协议

要完成可靠的按键控制，至少还缺以下 DJI 侧信息：

1. Nano 是通过 BLE、Wi-Fi，还是 BLE 配网后再通过 Wi-Fi 接收录像命令。
2. 设备发现标识、服务 UUID、characteristic UUID，以及读/写/notify 语义。
3. 首次配对、绑定、会话鉴权、加密与重连流程。
4. 开始录像、停止录像、拍照、唤醒等命令帧及返回码。
5. 当前录像状态的查询或通知机制；否则单一 toggle 动作在丢包或重连后容易与相机真实状态相反。
6. Nano 是否允许 Mimo 之外的客户端连接，以及能否与麦克风、Vision Dock 或手机连接并存。

Bluetooth SIG 规范只定义 BLE 的通用 GATT 框架：服务、characteristic、descriptor，以及发现、读、写、notification/indication 等过程；设备可以混用 SIG 定义和厂商自定义 attribute。也就是说，“设备有蓝牙”只代表传输能力，不代表存在通用的“相机开始录像”命令。没有 DJI 的自定义 UUID、值格式和安全流程，Karoo Extension 无法从 GATT 标准本身推导出 Nano 控制协议。[Bluetooth SIG《Bluetooth LE Primer》](https://www.bluetooth.com/bluetooth-le-primer/)、[Bluetooth Core Specification：GATT](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Core-60/out/en/host/generic-attribute-profile--gatt-.html)

DJI 当前公开的 Mobile SDK 支持产品资料列出了旧款 Osmo/Osmo Pro/Osmo Mobile 等设备，但没有 Osmo Nano；当前 MSDK 5 的受支持产品表也以飞行器和行业载荷为主，没有 Nano。[DJI Mobile SDK 支持产品](https://developer.dji.com/mobile-sdk/documentation/introduction/product_introduction.html)、[当前 DJI Mobile SDK 文档/支持列表](https://developer.dji.com/doc/mobile-sdk-tutorial/en/)

DJI 另外公开了 `dji-sdk/Osmo-GPS-Controller-Demo`：它提供平台无关的 R SDK 帧解析、BLE 连接、开始/停止录像、模式切换和相机状态订阅示例；官方 README 指定的硬件是 ESP32-C6，支持机型是 Osmo 360 和 Osmo Action 6/5 Pro/4。协议数据文档给这些机型列出 device ID，却在 Nano 行明确标注尚未支持、等待未来固件。这比“没有搜索到 API”更强：**DJI 确有公开控制协议，但当前官方支持边界明确不包含 Nano。** [DJI 官方 Demo README](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo)、[DJI R SDK 数据段文档](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo/blob/main/docs/protocol_data_segment.md#device-id-and-common-return-codes)

## 为什么 DJI 官方蓝牙遥控器不能作为可行性证明

DJI 的 GPS Bluetooth Remote Controller 确实能通过蓝牙控制部分 Osmo Action/Osmo 360 机型的拍照、录像、模式切换和睡眠/唤醒；DJI 还公开了相应 R SDK 的协议与代码。但官方协议文档明确说明 Nano 尚未支持，不能证明 Nano 实现了相同协议。[DJI 官方遥控器说明](https://repair.dji.com/help/content?customId=01700008289&lang=en&paperDocType=ARTICLE&re=US&spaceId=17)、[DJI 官方协议支持状态](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo/blob/main/docs/protocol_data_segment.md#device-id-and-common-return-codes)

DJI 2025-11-24 发布的 Osmo 系列配件兼容表在 “Osmo Action GPS Bluetooth Remote Controller” 行对 Osmo Nano 标记为 `×`，而对 Osmo 360、Action 5 Pro、Action 4 标记为 `√`。这是目前最直接的官方兼容性证据。因此不能以“官方遥控器也是蓝牙”为理由，假设 Karoo 只需模拟该遥控器即可控制 Nano。[DJI 官方配件兼容表](https://dl.djicdn.com/downloads/Osmo_Nano/20251124/DJI_Osmo_Series_Accessories_List_en.pdf)

DJI Nano 支持页中仍出现“如何连接 Osmo Action GPS Bluetooth Remote Controller”的通用 FAQ 入口，但该页没有给出 Nano 专用连接步骤，且与上述型号矩阵冲突。对具体兼容性应优先采用明确逐型号标注的配件兼容表，不把通用 FAQ 标题当作兼容证明。[DJI Nano 支持页](https://www.dji.com/support/product/nano)、[DJI 官方配件兼容表](https://dl.djicdn.com/downloads/Osmo_Nano/20251124/DJI_Osmo_Series_Accessories_List_en.pdf)

## 逆向 PoC 的已有信号（非官方证据）

为回答“理论上能不能做”，本节单独记录一个第三方信号，但不把它提升为官方支持结论。

开源项目 `WingerATB/theslate` 声称：Action/360 使用 DJI 已公开的 R SDK，而 Nano 使用另一套协议；其 1.2.0 状态说明称已在 Osmo Nano 与 Osmo 360 上稳定实现配对、实时相机状态、所有录像模式和 AUX 触发。这个项目面向 ESP32 飞控附件，不是 Karoo Android 扩展；其 Nano 实现也不是 DJI 文档定义的官方路径。项目采用 PolyForm Strict 1.0.0，明确限制修改、再分发和商业使用，因此也不能假设可以直接复制到本仓库。[`WingerATB/theslate` 协议说明与许可证](https://github.com/WingerATB/theslate#built-on-djis-own-protocols)、[`WingerATB/theslate` 状态声明](https://github.com/WingerATB/theslate#status)

这份第三方自述说明 **Nano 的 BLE 控制在技术上很可能可行**，并把风险从“相机可能完全不接受第三方控制”缩小为“需要移植未获 DJI 支持的不同协议”。但仍需独立真机复现，尤其要验证 Nano 固件升级后的兼容性、许可边界、长时间重连与 Karoo 的 Android BLE 栈。

## 理论实现路径与验证门槛

如果以后 DJI 把 Nano 纳入 R SDK，或在合规前提下独立得到并验证 Nano 协议，目标链路可以是：

```text
AXS Bonus Button（ANT+）
  → Karoo OS 的控制映射
  → Extension.onBonusAction(actionId)
  → Extension 请求 Karoo Bluetooth
  → 与 Osmo Nano 建立 BLE/Wi-Fi 会话
  → 发送 start / stop 命令
  → 读取相机确认并向骑手反馈
```

建议先做“探测 PoC”，不要直接进入产品开发。Go / No-Go 门槛如下：

1. 真机确认 Karoo 能扫描到 Nano，并记录广播标识；若只能由 Mimo 通过特殊流程发现，也要记录流程。
2. 在不绕过安全保护的前提下确认 GATT 服务；判断它是否确实不同于 DJI R SDK，并确保协议实现具有合法来源和可用许可。
3. 证明 Karoo 能独立完成配对/重连，不依赖手机上的 Mimo 保持会话。
4. 至少稳定完成 100 次开始/停止录像，并能读取确定的录像状态；不能只靠无确认的 toggle。
5. 验证骑行中 AXS/ANT+、Karoo BLE、Nano、功率计/心率计等并发连接，覆盖休眠、断线、相机低电、Extension 重启和 Karoo 重启。
6. 确认协议使用与分发不违反 DJI 条款、当地法律或应用商店/扩展商店规则。

在第 2～4 项完成前，结论仍应保持为“研究型可能”，不能承诺功能。

## 可行替代方案

### A. 直接使用 Nano 官方语音控制

这是当前最简单、完全基于官方功能的方案：在相机一米内说“开始录像/Start Recording”或“停止录像/Stop Recording”。缺点是噪声、风噪和骑行环境会降低可靠性，且没有利用 AXS 按键。[DJI Osmo Nano FAQ](https://www.dji.com/nano/faq)

让 Extension 经 Karoo 播放预录语音去触发 Nano，理论上可以试验，但 DJI 只承诺识别人声命令，并未承诺识别另一设备播放的录音；Karoo 的扬声器/蜂鸣器输出、骑行风噪和误触发也需要实测。因此不建议把它当作可靠产品方案。

### B. 使用 DJI Mimo 作为官方控制端

手机上的 DJI Mimo 是 Nano 的官方控制方式，可用于画面监看、设参与相机控制。实际骑行时可把手机放在易触达位置，或使用手机自身可用的快捷操作；但 DJI 没有公开 Mimo 接收 Karoo/AXS 外部命令的接口，所以它不是现成的 AXS 桥接方案。[Osmo Nano 用户手册](https://dl.djicdn.com/downloads/Osmo_Nano/Osmo_Nano_User_Manual-en.pdf)

### C. 更换具有公开或官方远控能力的相机/遥控组合

如果“手不离把、按键可靠启停录像”是硬需求，优先选择厂家明确支持独立遥控器或公开控制 API 的相机，并在购买前确认该遥控器能安装在车把上。DJI 官方 GPS Bluetooth Remote Controller 当前支持的具体机型应以 DJI 说明和配件矩阵为准；该矩阵明确不含 Nano。[DJI 官方遥控器说明](https://repair.dji.com/help/content?customId=01700008289&lang=en&paperDocType=ARTICLE&re=US&spaceId=17)、[DJI 官方配件兼容表](https://dl.djicdn.com/downloads/Osmo_Nano/20251124/DJI_Osmo_Series_Accessories_List_en.pdf)

### D. 外置硬件桥接

可以设计独立硬件接收无线命令并物理触发相机按钮，但这需要额外电路、供电、防水、安装和安全验证；若涉及拆机或电气接入，还可能影响保修。它不属于“仅安装 Karoo Extension”的方案，也没有本次官方资料可证明其对 Nano 可行。

## 最终建议

当前不建议直接承诺产品功能，但可以立项一个有明确退出条件的真机 PoC。Karoo 侧 `BonusAction → onBonusAction` 有官方 API 保证；DJI 侧先验证 Nano 协议，不要误用只覆盖 Action/360 的官方 R SDK。投入完整开发前，仍应向 DJI 支持/开发者渠道书面确认以下问题：

> Osmo Nano 是否提供第三方 Android 应用可用的本地录像控制 API？是否支持 BLE GATT 或局域网命令开始/停止录像？若支持，协议、SDK、授权和并发连接限制在哪里公开？

如果 DJI 答复没有 Nano 官方接口，后续只能在“接受逆向协议、许可和固件风险”的前提下继续研究，不能称为官方支持方案；否则应改用语音、Mimo 或更换具有官方遥控能力的相机。若 DJI 未来把 Nano 加入 R SDK，再按上面的链路实现，Karoo Extension 本身不存在架构性阻碍。

## 已核对的主要官方来源

- [Hammerhead：用 SRAM AXS Controllers 控制 Karoo](https://support.hammerhead.io/hc/en-us/articles/25672636525979-Karoo-OS-Controlling-Karoo-with-SRAM-AXS-Controllers)
- [SRAM：AXS Integration](https://www.sram.com/en/learn/hammerhead/technologies/axs-integration)
- [Hammerhead `karoo-ext` `KarooExtension.kt` 固定版本源码](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt)
- [Hammerhead `karoo-ext` 官方 SampleExtension 固定版本源码](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt)
- [DJI：Osmo Nano 用户手册](https://dl.djicdn.com/downloads/Osmo_Nano/Osmo_Nano_User_Manual-en.pdf)
- [DJI：Osmo 系列配件兼容表](https://dl.djicdn.com/downloads/Osmo_Nano/20251124/DJI_Osmo_Series_Accessories_List_en.pdf)
- [DJI：Osmo Nano FAQ](https://www.dji.com/nano/faq)
- [DJI：Osmo Action GPS Bluetooth Remote Controller 说明](https://repair.dji.com/help/content?customId=01700008289&lang=en&paperDocType=ARTICLE&re=US&spaceId=17)
- [DJI：`Osmo-GPS-Controller-Demo`](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo)
- [DJI：R SDK 数据段与 Nano“尚未支持”说明](https://github.com/dji-sdk/Osmo-GPS-Controller-Demo/blob/main/docs/protocol_data_segment.md#device-id-and-common-return-codes)
- [DJI：Mobile SDK 支持产品](https://developer.dji.com/mobile-sdk/documentation/introduction/product_introduction.html)
- [Bluetooth SIG：Bluetooth LE Primer](https://www.bluetooth.com/bluetooth-le-primer/)
