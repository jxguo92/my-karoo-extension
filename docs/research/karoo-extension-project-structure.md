# Karoo Extension 项目结构调研与决策

> 调研时间：2026-09-20（Asia/Shanghai）  
> 目标：先交付定制 Data Field，未来可增加地图层、提醒、设备接入等能力；开发方式为纯 vibe coding，因此优先考虑 agent 可定位、可验证、低认知负担。

本地观察：调研开始时仓库尚无 README、源码或既有 `docs/` 约定，因此本文按约定缺失时的默认路径放在 `docs/research/`；未改动其他文件。

## 结论先行

本项目应从**单一 `app` 模块 + package-by-feature（按功能垂直切片）**开始。每个 Data Field 放在自己的目录中，流接入、纯计算、状态、Glance 视图和测试尽量就近；`KarooExtension` 只做注册和生命周期编排。暂不建立 `domain/data/ui` 多模块，不默认引入 DI 框架。

推荐技术选择：

- Kotlin、Coroutines/Flow、Jetpack Compose（设置页）和 DataStore。
- 图形 Data Field 才使用 Glance；纯数值 Data Field 只实现 stream。
- 依赖版本统一放 `gradle/libs.versions.toml`。
- 初期使用显式构造函数注入和一个很小的 `AppContainer`；依赖图显著变复杂后再考虑 Koin。
- 从第一天建立纯计算单元测试、Data Type ID 契约测试和 CI；按 tag 生成签名 APK 与 `manifest.json`。

这不是最“企业化”的布局，但最符合当前规模。官方模板本身只有一个 `app` 模块；本次检查的六个社区项目也全部维持单 `app` 模块，包括已经包含地图、离线 POI、网络和多种 Data Field 的 RouteGraph，以及包含 OAuth、播放控制和大量页面的 Spintunes。来源见下文。

## 一手来源观察

以下为“观察”，不是本项目必须照抄的设计。

### 官方 SDK 与模板

