# SRAM AXS Bonus Button 控制 Garmin Varia 的可行性调研

> 调研时间：2026-09-21（Asia/Shanghai）  
> 目标：判断 Karoo Extension 能否接收 SRAM AXS Bonus Button，并控制 Garmin Varia，重点以 RTL515/RTL516 的雷达和尾灯能力为例。

## 结论先行

**可以实现原型，但目前不能只靠 Hammerhead 的公开 Extension API 完整实现。**

- **AXS Bonus Button → Extension：官方支持，风险低。** Extension 在
  `extension_info.xml` 声明 `BonusAction`，用户把短按或长按映射到该动作后，Karoo
  会调用 `KarooExtension.onBonusAction(actionId)`。该能力从 `karoo-ext` 1.1.7
  提供；本项目使用 1.1.9，版本满足要求。
- **Extension → Karoo 已配对的 Varia 尾灯：公开 API 不支持发控制命令。** SDK 可以
  观察已保存设备、申请 ANT/蓝牙资源，也允许 Extension 自己向 Karoo 暴露设备；但
  没有公开的“设置原生传感器/灯的模式” effect 或 command。
- **RTL515/516 尾灯控制在技术上可行。** ANT+ Bike Lights profile 本身定义远程配置、
  监控和控制；Garmin 也明确说明 RTL515 可以由兼容显示设备改变灯光模式。社区项目
  KarooFireFly 已在 RTL515/516 上实测，通过 Karoo 的内部 `SensorService` AIDL 设置
  灯光模式，并已经把命令接到 Bonus Actions。
- **该现成路径依赖未公开、逆向得到的 Karoo 内部接口，维护风险高。** KOS 更新可能
  改变 service 名称、Binder transaction code 或 Parcelable；它不应被描述为官方 SDK
  能力，也不适合在没有版本门控和失败回退的情况下发布。
- **若“控制 Varia”指控制雷达探测本身，不建议做。** 雷达数据是 Varia 向显示端发送的
  传感器数据；公开资料没有提供“让 Extension 开/关探测”的 Karoo API。RTL515 的
  standby 模式会停止车辆探测，因此更不能把“循环灯光模式”简单实现为模拟设备按钮。

因此，建议把第一版目标定义为：**用 AXS Bonus Button 在 RTL515 的已知灯光模式之间
切换（或灯开/关），保留 Karoo 原生雷达显示和告警**。这条路径可做，但应明确标为
experimental，并接受每次 KOS 升级后的真机回归成本。

## 1. AXS Bonus Button 输入：公开 SDK 足够

Hammerhead 官方支持页面说明，配对兼容的 SRAM Force AXS 或 RED AXS 系统后，可以在
`Sensors → AXS Groupset → Configure Controls` 中给 Bonus Button 配置动作；短按和长按
可以分别映射，两个 Bonus Button 也可映射组合动作。官方列出的动作分类包括 AXS
Actions、Karoo 的骑行/训练/导航/硬件动作。前置条件是控制器已与 AXS 主组件配对：
[Hammerhead：Controlling Karoo with SRAM AXS Controllers](https://support.hammerhead.io/hc/en-us/articles/25672636525979-Karoo-OS-Controlling-Karoo-with-SRAM-AXS-Controllers)。

Extension 还能把自己的动作加入该映射界面：

1. 官方 README 的 `extension_info.xml` 示例包含 `<BonusAction actionId="..." ... />`：
   [karoo-ext README](https://github.com/hammerheadnav/karoo-ext/blob/master/README.md)。
2. `KarooExtension.onBonusAction` 的官方 API 文档明确说明：用户把声明的动作分配给
   controller 后，按键触发时，配置的 `actionId` 会传给定义它的 Extension；该 API
   自 1.1.7 起存在：
   [KarooExtension.kt](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L995-L1017)。
3. 官方 sample 同时展示 XML 声明和 callback 分发：
   [extension_info.xml](https://github.com/hammerheadnav/karoo-ext/blob/master/app/src/main/res/xml/extension_info.xml#L386-L396)、
   [SampleExtension.kt](https://github.com/hammerheadnav/karoo-ext/blob/master/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt#L231-L254)。

这里收到的是**用户已经配置好的语义动作**，而不是原始按键数据。实现可以声明例如
`varia-toggle` 和 `varia-cycle-mode` 两个 actionId，让用户自行决定左/右键、短/长按如何
绑定；代码不应依赖某个固定物理按键编号。

## 2. Varia 能力应拆成“雷达”和“尾灯”两部分

RTL515 同时实现后视雷达和智能尾灯。两者虽然装在同一设备中，却是不同的控制问题：

| 能力 | 数据方向 | 本需求的可行性 |
| --- | --- | --- |
| 雷达目标/威胁数据 | Varia → Karoo | Karoo 已原生支持；Extension 可消费 Karoo 导出的相关 stream，但这不是控制 |
| 雷达探测开/关 | Extension → Varia | 未发现公开 Karoo Extension API；不建议作为首版目标 |
| 尾灯模式/开关 | Controller → Varia | ANT+ Bike Lights 协议支持；Karoo 公开 Extension API 未暴露命令，内部 AIDL 可绕行 |
| BLE 雷达连接 | Varia ↔ App | Garmin 官方 Varia App 使用 BLE 显示雷达信息；没有公开、稳定的 BLE 灯控契约可供本项目直接采用 |

Garmin 用户手册列出 RTL515 的常亮、Peloton、夜间闪烁、日间闪烁和 standby 模式；
standby 下设备不探测车辆。手册还说明兼容 Edge 的 Light Network 可以设置单灯强度、
闪烁模式或关闭灯光：
[Garmin RTL515/516 Owner's Manual](https://www8.garmin.com/manuals/webhelp/GUID-C41F445D-457F-447D-88C8-FE286BF157E9/EN-US/Varia_RTL515_516_OM_EN-US.pdf)。

协议层面，ANT+ 官方公告说明 Bike Lights Device Profile 定义了无线配置、监控和控制
自行车灯；Radar Device Profile 则定义雷达与显示设备之间的车辆可视化数据：
[Bike Lights profile 公告](https://www.thisisant.com/developer/resources/tech-bulletin/ant-bike-lights-device-profile-released-to-ant-members)、
[Radar profile 公告](https://www.thisisant.com/developer/resources/tech-bulletin/ant-radar-device-profile-released)。
完整 profile 和 Android ANT SDK 受 Garmin/ANT 下载条款约束，不能把网络 key 或受限
规范内容直接复制进仓库：
[Garmin ANT Downloads](https://developer.garmin.com/ant-program/downloads/)。

## 3. 为什么公开 `karoo-ext` 仍不足以控制已配对的灯

公开 SDK 有几个看起来接近、但职责不同的入口：

- `SavedDevices` 只公开保存设备的 ID、连接类型、名称、是否启用、详情和支持的数据类型，
  是观察事件，不带写命令：
  [KarooEvent.kt / SavedDevices](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt#L2477-L2701)。
- `KarooExtension.startScan` / `connectDevice` 用于 Extension 扫描并向 Karoo 提供自己的
  `Device`；官方注释要求 `connectDevice` 的 uid 来自该 Extension 的 `startScan`。
  它不是连接或接管 Karoo 原生已配对设备的通用客户端 API：
  [KarooExtension.kt](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L935-L960)。
- `RequestAnt` / `ReleaseAnt` 允许应用请求 Karoo 保持 ANT radio 开启，但模型只包含
  `resourceId`，没有 channel、device 或 light-mode 命令：
  [KarooEffect.kt](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEffect.kt#L1024-L1073)。
- 当前公开 `KarooEffect` 有骑行、屏幕、地图、提示、ANT/蓝牙资源等 effect，但没有
  Varia 或通用灯光控制 effect：
  [KarooEffect.kt](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEffect.kt)。

`RequestAnt` 因此不能等同于“获得与 Karoo 自身相同的 ANT+ 传感器控制 API”。即使自行
实现 Bike Lights profile，也还需要能打开正确 RF channel、管理网络和避免与 Karoo
自己的连接冲突。

## 4. 三条实现路径评估

### 路径 A：调用 Karoo 内部 SensorService（最短可用路径）

社区开源项目 KarooFireFly 提供了目前最直接的可行性证据：README 声明 Garmin Varia
RTL515/516 的 ANT+ 灯控已经实测，并提供 `Toggle Lights`、`Cycle Mode` 两个可映射到
AXS 按键或 Karoo 硬件按键的 Bonus Actions：
[KarooFireFly](https://github.com/derstrassi/karoofirefly)。

它不是通过公开 `karoo-ext` 灯控 API，而是绑定 Karoo 内部 `SensorService`，取得
`LightCommandConnection` binder，调用 capability query 与 `setLightMode`。项目作者
同时记录：Karoo 会阻止第三方直接调用 `setRfFrequency()`，因此无法自行打开 ANT+
channel；内部 AIDL 的 transaction code 和类结构可能随固件改变。该说明和实现源码是
“现有实现如何工作”的一手证据，但不是 Hammerhead 对兼容性的承诺。

评估：

- 实现量最小，而且能复用 Karoo 已配对、已连接的 ANT+ 灯。
- 必须关闭对应灯在 Karoo 中的原生 Auto Control，否则两个 controller 会互相覆盖。
- 需要对 Binder 不存在、transaction 失败、设备断线和不支持的 mode 做无崩溃回退。
- 应按 KOS build 做 allowlist/已验证版本记录；升级 KOS 后必须重测。
- 使用未公开 API 可能影响进入官方 Extension Library，需要在实现前向 Hammerhead 确认
  审核政策。即使能 sideload，也不代表适合商店发布。

**这是建议用于技术验证的方案，但不是建议长期依赖的产品接口。**

### 路径 B：Extension 直接实现 ANT+ Bike Lights controller

协议在理论上允许，但 KarooFireFly 的设备实测指出第三方进程无法设置 ANT RF frequency。
公开 `RequestAnt` 只管理 radio 资源，并未提供 channel API。因此不能仅依据
`RequestAnt` 判断这条路可行。

评估：当前不作为主方案。只有 Hammerhead 明确开放第三方 ANT channel，或提供正式
Light Control effect 后，才值得投入完整的 profile、配对和冲突管理实现。

### 路径 C：直接 BLE 控制 Varia

Garmin 手册只承诺 Varia App 通过 BLE 显示雷达、设备和提醒设置；公开资料没有给出可供
第三方使用的稳定 BLE 灯光控制服务。即便逆向 GATT 可做，也会形成第二套私有协议依赖，
并带来 Android 蓝牙权限、配对、后台连接和与 Garmin App/Karoo 并发连接的问题。

评估：不建议用于 RTL515/516 首版。若目标 Varia 型号另有官方公开 BLE SDK，应按具体
型号重新评估，不能从 RTL515 的“支持 Bluetooth”推导出“第三方可控制尾灯”。

### 可维护替代：外部桥接器

如果不能接受私有 Karoo AIDL，可让 Extension 的 Bonus Action 通过 BLE 或网络发命令给
一个独立硬件桥接器，由桥接器实现 ANT+ Bike Lights controller。它避开 Karoo 内部
API，但增加额外硬件、供电和协议合规成本，一般只适合研究或自用。

## 5. 推荐的最小验证原型

不要第一步就迁入完整的 KarooFireFly。建议按下面的 kill points 分阶段验证：

1. **只验证输入链。** 在本项目 `extension_info.xml` 声明一个
   `varia-test-action`，在 `onBonusAction` 记录日志并显示短暂 `InRideAlert`。安装后重启
   Karoo，把真实 AXS Bonus Button 的短按映射到它。若目标 AXS 组合在 Configure
   Controls 中看不到 Extension action，先停止 Varia 工作并解决兼容/配对问题。
2. **只读发现 Varia。** 订阅 `SavedDevices`，记录 RTL515/516 在目标 KOS 中的 id、
   connectionType、supportedDataTypes 和它作为 Radar/Light 的呈现方式；不发送命令。
3. **隔离内部 API spike。** 单独做 `VariaLightController` adapter，只暴露
   `supportedModes()`、`setMode()` 和连接状态。所有 AIDL/reflection 留在 `core/karoo/`
   边界，feature 代码不得知道 transaction code。
4. **先查询 capability，再设置精确 mode。** 不模拟/盲目循环物理按键，以免进入会停止
   雷达探测的 standby。首次只验证 `off ↔ day-flash` 或设备实际返回的两个安全模式。
5. **接入 Bonus Action 状态机。** 对重复按键去抖；只有收到设置成功或观察到新模式后
   才更新 UI；断线时提示一次，不持续弹警报。
6. **设置固件护栏。** 记录已验证的 Karoo hardware/KOS build；未知版本默认禁用私有
   控制并给出清晰提示，而不是尝试未知 Binder transaction。

原型成功标准：

- AXS 短按和长按映射能稳定触发不同 `actionId`；
- RTL515 的灯光模式按预期切换，同时 Karoo 原生雷达目标和音频告警不中断；
- 骑行开始、暂停、结束、Varia 断线重连、Extension/设备重启后状态可恢复；
- 原生 Auto Control 开启时能检测冲突或明确阻止启用本功能；
- 无 Varia、Varia 关机、非 RTL515 型号、Binder 接口变化时 Extension 不崩溃；
- 长按/连按不造成命令风暴；退出或销毁 Extension 后没有残留连接。

## 6. 对本仓库的设计建议

若决定实现，保持现有单模块和垂直切片：

```text
feature/varia_control/
├── VariaControlState.kt
├── VariaControlReducer.kt       # 纯 Kotlin：toggle/cycle/失败状态
└── VariaBonusActionHandler.kt

core/karoo/
├── SavedDeviceGateway.kt        # 公开 karoo-ext 观察接口
└── VariaLightController.kt      # 内部 AIDL 适配，实验性边界
```

`MyKarooExtension` 只把 `onBonusAction(actionId)` 转交 handler。内部 AIDL adapter 应可以
整块删除或替换；状态转换用纯 Kotlin 测试覆盖正常切换、断线、未知模式、命令失败和
连续按键。由于 Bonus Action 不是 Data Field，本变更不需要改 `docs/data-fields.md`；但
XML 声明、actionId 常量和契约测试仍应保持一致。

## 最终判断

| 判断维度 | 结论 |
| --- | --- |
| AXS Bonus Button 触发 Extension | **官方支持，可做** |
| 仅用公开 karoo-ext 控制 Karoo 已配对 Varia 灯 | **目前不可做** |
| 用内部 SensorService AIDL 控制 RTL515/516 灯 | **已有实测先例，可做实验性实现** |
| 稳定性/商店发布把握 | **低到中；需 Hammerhead 确认，且每版 KOS 回归** |
| 直接控制雷达探测 | **无公开支持，不建议列入首版** |
| 推荐决策 | **先做两阶段 spike：BonusAction 输入 → 私有灯控最小验证，再决定是否产品化** |