- `karoo-ext` 是普通 Android library；应用通过 GitHub Packages 引入 `io.hammerhead:karoo-ext`，仓库认证可来自 Gradle 属性或环境变量。官方 README 同时推荐从官方模板开始：[README](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/README.md)。
- Extension 是一个继承 `KarooExtension` 的 Android `Service`。`AndroidManifest.xml` 通过 `io.hammerhead.karooext.KAROO_EXTENSION` intent filter 暴露服务，并通过 `EXTENSION_INFO` 指向 `res/xml/extension_info.xml`：[示例 Manifest](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/AndroidManifest.xml)、[extension_info.xml](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/res/xml/extension_info.xml)。
- `extension_info.xml` 是 Karoo 可发现能力的声明源；其中的 `DataType.typeId` 必须与 `KarooExtension.types` 返回的实现一致。SDK 在 `KarooExtension` 文档中明确写出这一约束：[KarooExtension.kt](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt)。
- `DataTypeImpl` 将能力分为 `startStream` 和 `startView`；订阅出现时开始 stream、视图附着时开始 view，二者都通过 emitter 的 cancellable 负责清理：[DataTypeImpl.kt](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/DataTypeImpl.kt)。官方数值组合示例使用 Flow 合并功率与心率，并在取消回调中取消 Job：[PowerHrDataType.kt](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/PowerHrDataType.kt)。
- 第三方进程中的自定义图形通过 `RemoteViews` 交给 Karoo OS；官方样例使用 Glance 生成 `RemoteViews`，但 README 明确说 Compose、Hilt、ViewModel、Glance 都不是 SDK 的硬依赖：[README 的 Sample App 与 Methodologies](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/README.md)、[CustomSpeedDataType.kt](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/CustomSpeedDataType.kt)。
- SDK 还能扩展设备扫描/连接、地图层、FIT 写入和 Bonus Action；官方 SampleExtension 将这些入口放在同一个 Extension service 中，并在 `onDestroy` 取消 Job、释放资源和断开连接：[SampleExtension.kt](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/kotlin/io/hammerhead/sampleext/extension/SampleExtension.kt)。
- SDK 源仓库的 `:lib + :app` 是“发布的库 + 用于验证库的样例应用”，不是普通扩展应用的推荐模块数：[settings.gradle.kts](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/settings.gradle.kts)、[README 的 Sample App](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/README.md)。相反，官方 `karoo-ext-template` 只有一个 `app` 模块，包含 Activity、Extension service、Compose screen 和 `extension_info.xml`：[模板目录](https://github.com/hammerheadnav/karoo-ext-template/tree/c789ab4a76b682772c73589182991f7254cb3ace)、[模板 settings.gradle.kts](https://github.com/hammerheadnav/karoo-ext-template/blob/c789ab4a76b682772c73589182991f7254cb3ace/settings.gradle.kts)。
- 官方模板和 SDK 样例都使用 Gradle version catalog；模板只包含应用所需依赖，SDK 样例再按需要加入 Hilt、Glance、序列化等：[模板 libs.versions.toml](https://github.com/hammerheadnav/karoo-ext-template/blob/c789ab4a76b682772c73589182991f7254cb3ace/gradle/libs.versions.toml)、[SDK libs.versions.toml](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/gradle/libs.versions.toml)。

### 社区样本

样本来自 `awesome-karoo` 的 Hammerhead library 列表或代表性扩展列表：[awesome-karoo](https://github.com/timklge/awesome-karoo/blob/master/README.md)。选择兼顾 GitHub 热度、近期活跃度和能力覆盖，而不是只复制同一作者的最简单例子。

1. **karoo-headwind**：高热度、Data Field 数量多，覆盖数值字段、图形字段、网络、缓存和测试。它仍是单 `app` 模块；所有实现集中在 `datatypes/`，共享天气数据由 Preferences DataStore + Flow 提供，重复的数值字段行为抽到 `BaseDataType`，图形字段用 Glance：[目录](https://github.com/timklge/karoo-headwind/tree/ab54194c86f7b3f697e7512ffded74c237b04206/app/src)、[Extension 注册](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/KarooHeadwindExtension.kt)、[DataStore](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/DataStore.kt)、[BaseDataType](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/main/kotlin/de/timklge/karooheadwind/datatypes/BaseDataType.kt)。它包含纯逻辑/DataStore 单元测试，并在 GitHub Actions 中对分支和 PR 执行 Gradle build、对 tag 发布 APK、manifest、图标和截图：[tests](https://github.com/timklge/karoo-headwind/tree/ab54194c86f7b3f697e7512ffded74c237b04206/app/src/test)、[workflow](https://github.com/timklge/karoo-headwind/blob/ab54194c86f7b3f697e7512ffded74c237b04206/.github/workflows/android.yml)。

2. **karoo-routegraph**：代表复杂图形 Data Field + 地图层 + POI/离线数据。它仍是单 `app` 模块，但包已经按 `datatypes/`、`datatypes/minimap/`、`pois/`、`screens/` 拆分；全局状态用不可变 data class + `MutableStateFlow` provider，长生命周期服务由 Koin 组装：[目录](https://github.com/timklge/karoo-routegraph/tree/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph)、[Application/Koin](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/KarooRouteGraphApplication.kt)、[RouteGraphViewModel](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/app/src/main/kotlin/de/timklge/karooroutegraph/RouteGraphViewModel.kt)。它有多组计算型单元测试，CI build 后上传/发布 APK 与 manifest：[tests](https://github.com/timklge/karoo-routegraph/tree/631c922df28605d556a48adb00ab36d28d85e245/app/src/test)、[workflow](https://github.com/timklge/karoo-routegraph/blob/631c922df28605d556a48adb00ab36d28d85e245/.github/workflows/android.yml)。

3. **karoo-reminder**：代表“未来不只是 Data Field”的事件型扩展。其 `extension_info.xml` 没有 DataType，Extension service 直接订阅 RideState/系统 Data Type，并发出屏幕、声音和 InRideAlert effect；设置以可序列化模型写入 DataStore，配置界面使用 Compose Navigation：[Extension](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/app/src/main/kotlin/de/timklge/karooreminder/KarooReminderExtension.kt)、[extension_info.xml](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/app/src/main/res/xml/extension_info.xml)、[Reminder model](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/app/src/main/kotlin/de/timklge/karooreminder/screens/Reminder.kt)、[screen/state persistence](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/app/src/main/kotlin/de/timklge/karooreminder/screens/MainScreen.kt)。它也是单 `app`，tag workflow 直接发布完整扩展资产：[settings.gradle.kts](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/settings.gradle.kts)、[workflow](https://github.com/timklge/karoo-reminder/blob/0a392846869a4c1b0032c83257ad6405ea4c6ee7/.github/workflows/android.yml)。

4. **karoo-powerbar**：代表 Android overlay/foreground service 能力。Karoo Extension service 只负责启动并连接系统，真正 overlay 由单独 foreground service 维护；设置放在 DataStore Flow 中：[KarooPowerbarExtension](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/KarooPowerbarExtension.kt)、[ForegroundService](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/ForegroundService.kt)、[Settings](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/kotlin/de/timklge/karoopowerbar/Settings.kt)、[Manifest](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/app/src/main/AndroidManifest.xml)。它仍只含一个 `app` 模块，并使用同类 tag release workflow：[settings.gradle.kts](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/settings.gradle.kts)、[workflow](https://github.com/timklge/karoo-powerbar/blob/b41856ecbd0ff4a4b717d28601a438b6fa0569aa/.github/workflows/android.yml)。

5. **karoo-tilehunting**：代表 Data Field + 地图层 + 网络 + Protobuf DataStore。三个数值字段各自是小型 `DataTypeImpl`，共享数据通过类型化 DataStore 提供；后台能力放在 `services/` 并由 Koin 装配：[Extension](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/app/src/main/kotlin/de/timklge/karootilehunting/KarooTilehuntingExtension.kt)、[Application/Koin](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/app/src/main/kotlin/de/timklge/karootilehunting/KarooTilehuntingApplication.kt)、[ExploredTilesDataType](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/app/src/main/kotlin/de/timklge/karootilehunting/datatypes/ExploredTilesDataType.kt)、[Protobuf DataStore](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/app/src/main/kotlin/de/timklge/karootilehunting/datastores/ExploredTilesDataStore.kt)。它有边界算法单元测试，CI 构建和 tag 发布 APK/manifest/媒体资产：[test](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/app/src/test/kotlin/BoundsTest.kt)、[workflow](https://github.com/timklge/karoo-tilehunting/blob/1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5/.github/workflows/android.yml)。

6. **karoo-spintunes**：代表交互型全页图形 Data Field、OAuth/API 与多个应用页面。它仍是单 `app` 模块；播放器切片把 `PlayerDataType`、Glance view/provider 与 actions 放在 `datatypes/` 下，播放状态由 `MutableStateFlow` provider 管理，跨 Activity/Extension/Glance callback 的长生命周期对象由 Koin 组装：[目录](https://github.com/timklge/karoo-spintunes/tree/c392d94fbda45390e1bb7c2d054b1aa5132d441d/app/src/main/kotlin/de/timklge/karoospintunes)、[Application/Koin](https://github.com/timklge/karoo-spintunes/blob/c392d94fbda45390e1bb7c2d054b1aa5132d441d/app/src/main/kotlin/de/timklge/karoospintunes/KarooSpintunesApplication.kt)、[PlayerDataType](https://github.com/timklge/karoo-spintunes/blob/c392d94fbda45390e1bb7c2d054b1aa5132d441d/app/src/main/kotlin/de/timklge/karoospintunes/datatypes/PlayerDataType.kt)、[PlayerStateProvider](https://github.com/timklge/karoo-spintunes/blob/c392d94fbda45390e1bb7c2d054b1aa5132d441d/app/src/main/kotlin/de/timklge/karoospintunes/spotify/PlayerStateProvider.kt)、[Glance action](https://github.com/timklge/karoo-spintunes/blob/c392d94fbda45390e1bb7c2d054b1aa5132d441d/app/src/main/kotlin/de/timklge/karoospintunes/datatypes/actions/NextAction.kt)。其 CI 对 tag 发布 APK、manifest、图标和截图：[workflow](https://github.com/timklge/karoo-spintunes/blob/c392d94fbda45390e1bb7c2d054b1aa5132d441d/.github/workflows/android.yml)。

### 跨样本归纳

- **单模块是社区常态**：六个样本都只 `include("app")`；复杂度主要通过 package、不可变状态和 service/provider 拆分，而不是 Gradle 模块拆分。各仓库的 `settings.gradle.kts` 可由上面的链接核对。
- **Flow 是运行时主干，DataStore 是持久化边界**：实时 Karoo 数据、设置与业务状态被组合为 Flow；用户设置或较大缓存才落盘。Headwind、Reminder、Powerbar、Tilehunting 和 Spintunes 的上述源文件都体现这一点。
- **Data Field 声明存在双点同步风险**：XML 声明 `typeId`，Kotlin `types` 列表再注册实现；官方 API 要求两边匹配，但社区样本普遍靠人工维护，没有显式契约测试。该风险直接来自官方约束与各 Extension 注册代码。
- **测试覆盖与项目热度并不等价**：六个项目都有 CI build/release，但本次快照中 Reminder、Powerbar、Spintunes 没有 `app/src/test`；Headwind、RouteGraph、Tilehunting 有针对计算或边界的 JVM 测试。来源为各仓库固定 commit 的 `app/src` 目录和 workflow。

## 项目结构决策（建议）

以下为本项目建议，不是对社区仓库现状的描述。

### 1. 采用单 `app` 模块

理由：

1. 官方模板就是单 `app`；SDK 源仓库的双模块是库开发需求，不应照搬。
2. Karoo 的 Extension service、Manifest、`extension_info.xml`、Data Field implementation 和发布 APK 天然属于同一部署单元。
3. 六个代表项目表明，即使加入地图、网络、OAuth、后台 service、离线数据，package 级边界在较长时间内仍足够。
4. 对 vibe coding 而言，单模块减少 Gradle 配置、跨模块可见性、依赖图和“为了改一个字段要跳五层”的负担。边界通过目录、构造函数和测试建立，而不是先通过构建系统建立。

### 2. 推荐目录树

包名以下用 `com.example.mykaroo` 占位；创建项目时一次性替换为正式 namespace/applicationId。

```text
.
├── AGENTS.md                         # agent 入口：命令、边界、完成定义
├── README.md                         # 人类入口：功能、安装、开发、发布
├── docs/
│   ├── architecture.md               # 依赖方向、生命周期、状态规则
│   ├── data-fields.md                # typeId 注册表、单位、预览、来源
│   └── research/
│       └── karoo-extension-project-structure.md
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/example/mykaroo/
│       │   │   ├── MainActivity.kt
│       │   │   ├── MyKarooApplication.kt       # 仅在需要容器/初始化时存在
│       │   │   ├── extension/
│       │   │   │   ├── MyKarooExtension.kt     # composition root；只编排
│       │   │   │   └── DataFieldRegistry.kt    # Kotlin 注册表
│       │   │   ├── feature/
│       │   │   │   ├── datafield/
│       │   │   │   │   ├── power_balance/
│       │   │   │   │   │   ├── PowerBalanceDataField.kt
│       │   │   │   │   │   ├── PowerBalanceCalculator.kt
│       │   │   │   │   │   ├── PowerBalanceState.kt
│       │   │   │   │   │   ├── PowerBalanceView.kt    # 仅图形字段
│       │   │   │   │   │   └── PowerBalancePreview.kt # 固定预览数据
│       │   │   │   │   └── another_field/
│       │   │   │   ├── settings/
│       │   │   │   └── map/                     # 未来真正需要时创建
│       │   │   ├── core/
│       │   │   │   ├── karoo/                   # SDK adapter/Flow helpers
│       │   │   │   ├── storage/                 # DataStore 与 serializer
│       │   │   │   ├── ui/                      # 少量跨功能 UI token
│       │   │   │   └── util/                    # 必须非常克制
│       │   │   └── di/
│       │   │       └── AppContainer.kt           # 显式手工装配，初期足够
│       │   └── res/
│       │       ├── xml/extension_info.xml
│       │       ├── values/strings.xml
│       │       └── drawable/...
│       └── test/kotlin/com/example/mykaroo/
│           ├── extension/DataFieldRegistryTest.kt
│           └── feature/datafield/
│               └── power_balance/PowerBalanceCalculatorTest.kt
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── .github/workflows/
    ├── build.yml
    └── release.yml
```

### 3. Data Field 使用垂直切片

每个字段目录应能回答五个问题：输入是什么、如何计算、状态是什么、如何显示、如何验证。建议规则：

- `*DataField.kt` 只负责 SDK adapter：订阅 Karoo Flow、把结果映射为 `StreamState`、注册 cancellable。
- `*Calculator.kt`/`*Formatter.kt` 保持纯 Kotlin，不依赖 Android 或 Karoo SDK；这是测试的主要目标。
- `*State.kt` 是不可变 data class/sealed interface；更新通过 `StateFlow`，避免可变 singleton。
- `*View.kt` 仅在 `graphical="true"` 时存在，使用 Glance 生成 RemoteViews；Compose 设置页不要与 Glance UI 混在同一文件。
- `*Preview.kt` 给 `ViewConfig.preview` 提供固定、可复现数据，不使用随机数或真实网络。
- 字段专用资源以字段前缀命名，例如 `power_balance_icon`；共享代码只有在至少两个切片真正复用后才移入 `core/`。
- `DataFieldRegistry` 是 Kotlin 侧唯一注册入口；`MyKarooExtension.types` 不再手写散落的构造逻辑。

建议把 XML 与 Kotlin 的同步变成测试：解析 `extension_info.xml` 中的 `typeId`，与 registry 的 typeId 集合做集合相等断言，同时检查 ID 不重复、Extension ID 不含点。这样可以在 CI 中提前发现“设备上字段不出现”这类配置错误。

### 4. 生命周期与状态

- `MyKarooExtension` 拥有一个 `CoroutineScope(SupervisorJob() + Dispatchers.IO)`；在 `onDestroy` 一次性取消。字段内部由 emitter cancellable 取消自己的子任务。
- 对 `KarooSystemService` 包一层很薄的 `KarooGateway`/Flow helper，集中连接、断开和 SDK 类型转换；不要把 SDK 对象散布到纯计算类。
- 运行时状态用 `StateFlow`；设置和需要跨进程重启保留的数据用 DataStore。小型设置用 Preferences DataStore；结构复杂且需要迁移时再用 Proto DataStore。
- 不把 Activity 当作 Extension service 的状态所有者。Activity 和 Extension 通过 repository/DataStore/StateFlow 观察同一来源。
- 每个订阅明确 `NotAvailable`、断线、预览、暂停/重置和单位制行为；这些状态写进字段测试用例和 `docs/data-fields.md`。

### 5. Gradle、DI、Compose 与 Glance

**Gradle version catalog：建议使用。** 官方模板、SDK 和六个样本都采用 `gradle/libs.versions.toml`。它让 agent 只在一个位置查找/升级版本，也减少依赖字符串重复。不要在第一版引入 convention plugin 或 `buildSrc`。

**DI：初期不引入 Hilt/Koin。** 使用构造函数注入和 `AppContainer` 即可。官方明确说 Hilt 不是 SDK 必需；官方模板也没有 DI。引入 DI 的合理信号是：Activity、Extension service 和多个 Glance callback 共享大量长生命周期对象，手工容器开始产生重复构造或生命周期错误。若达到该点，优先 Koin：本次复杂社区样本 RouteGraph、Tilehunting、Spintunes 都证明它适合这一类跨 Android 入口装配。不要因为“未来可能复杂”而预装框架。

**Compose：建议用于 Activity/设置页。** 官方模板和六个样本均以 Compose 构建应用 UI；它与当前 Android/Kotlin 生态一致，代码局部性也适合 agent 修改。

**Glance：按需使用。** 只有图形 Data Field 的 `startView` 需要 RemoteViews/Glance；纯数值 Data Field 不应为了统一外观而引入 view 层。Glance API 不是完整 Compose API，文件和测试应与普通 Compose screen 分开。

### 6. 测试策略

第一阶段最小测试集：

1. 每个 Calculator/Formatter 的表驱动测试：正常值、缺失值、边界、单位制、溢出/非法输入。
2. `DataFieldRegistryTest`：XML 与 Kotlin typeId 完全一致、无重复。
3. Flow/state 测试：输入顺序、断线、`NotAvailable`、取消后不再发射。
4. 设置序列化/迁移测试：旧字段缺失时回退到默认值，未知字段可忽略。
5. 图形字段至少对“尺寸分类 + 预览状态 + 点击 action”做逻辑测试；设备上的像素和交互仍需 Karoo 真机 smoke test。

CI 的 PR gate 建议执行 `./gradlew lint test assembleDebug`；release job 再执行签名 `assembleRelease`。不要只依赖 assemble，因为社区样本显示“有 CI”不必然意味着有测试。

### 7. 发布结构

沿用社区已验证的 tag 流程：

- `build.yml`：push/PR 执行 lint、unit test、debug build，并上传 APK 作为临时 artifact。
- `release.yml`：仅 tag 触发；从 tag/run number 注入 `versionName/versionCode`，使用 secrets 中的 keystore 签名，生成 `app/manifest.json`，发布 `app-release.apk`、manifest、icon 和截图。
- `AndroidManifest.xml` 的 `MANIFEST_URL` 指向稳定的 release asset URL；`manifest.json` 的包名、版本、APK URL 与 Gradle 配置由同一个生成任务派生，避免三处手改。
- 本地开发凭据放 `local.properties`，CI 使用环境变量/secrets；不提交 token 或 keystore。

## 何时才拆 Gradle 模块

保持单模块，直到至少满足下面一项，并且有可度量的收益：

1. 出现第二个独立 APK/发布物，必须复用同一套领域代码。
2. 某个稳定边界需要被多个功能复用，且其公开 API 已经小而明确；例如经过多个功能验证后的纯 Kotlin 计算库。
3. CI/本地增量构建已成为实际瓶颈，拆分后能独立缓存或只运行目标测试。
4. 某个大型功能需要隔离重量级依赖，且依赖不应泄漏给其他功能。
5. 多个 agent 经常因同一 package/build file 冲突；经过 package 拆分仍不能解决。

第一个可考虑的拆分通常是纯 Kotlin `:core:domain`，而不是把每个 Data Field 机械地拆成一个 module。只有当字段拥有独立依赖、稳定接口和独立测试价值时，才考虑 `:feature:<name>`。拆模块前应写一页 ADR，记录问题、预期收益和回退方式。

## Agent-friendly 约定

建议项目初始化时同时添加 `AGENTS.md`，至少写清：

- 快速验证命令、完整验证命令、真机 smoke test 清单。
- `extension_info.xml` 与 `DataFieldRegistry` 必须同步，新增字段必须新增测试和 `docs/data-fields.md` 条目。
- 依赖方向：`feature -> core`；切片之间不直接引用实现；`extension` 只装配。
- 不在 `MainActivity`、`MyKarooExtension` 或 `Extensions.kt` 堆业务逻辑。
- 不创建抽象层，除非已有两个真实调用者或是 SDK/Android 边界。
- 变更完成定义：format/lint/test/build 通过；新增图形字段有 deterministic preview；新增设置有默认值/迁移策略。

这些约定比提前拆很多模块更能帮助 agent：它们减少搜索空间、规定唯一修改点，并把最易遗漏的 XML/Kotlin 双点配置变成可执行检查。

## 建议的落地顺序

1. 以官方模板为骨架，保留单 `app`、Manifest service 与 `extension_info.xml`。
2. 建立 version catalog、`MyKarooExtension`、`DataFieldRegistry`、`AppContainer` 和一个最小数值字段。
3. 先写 Calculator 与 registry contract tests，再接 SDK stream。
4. 需要图形字段时加入 Glance；需要设置页时加入 Compose + DataStore。
5. 加入 `build.yml`；首次发布前再加入签名与 `release.yml`/manifest 生成任务。
6. 后续功能仍按 `feature/` 垂直切片增长；满足上节触发条件后再拆模块。

## 已检查的固定版本

- `hammerheadnav/karoo-ext`：`f79f103cd0e1a9808db64474796b30d822cc0b17`
- `hammerheadnav/karoo-ext-template`：`c789ab4a76b682772c73589182991f7254cb3ace`
- `timklge/karoo-headwind`：`ab54194c86f7b3f697e7512ffded74c237b04206`
- `timklge/karoo-routegraph`：`631c922df28605d556a48adb00ab36d28d85e245`
- `timklge/karoo-reminder`：`0a392846869a4c1b0032c83257ad6405ea4c6ee7`
- `timklge/karoo-powerbar`：`b41856ecbd0ff4a4b717d28601a438b6fa0569aa`
- `timklge/karoo-tilehunting`：`1303655ce5a6e6c4d8e45c01e5246fc8c3b90db5`
- `timklge/karoo-spintunes`：`c392d94fbda45390e1bb7c2d054b1aa5132d441d`
